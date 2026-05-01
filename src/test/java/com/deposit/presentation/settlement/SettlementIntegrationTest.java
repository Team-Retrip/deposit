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
