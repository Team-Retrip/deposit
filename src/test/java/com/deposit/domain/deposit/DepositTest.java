package com.deposit.domain.deposit;

import com.deposit.common.exception.BusinessException;
import com.deposit.common.exception.ErrorCode;
import com.deposit.domain.deposit.vo.DepositStatus;
import com.deposit.domain.deposit.vo.Money;
import com.deposit.domain.deposit.vo.PaymentMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Deposit 도메인")
class DepositTest {

    private static final UUID TRIP_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    private static final Long USER_ID = 100L;
    private static final Money AMOUNT = Money.wons(50000);
    private static final BigDecimal CARD_FEE_RATE = new BigDecimal("0.035");
    private static final BigDecimal BANK_FEE_RATE = new BigDecimal("0.005");

    private Deposit deposit;

    @BeforeEach
    void setUp() {
        deposit = Deposit.create(TRIP_ID, USER_ID, AMOUNT);
    }

    @Test
    @DisplayName("생성 시 상태는 PENDING")
    void create_statusIsPending() {
        assertThat(deposit.getStatus()).isEqualTo(DepositStatus.PENDING);
        assertThat(deposit.getTripId()).isEqualTo(TRIP_ID);
        assertThat(deposit.getUserId()).isEqualTo(USER_ID);
        assertThat(deposit.getAmount()).isEqualTo(AMOUNT);
    }

    @Nested
    @DisplayName("결제(pay)")
    class Pay {

        @Test
        @DisplayName("카드 결제 성공 시 상태 PAID, 환불 기한 30일 후 설정")
        void pay_card_success() {
            deposit.pay("PG-TXN-001", CARD_FEE_RATE, PaymentMethod.CARD);

            assertThat(deposit.getStatus()).isEqualTo(DepositStatus.PAID);
            assertThat(deposit.getPgTransactionId()).isEqualTo("PG-TXN-001");
            assertThat(deposit.getPaidAt()).isNotNull();
            assertThat(deposit.getRefundDeadline())
                    .isAfter(LocalDateTime.now().plusDays(29))
                    .isBefore(LocalDateTime.now().plusDays(31));
        }

        @Test
        @DisplayName("이미 납부된 보증금을 다시 결제하면 예외")
        void pay_alreadyPaid_throws() {
            deposit.pay("PG-TXN-001", CARD_FEE_RATE, PaymentMethod.CARD);

            assertThatThrownBy(() -> deposit.pay("PG-TXN-002", CARD_FEE_RATE, PaymentMethod.CARD))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.DEPOSIT_ALREADY_PAID);
        }
    }

    @Nested
    @DisplayName("환불 기간 및 금액 계산")
    class RefundCalculation {

        @Test
        @DisplayName("30일 이내 환불: 전액 환불, 수수료 0")
        void refund_withinPeriod_fullRefund() {
            deposit.pay("PG-TXN-001", CARD_FEE_RATE, PaymentMethod.CARD);

            assertThat(deposit.isWithinFreeRefundPeriod()).isTrue();
            assertThat(deposit.calculateRefundAmount()).isEqualTo(AMOUNT);
            assertThat(deposit.calculatePgFee()).isEqualTo(Money.ZERO);
        }

        @Test
        @DisplayName("30일 초과 환불: 카드 수수료(3.5%) 공제")
        void refund_afterPeriod_cardFeeDeducted() {
            Deposit expiredDeposit = Deposit.builder()
                    .tripId(TRIP_ID)
                    .userId(USER_ID)
                    .amount(AMOUNT)
                    .status(DepositStatus.PAID)
                    .pgTransactionId("PG-TXN-001")
                    .pgFeeRate(CARD_FEE_RATE)
                    .paymentMethod(PaymentMethod.CARD)
                    .paidAt(LocalDateTime.now().minusDays(31))
                    .refundDeadline(LocalDateTime.now().minusDays(1))
                    .createdAt(LocalDateTime.now().minusDays(31))
                    .build();

            assertThat(expiredDeposit.isWithinFreeRefundPeriod()).isFalse();
            assertThat(expiredDeposit.calculatePgFee().getAmount())
                    .isEqualByComparingTo("1750"); // 50000 * 3.5%
            assertThat(expiredDeposit.calculateRefundAmount().getAmount())
                    .isEqualByComparingTo("48250"); // 50000 - 1750
        }

        @Test
        @DisplayName("30일 초과 환불: 계좌이체 수수료(0.5%) 공제")
        void refund_afterPeriod_bankTransferFeeDeducted() {
            Deposit expiredDeposit = Deposit.builder()
                    .tripId(TRIP_ID)
                    .userId(USER_ID)
                    .amount(AMOUNT)
                    .status(DepositStatus.PAID)
                    .pgTransactionId("PG-TXN-002")
                    .pgFeeRate(BANK_FEE_RATE)
                    .paymentMethod(PaymentMethod.BANK_TRANSFER)
                    .paidAt(LocalDateTime.now().minusDays(31))
                    .refundDeadline(LocalDateTime.now().minusDays(1))
                    .createdAt(LocalDateTime.now().minusDays(31))
                    .build();

            assertThat(expiredDeposit.calculatePgFee().getAmount())
                    .isEqualByComparingTo("250"); // 50000 * 0.5%
            assertThat(expiredDeposit.calculateRefundAmount().getAmount())
                    .isEqualByComparingTo("49750"); // 50000 - 250
        }
    }

    @Nested
    @DisplayName("환불 요청(requestRefund)")
    class RequestRefund {

        @Test
        @DisplayName("납부 완료 상태에서 환불 요청 성공")
        void requestRefund_success() {
            deposit.pay("PG-TXN-001", CARD_FEE_RATE, PaymentMethod.CARD);
            deposit.requestRefund();

            assertThat(deposit.getStatus()).isEqualTo(DepositStatus.REFUND_REQUESTED);
        }

        @Test
        @DisplayName("미납 상태에서 환불 요청 시 예외")
        void requestRefund_notPaid_throws() {
            assertThatThrownBy(() -> deposit.requestRefund())
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.DEPOSIT_NOT_PAID);
        }

        @Test
        @DisplayName("이미 환불 완료된 경우 예외")
        void requestRefund_alreadyRefunded_throws() {
            deposit.pay("PG-TXN-001", CARD_FEE_RATE, PaymentMethod.CARD);
            deposit.requestRefund();
            deposit.completeRefund();

            assertThatThrownBy(() -> deposit.requestRefund())
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.DEPOSIT_ALREADY_REFUNDED);
        }

        @Test
        @DisplayName("환불 진행 중 중복 요청 시 예외")
        void requestRefund_inProgress_throws() {
            deposit.pay("PG-TXN-001", CARD_FEE_RATE, PaymentMethod.CARD);
            deposit.requestRefund();

            assertThatThrownBy(() -> deposit.requestRefund())
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.DEPOSIT_REFUND_IN_PROGRESS);
        }
    }

    @Nested
    @DisplayName("isPaid()")
    class IsPaid {

        @Test
        @DisplayName("PENDING 상태는 미납 처리")
        void isPaid_pending_false() {
            assertThat(deposit.isPaid()).isFalse();
        }

        @Test
        @DisplayName("PAID / REFUND_REQUESTED / REFUNDED 는 납부 처리")
        void isPaid_paidStatuses_true() {
            deposit.pay("PG-TXN-001", CARD_FEE_RATE, PaymentMethod.CARD);
            assertThat(deposit.isPaid()).isTrue();

            deposit.requestRefund();
            assertThat(deposit.isPaid()).isTrue();

            deposit.completeRefund();
            assertThat(deposit.isPaid()).isTrue();
        }
    }
}
