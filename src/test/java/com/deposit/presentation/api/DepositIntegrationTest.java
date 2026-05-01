package com.deposit.presentation.api;

import com.deposit.application.deposit.dto.command.CreateDepositPolicyCommand;
import com.deposit.application.deposit.dto.command.RequestDepositCommand;
import com.deposit.application.deposit.dto.result.DepositResult;
import com.deposit.application.deposit.port.in.CreateDepositPolicyUseCase;
import com.deposit.application.deposit.port.in.RequestDepositUseCase;
import com.deposit.domain.deposit.vo.Money;
import com.deposit.domain.deposit.vo.PaymentMethod;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

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

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired CreateDepositPolicyUseCase createDepositPolicyUseCase;
    @Autowired RequestDepositUseCase requestDepositUseCase;

    // ──────────────────────────────────────────────
    // 보증금 정책 API
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/trips/{tripId}/deposit-policy → 정책 생성 성공")
    void createPolicy_returns201() throws Exception {
        Map<String, Object> body = Map.of(
                "organizerId", 10,
                "depositAmount", 50000
        );

        mockMvc.perform(post("/api/trips/" + TRIP_1 + "/deposit-policy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tripId").value(TRIP_1.toString()))
                .andExpect(jsonPath("$.data.depositAmount").value(50000));
    }

    @Test
    @DisplayName("POST /api/trips/{tripId}/deposit-policy → 중복 정책 생성 시 409")
    void createPolicy_duplicate_returns409() throws Exception {
        createDepositPolicyUseCase.createPolicy(CreateDepositPolicyCommand.builder()
                .tripId(TRIP_2).organizerId(10L).depositAmount(Money.wons(50000)).build());

        Map<String, Object> body = Map.of("organizerId", 10, "depositAmount", 50000);

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
    // 헬퍼 메서드
    // ──────────────────────────────────────────────

    private Long createDepositAndGetId(UUID tripId, Long userId) {
        createDepositPolicyUseCase.createPolicy(CreateDepositPolicyCommand.builder()
                .tripId(tripId).organizerId(10L).depositAmount(Money.wons(50000)).build());
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
}
