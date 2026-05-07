package com.deposit.presentation.settlement;

import com.deposit.application.settlement.dto.command.CreateSettlementCommand;
import com.deposit.application.settlement.port.in.CreateSettlementUseCase;
import com.deposit.domain.deposit.vo.Money;
import com.deposit.domain.settlement.vo.SettlementType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("정산 통합 테스트")
class SettlementIntegrationTest {

    private static final UUID TRIP_1  = UUID.fromString("aa000000-0000-0000-0000-000000000001");
    private static final UUID TRIP_2  = UUID.fromString("aa000000-0000-0000-0000-000000000002");
    private static final UUID TRIP_3  = UUID.fromString("aa000000-0000-0000-0000-000000000003");
    private static final UUID TRIP_4  = UUID.fromString("aa000000-0000-0000-0000-000000000004");
    private static final UUID TRIP_5  = UUID.fromString("aa000000-0000-0000-0000-000000000005");
    private static final UUID TRIP_6  = UUID.fromString("aa000000-0000-0000-0000-000000000006");
    private static final UUID TRIP_7  = UUID.fromString("aa000000-0000-0000-0000-000000000007");
    private static final UUID TRIP_8  = UUID.fromString("aa000000-0000-0000-0000-000000000008");
    private static final UUID TRIP_9  = UUID.fromString("aa000000-0000-0000-0000-000000000009");
    private static final UUID TRIP_10 = UUID.fromString("aa000000-0000-0000-0000-000000000010");
    private static final UUID TRIP_11 = UUID.fromString("aa000000-0000-0000-0000-000000000011");
    private static final UUID TRIP_12 = UUID.fromString("aa000000-0000-0000-0000-000000000012");
    private static final UUID TRIP_13 = UUID.fromString("aa000000-0000-0000-0000-000000000013");
    private static final UUID TRIP_14 = UUID.fromString("aa000000-0000-0000-0000-000000000014");
    private static final UUID TRIP_15 = UUID.fromString("aa000000-0000-0000-0000-000000000015");
    private static final UUID TRIP_16 = UUID.fromString("aa000000-0000-0000-0000-000000000016");

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired CreateSettlementUseCase createSettlementUseCase;

    // ──────────────────────────────────────────────
    // 즉시 정산
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("POST 즉시 정산 → 201, status=NOTIFIED, 알림 메시지 반환")
    void createImmediate_returns201WithNotification() throws Exception {
        Map<String, Object> body = Map.of(
                "payerId", 10,
                "debtorId", 20,
                "amount", 15000,
                "description", "점심값",
                "type", "IMMEDIATE"
        );

        mockMvc.perform(post("/api/trips/" + TRIP_1 + "/settlements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.type").value("IMMEDIATE"))
                .andExpect(jsonPath("$.data.status").value("NOTIFIED"))
                .andExpect(jsonPath("$.data.notificationMessage").value(containsString("20")))
                .andExpect(jsonPath("$.message").value(containsString("알림")));
    }

    // ──────────────────────────────────────────────
    // 나중에 정산
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("POST 나중에 정산 → 201, status=ACCUMULATED, 누적 메시지 반환")
    void createDeferred_returns201Accumulated() throws Exception {
        Map<String, Object> body = Map.of(
                "payerId", 10,
                "debtorId", 20,
                "amount", 30000,
                "description", "숙박비",
                "type", "DEFERRED"
        );

        mockMvc.perform(post("/api/trips/" + TRIP_2 + "/settlements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.type").value("DEFERRED"))
                .andExpect(jsonPath("$.data.status").value("ACCUMULATED"))
                .andExpect(jsonPath("$.data.notificationMessage").doesNotExist())
                .andExpect(jsonPath("$.message").value(containsString("누적")));
    }

    @Test
    @DisplayName("나중에 정산 2회 → 잔액이 누적됨")
    void createDeferred_twice_balanceAccumulates() throws Exception {
        Map<String, Object> body1 = Map.of("payerId", 10, "debtorId", 20, "amount", 20000,
                "description", "교통비", "type", "DEFERRED");
        Map<String, Object> body2 = Map.of("payerId", 10, "debtorId", 20, "amount", 30000,
                "description", "숙박비", "type", "DEFERRED");

        mockMvc.perform(post("/api/trips/" + TRIP_3 + "/settlements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/trips/" + TRIP_3 + "/settlements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body2)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/trips/" + TRIP_3 + "/settlements/balances"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].totalAmount").value(50000));
    }

    // ──────────────────────────────────────────────
    // 조회
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("GET 전체 정산 내역 조회 → 즉시/나중에 모두 반환")
    void getSettlements_returnsMixed() throws Exception {
        createSettlement(TRIP_4, 10L, 20L, 10000, "점심값", SettlementType.IMMEDIATE);
        createSettlement(TRIP_4, 10L, 30L, 25000, "숙박비", SettlementType.DEFERRED);

        mockMvc.perform(get("/api/trips/" + TRIP_4 + "/settlements"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    @DisplayName("GET 누적 잔액 전체 조회 → DEFERRED 항목만 잔액에 반영")
    void getBalances_onlyDeferredAccumulated() throws Exception {
        createSettlement(TRIP_5, 10L, 20L, 10000, "점심값", SettlementType.IMMEDIATE);  // 잔액 없음
        createSettlement(TRIP_5, 10L, 30L, 25000, "숙박비", SettlementType.DEFERRED);
        createSettlement(TRIP_5, 10L, 40L, 15000, "교통비", SettlementType.DEFERRED);

        mockMvc.perform(get("/api/trips/" + TRIP_5 + "/settlements/balances"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    @DisplayName("GET 특정 채무자 잔액 조회")
    void getBalancesByDebtor_filtered() throws Exception {
        createSettlement(TRIP_6, 10L, 20L, 10000, "A 비용", SettlementType.DEFERRED);
        createSettlement(TRIP_6, 10L, 30L, 20000, "B 비용", SettlementType.DEFERRED);

        mockMvc.perform(get("/api/trips/" + TRIP_6 + "/settlements/balances/debtor/20"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].debtorId").value(20))
                .andExpect(jsonPath("$.data[0].totalAmount").value(10000));
    }

    // ──────────────────────────────────────────────
    // 정산 금액 수정
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("PATCH 즉시 정산 금액 수정 → 수정된 금액으로 알림 재발송, 알림 메시지 반환")
    void updateImmediate_resendNotification() throws Exception {
        Long settlementId = createSettlementAndGetId(TRIP_1, 10L, 20L, 15000, "점심값", SettlementType.IMMEDIATE);

        Map<String, Object> body = Map.of("amount", 25000);

        mockMvc.perform(patch("/api/trips/" + TRIP_1 + "/settlements/" + settlementId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.amount").value(25000))
                .andExpect(jsonPath("$.data.status").value("NOTIFIED"))
                .andExpect(jsonPath("$.message").value(containsString("알림")));
    }

    @Test
    @DisplayName("PATCH 나중에 정산 금액 증가 수정 → 잔액 증가")
    void updateDeferred_increaseAmount_balanceIncreases() throws Exception {
        Long settlementId = createSettlementAndGetId(TRIP_2, 10L, 20L, 20000, "교통비", SettlementType.DEFERRED);

        Map<String, Object> body = Map.of("amount", 35000);

        mockMvc.perform(patch("/api/trips/" + TRIP_2 + "/settlements/" + settlementId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.amount").value(35000))
                .andExpect(jsonPath("$.data.status").value("ACCUMULATED"))
                .andExpect(jsonPath("$.message").value(containsString("35000")));

        // 잔액도 35000 으로 변경됐는지 확인
        mockMvc.perform(get("/api/trips/" + TRIP_2 + "/settlements/balances"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].totalAmount").value(35000));
    }

    @Test
    @DisplayName("PATCH 나중에 정산 금액 감소 수정 → 잔액 감소")
    void updateDeferred_decreaseAmount_balanceDecreases() throws Exception {
        Long settlementId = createSettlementAndGetId(TRIP_3, 10L, 20L, 50000, "숙박비", SettlementType.DEFERRED);

        Map<String, Object> body = Map.of("amount", 30000);

        mockMvc.perform(patch("/api/trips/" + TRIP_3 + "/settlements/" + settlementId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.amount").value(30000));

        mockMvc.perform(get("/api/trips/" + TRIP_3 + "/settlements/balances"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].totalAmount").value(30000));
    }

    @Test
    @DisplayName("PATCH 다른 정산 금액 수정 시 기존 정산 잔액 유지")
    void updateDeferred_otherSettlementUnchanged() throws Exception {
        // 같은 trip, 같은 payer-debtor 조합에 정산 2건 (누적 50000)
        createSettlement(TRIP_4, 10L, 20L, 20000, "교통비", SettlementType.DEFERRED);
        Long secondId = createSettlementAndGetId(TRIP_4, 10L, 20L, 30000, "식비", SettlementType.DEFERRED);

        // 두 번째 정산만 30000 → 40000 수정
        mockMvc.perform(patch("/api/trips/" + TRIP_4 + "/settlements/" + secondId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("amount", 40000))))
                .andExpect(status().isOk());

        // 잔액: 20000 + 40000 = 60000
        mockMvc.perform(get("/api/trips/" + TRIP_4 + "/settlements/balances"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].totalAmount").value(60000));
    }

    @Test
    @DisplayName("PATCH 취소된 정산 수정 → 409")
    void update_cancelledSettlement_returns409() throws Exception {
        Long settlementId = createSettlementAndGetId(TRIP_6, 10L, 20L, 15000, "점심값", SettlementType.IMMEDIATE);
        mockMvc.perform(delete("/api/trips/" + TRIP_6 + "/settlements/" + settlementId))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/trips/" + TRIP_6 + "/settlements/" + settlementId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("amount", 20000))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("PATCH 존재하지 않는 정산 수정 → 404")
    void update_notFound_returns404() throws Exception {
        mockMvc.perform(patch("/api/trips/" + TRIP_5 + "/settlements/99999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("amount", 10000))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("PATCH amount 0원 → 400")
    void update_zeroAmount_returns400() throws Exception {
        Long settlementId = createSettlementAndGetId(TRIP_5, 10L, 20L, 10000, "점심값", SettlementType.IMMEDIATE);

        mockMvc.perform(patch("/api/trips/" + TRIP_5 + "/settlements/" + settlementId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("amount", 0))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ──────────────────────────────────────────────
    // 정산 취소
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("DELETE 즉시 정산 취소 → 200, status=CANCELLED, 취소 알림 메시지 반환")
    void cancelImmediate_returns200WithCancellationMessage() throws Exception {
        Long settlementId = createSettlementAndGetId(TRIP_7, 10L, 20L, 15000, "점심값", SettlementType.IMMEDIATE);

        mockMvc.perform(delete("/api/trips/" + TRIP_7 + "/settlements/" + settlementId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.type").value("IMMEDIATE"))
                .andExpect(jsonPath("$.data.notificationMessage").value(containsString("20")))
                .andExpect(jsonPath("$.message").value(containsString("취소 알림")));
    }

    @Test
    @DisplayName("DELETE 나중에 정산 취소 → 200, status=CANCELLED, 잔액 차감됨")
    void cancelDeferred_returns200AndDeductsBalance() throws Exception {
        createSettlement(TRIP_8, 10L, 20L, 20000, "교통비", SettlementType.DEFERRED);
        Long settlementId = createSettlementAndGetId(TRIP_8, 10L, 20L, 30000, "숙박비", SettlementType.DEFERRED);
        // 누적 잔액 = 50000

        mockMvc.perform(delete("/api/trips/" + TRIP_8 + "/settlements/" + settlementId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.message").value("정산이 취소되었습니다."));

        // 잔액 50000 - 30000 = 20000
        mockMvc.perform(get("/api/trips/" + TRIP_8 + "/settlements/balances"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].totalAmount").value(20000));
    }

    @Test
    @DisplayName("DELETE 나중에 정산 전체 취소 → 잔액 0원")
    void cancelDeferred_allCancelled_balanceBecomesZero() throws Exception {
        Long settlementId = createSettlementAndGetId(TRIP_9, 10L, 20L, 30000, "숙박비", SettlementType.DEFERRED);

        mockMvc.perform(delete("/api/trips/" + TRIP_9 + "/settlements/" + settlementId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/trips/" + TRIP_9 + "/settlements/balances"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].totalAmount").value(0));
    }

    @Test
    @DisplayName("DELETE 이미 취소된 정산 재취소 → 409")
    void cancelAlreadyCancelled_returns409() throws Exception {
        Long settlementId = createSettlementAndGetId(TRIP_10, 10L, 20L, 15000, "점심값", SettlementType.IMMEDIATE);
        mockMvc.perform(delete("/api/trips/" + TRIP_10 + "/settlements/" + settlementId))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/trips/" + TRIP_10 + "/settlements/" + settlementId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("DELETE 존재하지 않는 정산 취소 → 404")
    void cancelNotFound_returns404() throws Exception {
        mockMvc.perform(delete("/api/trips/" + TRIP_11 + "/settlements/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ──────────────────────────────────────────────
    // 정산 현황 요약 조회
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("GET summary: 활성/취소 정산과 누적 잔액을 분리하여 반환")
    void getSummary_separatesActiveAndCancelledAndBalances() throws Exception {
        // 즉시 정산 2건
        Long cancelTarget = createSettlementAndGetId(TRIP_12, 10L, 20L, 15000, "점심값", SettlementType.IMMEDIATE);
        createSettlement(TRIP_12, 10L, 30L, 10000, "커피값", SettlementType.IMMEDIATE);
        // 나중에 정산 1건 (잔액 누적)
        createSettlement(TRIP_12, 10L, 20L, 25000, "숙박비", SettlementType.DEFERRED);
        // 즉시 정산 1건 취소
        mockMvc.perform(delete("/api/trips/" + TRIP_12 + "/settlements/" + cancelTarget))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/trips/" + TRIP_12 + "/settlements/summary"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activeSettlements", hasSize(2)))
                .andExpect(jsonPath("$.data.cancelledSettlements", hasSize(1)))
                .andExpect(jsonPath("$.data.cancelledSettlements[0].status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.pendingBalances", hasSize(1)))
                .andExpect(jsonPath("$.data.pendingBalances[0].totalAmount").value(25000));
    }

    @Test
    @DisplayName("GET summary: 정산 내역 없으면 모든 목록 비어 있음")
    void getSummary_empty() throws Exception {
        mockMvc.perform(get("/api/trips/" + TRIP_13 + "/settlements/summary"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activeSettlements", hasSize(0)))
                .andExpect(jsonPath("$.data.cancelledSettlements", hasSize(0)))
                .andExpect(jsonPath("$.data.pendingBalances", hasSize(0)));
    }

    // ──────────────────────────────────────────────
    // 유효성 검사
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("type 누락 → 400")
    void create_missingType_returns400() throws Exception {
        Map<String, Object> body = Map.of(
                "payerId", 10, "debtorId", 20, "amount", 10000, "description", "점심값"
                // type 누락
        );

        mockMvc.perform(post("/api/trips/" + TRIP_1 + "/settlements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("amount 0원 → 400")
    void create_zeroAmount_returns400() throws Exception {
        Map<String, Object> body = Map.of(
                "payerId", 10, "debtorId", 20, "amount", 0,
                "description", "점심값", "type", "IMMEDIATE"
        );

        mockMvc.perform(post("/api/trips/" + TRIP_1 + "/settlements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ──────────────────────────────────────────────
    // 정산 완료 처리
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("POST complete 즉시 정산 완료 → 200, status=COMPLETED")
    void completeImmediate_returns200() throws Exception {
        Long settlementId = createSettlementAndGetId(TRIP_14, 10L, 20L, 15000, "점심값", SettlementType.IMMEDIATE);

        mockMvc.perform(post("/api/trips/" + TRIP_14 + "/settlements/" + settlementId + "/complete"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.message").value("정산이 완료 처리되었습니다."));
    }

    @Test
    @DisplayName("POST complete 나중에 정산 완료 → 200, status=COMPLETED")
    void completeDeferred_returns200() throws Exception {
        Long settlementId = createSettlementAndGetId(TRIP_15, 10L, 20L, 25000, "숙박비", SettlementType.DEFERRED);

        mockMvc.perform(post("/api/trips/" + TRIP_15 + "/settlements/" + settlementId + "/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("POST complete 이미 완료된 정산 재완료 → 409")
    void completeAlreadyCompleted_returns409() throws Exception {
        Long settlementId = createSettlementAndGetId(TRIP_16, 10L, 20L, 15000, "점심값", SettlementType.IMMEDIATE);
        mockMvc.perform(post("/api/trips/" + TRIP_16 + "/settlements/" + settlementId + "/complete"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/trips/" + TRIP_16 + "/settlements/" + settlementId + "/complete"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ──────────────────────────────────────────────
    // 헬퍼
    // ──────────────────────────────────────────────

    private void createSettlement(UUID tripId, Long payerId, Long debtorId,
                                   long amount, String description, SettlementType type) {
        createSettlementUseCase.create(CreateSettlementCommand.builder()
                .tripId(tripId).payerId(payerId).debtorId(debtorId)
                .amount(Money.wons(amount)).description(description).type(type)
                .build());
    }

    private Long createSettlementAndGetId(UUID tripId, Long payerId, Long debtorId,
                                           long amount, String description, SettlementType type) {
        return createSettlementUseCase.create(CreateSettlementCommand.builder()
                .tripId(tripId).payerId(payerId).debtorId(debtorId)
                .amount(Money.wons(amount)).description(description).type(type)
                .build()).getId();
    }
}
