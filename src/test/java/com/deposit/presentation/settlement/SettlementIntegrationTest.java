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

    private static final UUID TRIP_1 = UUID.fromString("aa000000-0000-0000-0000-000000000001");
    private static final UUID TRIP_2 = UUID.fromString("aa000000-0000-0000-0000-000000000002");
    private static final UUID TRIP_3 = UUID.fromString("aa000000-0000-0000-0000-000000000003");
    private static final UUID TRIP_4 = UUID.fromString("aa000000-0000-0000-0000-000000000004");
    private static final UUID TRIP_5 = UUID.fromString("aa000000-0000-0000-0000-000000000005");
    private static final UUID TRIP_6 = UUID.fromString("aa000000-0000-0000-0000-000000000006");

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
}
