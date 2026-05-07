package com.deposit.presentation.api;

import com.deposit.application.deposit.dto.command.CreateDepositPolicyCommand;
import com.deposit.application.deposit.dto.command.RequestDepositCommand;
import com.deposit.application.deposit.dto.result.DepositResult;
import com.deposit.application.deposit.port.in.AutoRefundUnconfirmedTripsUseCase;
import com.deposit.application.deposit.port.in.ConfirmTripUseCase;
import com.deposit.application.deposit.port.in.CreateDepositPolicyUseCase;
import com.deposit.application.deposit.port.in.RequestDepositUseCase;
import com.deposit.application.deposit.port.out.SaveDepositPort;
import com.deposit.application.deposit.port.out.SaveDepositPolicyPort;
import com.deposit.domain.deposit.Deposit;
import com.deposit.domain.deposit.DepositPolicy;
import com.deposit.domain.deposit.vo.DepositStatus;
import com.deposit.domain.deposit.vo.Money;
import com.deposit.domain.deposit.vo.PaymentMethod;
import com.deposit.domain.deposit.vo.PgProvider;
import com.deposit.domain.deposit.vo.PolicyStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("보증금 통합 테스트")
class DepositIntegrationTest {

    private static final UUID TRIP_1  = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID TRIP_2  = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID TRIP_3  = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID TRIP_4  = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID TRIP_5  = UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final UUID TRIP_6  = UUID.fromString("00000000-0000-0000-0000-000000000006");
    private static final UUID TRIP_7  = UUID.fromString("00000000-0000-0000-0000-000000000007");
    private static final UUID TRIP_8  = UUID.fromString("00000000-0000-0000-0000-000000000008");
    private static final UUID TRIP_9  = UUID.fromString("00000000-0000-0000-0000-000000000009");
    private static final UUID TRIP_10 = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID TRIP_11 = UUID.fromString("00000000-0000-0000-0000-000000000011");
    private static final UUID TRIP_12 = UUID.fromString("00000000-0000-0000-0000-000000000012");
    private static final UUID TRIP_13 = UUID.fromString("00000000-0000-0000-0000-000000000013");
    private static final UUID TRIP_14 = UUID.fromString("00000000-0000-0000-0000-000000000014");
    private static final UUID TRIP_15 = UUID.fromString("00000000-0000-0000-0000-000000000015");
    private static final UUID TRIP_16 = UUID.fromString("00000000-0000-0000-0000-000000000016");
    private static final UUID TRIP_17 = UUID.fromString("00000000-0000-0000-0000-000000000017");
    private static final UUID TRIP_18 = UUID.fromString("00000000-0000-0000-0000-000000000018");
    private static final UUID TRIP_19 = UUID.fromString("00000000-0000-0000-0000-000000000019");
    private static final UUID TRIP_20 = UUID.fromString("00000000-0000-0000-0000-000000000020");

    private static final BigDecimal LATE_REFUND_FEE_RATE = new BigDecimal("0.035");

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired CreateDepositPolicyUseCase createDepositPolicyUseCase;
    @Autowired RequestDepositUseCase requestDepositUseCase;
    @Autowired ConfirmTripUseCase confirmTripUseCase;
    @Autowired AutoRefundUnconfirmedTripsUseCase autoRefundUnconfirmedTripsUseCase;
    @Autowired SaveDepositPort saveDepositPort;
    @Autowired SaveDepositPolicyPort saveDepositPolicyPort;

    // ──────────────────────────────────────────────
    // 보증금 정책 API
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/trips/{tripId}/deposit-policy → PG사·여행시작시각 포함 정책 생성 성공")
    void createPolicy_returns201() throws Exception {
        Map<String, Object> body = Map.of(
                "organizerId", 10,
                "depositAmount", 50000,
                "pgProvider", "TOSS_PAYMENTS",
                "tripStartAt", LocalDateTime.now().plusDays(7).toString()
        );

        mockMvc.perform(post("/api/trips/" + TRIP_1 + "/deposit-policy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tripId").value(TRIP_1.toString()))
                .andExpect(jsonPath("$.data.depositAmount").value(50000))
                .andExpect(jsonPath("$.data.pgProvider").value("TOSS_PAYMENTS"))
                .andExpect(jsonPath("$.data.tripStartAt").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/trips/{tripId}/deposit-policy → 중복 정책 생성 시 409")
    void createPolicy_duplicate_returns409() throws Exception {
        createDepositPolicyUseCase.createPolicy(CreateDepositPolicyCommand.builder()
                .tripId(TRIP_2).organizerId(10L).depositAmount(Money.wons(50000))
                .pgProvider(com.deposit.domain.deposit.vo.PgProvider.TOSS_PAYMENTS)
                .tripStartAt(LocalDateTime.now().plusDays(7)).build());

        Map<String, Object> body = Map.of(
                "organizerId", 10, "depositAmount", 50000,
                "pgProvider", "TOSS_PAYMENTS",
                "tripStartAt", LocalDateTime.now().plusDays(7).toString()
        );

        mockMvc.perform(post("/api/trips/" + TRIP_2 + "/deposit-policy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/trips/{tripId}/deposit-policy/requests → 참가자 보증금 요청")
    void requestDeposits_returns201() throws Exception {
        createDepositPolicyUseCase.createPolicy(CreateDepositPolicyCommand.builder()
                .tripId(TRIP_3).organizerId(10L).depositAmount(Money.wons(30000)).build());

        Map<String, Object> body = Map.of(
                "organizerId", 10,
                "userIds", List.of(101, 102, 103)
        );

        mockMvc.perform(post("/api/trips/" + TRIP_3 + "/deposit-policy/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"));
    }

    @Test
    @DisplayName("GET /api/trips/{tripId}/deposit-policy/status → 미납자 있을 때 allPaid=false")
    void getStatus_hasUnpaid() throws Exception {
        createDepositPolicyUseCase.createPolicy(CreateDepositPolicyCommand.builder()
                .tripId(TRIP_4).organizerId(10L).depositAmount(Money.wons(50000)).build());
        requestDepositUseCase.requestDeposits(RequestDepositCommand.builder()
                .tripId(TRIP_4).organizerId(10L).userIds(List.of(101L, 102L)).build());

        mockMvc.perform(get("/api/trips/" + TRIP_4 + "/deposit-policy/status"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.allPaid").value(false))
                .andExpect(jsonPath("$.data.unpaidCount").value(2))
                .andExpect(jsonPath("$.message").value(containsString("시작할 수 없습니다")));
    }

    // ──────────────────────────────────────────────
    // 보증금 결제 API
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/deposits/{depositId}/pay → 카드 결제 성공 + 30일 안내 반환")
    void pay_card_success() throws Exception {
        Long depositId = createDepositAndGetId(TRIP_5, 101L);

        Map<String, Object> body = Map.of(
                "userId", 101,
                "paymentMethod", "CARD"
        );

        mockMvc.perform(post("/api/deposits/" + depositId + "/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAID"))
                .andExpect(jsonPath("$.message").value(containsString("30일")))
                .andExpect(jsonPath("$.message").value(containsString("3.5%")));
    }

    @Test
    @DisplayName("POST /api/deposits/{depositId}/pay → 계좌이체 결제 성공 + 0.5% 안내")
    void pay_bankTransfer_success() throws Exception {
        Long depositId = createDepositAndGetId(TRIP_6, 101L);

        Map<String, Object> body = Map.of(
                "userId", 101,
                "paymentMethod", "BANK_TRANSFER"
        );

        mockMvc.perform(post("/api/deposits/" + depositId + "/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAID"))
                .andExpect(jsonPath("$.message").value(containsString("0.5%")));
    }

    @Test
    @DisplayName("전원 납부 후 status 조회 → allPaid=true")
    void getStatus_allPaid_afterPay() throws Exception {
        createDepositPolicyUseCase.createPolicy(CreateDepositPolicyCommand.builder()
                .tripId(TRIP_7).organizerId(10L).depositAmount(Money.wons(50000)).build());
        List<DepositResult> deposits = requestDepositUseCase.requestDeposits(
                RequestDepositCommand.builder()
                        .tripId(TRIP_7).organizerId(10L).userIds(List.of(101L)).build());

        Long depositId = deposits.get(0).getId();

        mockMvc.perform(post("/api/deposits/" + depositId + "/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("userId", 101, "paymentMethod", "CARD"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/trips/" + TRIP_7 + "/deposit-policy/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.allPaid").value(true))
                .andExpect(jsonPath("$.message").value(containsString("시작할 수 있습니다")));
    }

    // ──────────────────────────────────────────────
    // 환불 API
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/deposits/{depositId}/refund → 30일 이내 전액 환불")
    void refund_withinPeriod_fullRefund() throws Exception {
        Long depositId = createDepositAndGetId(TRIP_8, 101L);
        payDeposit(depositId, PaymentMethod.CARD);

        mockMvc.perform(post("/api/deposits/" + depositId + "/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("userId", 101))))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.feeDeducted").value(false))
                .andExpect(jsonPath("$.data.pgFeeAmount").value(0))
                .andExpect(jsonPath("$.data.refundedAmount").value(50000))
                .andExpect(jsonPath("$.message").value(containsString("전액 환불")));
    }

    @Test
    @DisplayName("미납 보증금 환불 요청 → 400")
    void refund_notPaid_returns400() throws Exception {
        Long depositId = createDepositAndGetId(TRIP_9, 101L);

        mockMvc.perform(post("/api/deposits/" + depositId + "/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("userId", 101))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("GET /api/deposits/{depositId} → 납부 후 30일 안내 문구 포함")
    void getDeposit_includesRefundNotice() throws Exception {
        Long depositId = createDepositAndGetId(TRIP_10, 101L);
        payDeposit(depositId, PaymentMethod.CARD);

        mockMvc.perform(get("/api/deposits/" + depositId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.refundNotice").value(containsString("30일")))
                .andExpect(jsonPath("$.data.refundDeadline").isNotEmpty());
    }

    @Test
    @DisplayName("유효성 검사 실패 → 400")
    void pay_missingPaymentMethod_returns400() throws Exception {
        Long depositId = createDepositAndGetId(TRIP_11, 101L);

        Map<String, Object> body = Map.of("userId", 101);  // paymentMethod 누락

        mockMvc.perform(post("/api/deposits/" + depositId + "/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ──────────────────────────────────────────────
    // 30일 이후 환불 — 3.5% 수수료 공제
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/deposits/{depositId}/refund → 30일 이후 카드 결제: 3.5% 공제 후 환불")
    void refund_afterPeriod_card_feeDeducted() throws Exception {
        // 50000원, 결제일 31일 전 → refundDeadline 이미 지남
        Long depositId = saveExpiredDeposit(TRIP_12, 101L, Money.wons(50000), PaymentMethod.CARD);

        mockMvc.perform(post("/api/deposits/" + depositId + "/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("userId", 101))))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.feeDeducted").value(true))
                .andExpect(jsonPath("$.data.pgFeeAmount").value(1750))      // 50000 * 3.5%
                .andExpect(jsonPath("$.data.refundedAmount").value(48250))  // 50000 - 1750
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.message").value(containsString("1750")))
                .andExpect(jsonPath("$.message").value(containsString("48250")));
    }

    @Test
    @DisplayName("30일 이후 환불 — 보증금 금액이 달라도 개인별 3.5% 공제 금액 환불")
    void refund_afterPeriod_customAmount_feeDeducted() throws Exception {
        // 100000원, 3.5% = 3500원 공제 → 96500원 환불
        Long depositId = saveExpiredDeposit(TRIP_13, 201L, Money.wons(100000), PaymentMethod.CARD);

        mockMvc.perform(post("/api/deposits/" + depositId + "/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("userId", 201))))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.feeDeducted").value(true))
                .andExpect(jsonPath("$.data.pgFeeAmount").value(3500))
                .andExpect(jsonPath("$.data.refundedAmount").value(96500));
    }

    @Test
    @DisplayName("30일 이후 복수 사용자 각자 개별 3.5% 공제 환불")
    void refund_afterPeriod_multipleUsers_eachFeeDeducted() throws Exception {
        // 사용자 A: 50000원 → 환불 48250원
        Long depositIdA = saveExpiredDeposit(TRIP_14, 301L, Money.wons(50000), PaymentMethod.CARD);
        // 사용자 B: 30000원 → 환불 28950원  (30000 * 3.5% = 1050)
        Long depositIdB = saveExpiredDeposit(TRIP_14, 302L, Money.wons(30000), PaymentMethod.CARD);

        mockMvc.perform(post("/api/deposits/" + depositIdA + "/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("userId", 301))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.feeDeducted").value(true))
                .andExpect(jsonPath("$.data.pgFeeAmount").value(1750))
                .andExpect(jsonPath("$.data.refundedAmount").value(48250));

        mockMvc.perform(post("/api/deposits/" + depositIdB + "/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("userId", 302))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.feeDeducted").value(true))
                .andExpect(jsonPath("$.data.pgFeeAmount").value(1050))
                .andExpect(jsonPath("$.data.refundedAmount").value(28950));
    }

    // ──────────────────────────────────────────────
    // 헬퍼 메서드
    // ──────────────────────────────────────────────

    // ──────────────────────────────────────────────
    // 여행 확정 (전원 납부 완료 후 방장이 확정)
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("POST confirm → 전원 납부 완료 → 여행 확정 성공, CONFIRMED")
    void confirmTrip_allPaid_success() throws Exception {
        Long depositId = createDepositAndGetId(TRIP_15, 101L);
        payDeposit(depositId, PaymentMethod.CARD);

        mockMvc.perform(post("/api/trips/" + TRIP_15 + "/deposit-policy/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("organizerId", 10))))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.confirmedDeposits").value(1))
                .andExpect(jsonPath("$.message").value(containsString("확정")));
    }

    @Test
    @DisplayName("POST confirm → 미납 인원 있으면 400")
    void confirmTrip_hasUnpaid_returns400() throws Exception {
        createDepositAndGetId(TRIP_16, 101L);
        // 납부 안 함

        mockMvc.perform(post("/api/trips/" + TRIP_16 + "/deposit-policy/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("organizerId", 10))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST confirm → 이미 확정된 여행 재확정 시 409")
    void confirmTrip_alreadyConfirmed_returns409() throws Exception {
        Long depositId = createDepositAndGetId(TRIP_17, 101L);
        payDeposit(depositId, PaymentMethod.CARD);
        // 첫 번째 확정
        mockMvc.perform(post("/api/trips/" + TRIP_17 + "/deposit-policy/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("organizerId", 10))))
                .andExpect(status().isOk());

        // 두 번째 확정 → 409
        mockMvc.perform(post("/api/trips/" + TRIP_17 + "/deposit-policy/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("organizerId", 10))))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("여행 시작 시간 초과 + 미확정 → 자동 환불 후 정책 CANCELLED")
    void autoRefund_expiredUnconfirmedTrip_refundsAndCancels() throws Exception {
        // 1) 정책 저장 (tripStartAt 이미 과거)
        DepositPolicy expiredPolicy = saveExpiredPolicy(TRIP_18);

        // 2) 납부자 2명 등록 (PAID 상태로 직접 저장)
        Deposit d1 = saveDepositWithStatus(TRIP_18, 501L, DepositStatus.PAID);
        Deposit d2 = saveDepositWithStatus(TRIP_18, 502L, DepositStatus.PAID);

        // 3) 자동 환불 트리거
        mockMvc.perform(post("/api/trips/" + TRIP_18 + "/deposit-policy/auto-refund"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.processedTrips").value(1))
                .andExpect(jsonPath("$.data.refundedDeposits").value(2))
                .andExpect(jsonPath("$.message").value(containsString("자동 환불")));
    }

    @Test
    @DisplayName("TOSS_PAYMENTS PG 결제 성공")
    void pay_tossPayments_success() throws Exception {
        Long depositId = createDepositAndGetId(TRIP_19, 101L, PgProvider.TOSS_PAYMENTS);

        mockMvc.perform(post("/api/deposits/" + depositId + "/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("userId", 101, "paymentMethod", "CARD"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAID"));
    }

    @Test
    @DisplayName("KG_INICIS PG 결제 성공")
    void pay_kgInicis_success() throws Exception {
        Long depositId = createDepositAndGetId(TRIP_20, 101L, PgProvider.KG_INICIS);

        mockMvc.perform(post("/api/deposits/" + depositId + "/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("userId", 101, "paymentMethod", "CARD"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAID"));
    }

    // ──────────────────────────────────────────────
    // 헬퍼 메서드
    // ──────────────────────────────────────────────

    private Long createDepositAndGetId(UUID tripId, Long userId) {
        return createDepositAndGetId(tripId, userId, PgProvider.TOSS_PAYMENTS);
    }

    private Long createDepositAndGetId(UUID tripId, Long userId, PgProvider pgProvider) {
        createDepositPolicyUseCase.createPolicy(CreateDepositPolicyCommand.builder()
                .tripId(tripId).organizerId(10L).depositAmount(Money.wons(50000))
                .pgProvider(pgProvider).tripStartAt(LocalDateTime.now().plusDays(7)).build());
        List<DepositResult> deposits = requestDepositUseCase.requestDeposits(
                RequestDepositCommand.builder()
                        .tripId(tripId).organizerId(10L).userIds(List.of(userId)).build());
        return deposits.get(0).getId();
    }

    private void payDeposit(Long depositId, PaymentMethod method) throws Exception {
        mockMvc.perform(post("/api/deposits/" + depositId + "/pay")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        Map.of("userId", 101, "paymentMethod", method.name()))))
                .andExpect(status().isOk());
    }

    /** 30일 환불 기간이 이미 만료된 보증금을 직접 저장 */
    private Long saveExpiredDeposit(UUID tripId, Long userId, Money amount, PaymentMethod method) {
        Deposit expired = Deposit.builder()
                .tripId(tripId).userId(userId).amount(amount)
                .status(DepositStatus.PAID)
                .pgTransactionId("PG-TXN-EXPIRED-" + userId)
                .pgFeeRate(LATE_REFUND_FEE_RATE).paymentMethod(method)
                .paidAt(LocalDateTime.now().minusDays(31))
                .refundDeadline(LocalDateTime.now().minusDays(1))
                .createdAt(LocalDateTime.now().minusDays(31))
                .build();
        return saveDepositPort.save(expired).getId();
    }

    /** 여행 시작 시간이 이미 과거인 ACTIVE 정책 직접 저장 (자동 환불 테스트용) */
    private DepositPolicy saveExpiredPolicy(UUID tripId) {
        DepositPolicy policy = DepositPolicy.create(
                tripId, 10L, Money.wons(50000),
                PgProvider.TOSS_PAYMENTS,
                LocalDateTime.now().minusHours(1)  // 여행 시작 시간 이미 지남
        );
        return saveDepositPolicyPort.save(policy);
    }

    /** 지정 상태의 보증금 직접 저장 */
    private Deposit saveDepositWithStatus(UUID tripId, Long userId, DepositStatus status) {
        Deposit deposit = Deposit.builder()
                .tripId(tripId).userId(userId).amount(Money.wons(50000))
                .status(status)
                .pgTransactionId(status == DepositStatus.PAID ? "PG-TXN-" + userId : null)
                .pgFeeRate(status == DepositStatus.PAID ? LATE_REFUND_FEE_RATE : null)
                .paymentMethod(status == DepositStatus.PAID ? PaymentMethod.CARD : null)
                .paidAt(status == DepositStatus.PAID ? LocalDateTime.now().minusDays(5) : null)
                .refundDeadline(status == DepositStatus.PAID ? LocalDateTime.now().plusDays(25) : null)
                .createdAt(LocalDateTime.now().minusDays(10))
                .build();
        return saveDepositPort.save(deposit);
    }
}
