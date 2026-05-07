package com.deposit.presentation.settlement;

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
@DisplayName("1/N 및 개별 분할 정산 통합 테스트")
class SplitSettlementIntegrationTest {

    private static final UUID TRIP_1 = UUID.fromString("cc000000-0000-0000-0000-000000000001");
    private static final UUID TRIP_2 = UUID.fromString("cc000000-0000-0000-0000-000000000002");
    private static final UUID TRIP_3 = UUID.fromString("cc000000-0000-0000-0000-000000000003");
    private static final UUID TRIP_4 = UUID.fromString("cc000000-0000-0000-0000-000000000004");
    private static final UUID TRIP_5 = UUID.fromString("cc000000-0000-0000-0000-000000000005");
    private static final UUID TRIP_6 = UUID.fromString("cc000000-0000-0000-0000-000000000006");
    private static final UUID TRIP_7 = UUID.fromString("cc000000-0000-0000-0000-000000000007");
    private static final UUID TRIP_8 = UUID.fromString("cc000000-0000-0000-0000-000000000008");

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    // ──────────────────────────────────────────────
    // 1/N 균등 분할 정산
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("POST split 즉시 정산 3명 균등 → 201, 3건 NOTIFIED, 알림 메시지")
    void split_immediate_3persons_even() throws Exception {
        Map<String, Object> body = Map.of(
                "payerId", 1,
                "debtorIds", List.of(10, 20, 30),
                "totalAmount", 60000,
                "description", "저녁 식사",
                "type", "IMMEDIATE"
        );

        mockMvc.perform(post("/api/trips/" + TRIP_1 + "/settlements/split")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.totalDebtors").value(3))
                .andExpect(jsonPath("$.data.baseAmount").value(20000))
                .andExpect(jsonPath("$.data.remainderAmount").doesNotExist())
                .andExpect(jsonPath("$.data.settlements", hasSize(3)))
                .andExpect(jsonPath("$.data.settlements[0].status").value("NOTIFIED"))
                .andExpect(jsonPath("$.data.settlements[0].amount").value(20000))
                .andExpect(jsonPath("$.message").value(containsString("3명")));
    }

    @Test
    @DisplayName("POST split 나머지 발생 → 첫 번째 채무자에게 나머지 추가, 합계 == 총액")
    void split_immediate_with_remainder() throws Exception {
        // 10001 / 3 = 3333 base, remainder = 2 → A=3335, B=3333, C=3333
        Map<String, Object> body = Map.of(
                "payerId", 1,
                "debtorIds", List.of(10, 20, 30),
                "totalAmount", 10001,
                "description", "커피",
                "type", "IMMEDIATE"
        );

        mockMvc.perform(post("/api/trips/" + TRIP_2 + "/settlements/split")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.baseAmount").value(3333))
                .andExpect(jsonPath("$.data.remainderAmount").value(2))
                // 첫 번째 채무자: 3333 + 2 = 3335
                .andExpect(jsonPath("$.data.settlements[0].amount").value(3335))
                .andExpect(jsonPath("$.data.settlements[1].amount").value(3333))
                .andExpect(jsonPath("$.data.settlements[2].amount").value(3333));
    }

    @Test
    @DisplayName("POST split 나중에 정산 → 201, ACCUMULATED, 채무자별 잔액 누적")
    void split_deferred_2persons() throws Exception {
        Map<String, Object> body = Map.of(
                "payerId", 1,
                "debtorIds", List.of(10, 20),
                "totalAmount", 40000,
                "description", "교통비",
                "type", "DEFERRED"
        );

        mockMvc.perform(post("/api/trips/" + TRIP_3 + "/settlements/split")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.settlements", hasSize(2)))
                .andExpect(jsonPath("$.data.settlements[0].status").value("ACCUMULATED"));

        // 각 채무자 잔액 20000
        mockMvc.perform(get("/api/trips/" + TRIP_3 + "/settlements/balances"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].totalAmount").value(20000))
                .andExpect(jsonPath("$.data[1].totalAmount").value(20000));
    }

    @Test
    @DisplayName("POST split debtorIds 없음 → 400")
    void split_emptyDebtors_returns400() throws Exception {
        Map<String, Object> body = Map.of(
                "payerId", 1,
                "debtorIds", List.of(),
                "totalAmount", 60000,
                "description", "저녁",
                "type", "IMMEDIATE"
        );

        mockMvc.perform(post("/api/trips/" + TRIP_4 + "/settlements/split")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    // ──────────────────────────────────────────────
    // 개별 금액 분할 정산
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("POST split/custom 즉시 정산 합계 == 총액 → 201, 개별 금액으로 알림")
    void customSplit_immediate_validSum() throws Exception {
        Map<String, Object> body = Map.of(
                "payerId", 1,
                "totalAmount", 60000,
                "description", "저녁 식사",
                "type", "IMMEDIATE",
                "debtorAmounts", List.of(
                        Map.of("debtorId", 10, "amount", 30000),
                        Map.of("debtorId", 20, "amount", 20000),
                        Map.of("debtorId", 30, "amount", 10000)
                )
        );

        mockMvc.perform(post("/api/trips/" + TRIP_5 + "/settlements/split/custom")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.settlements", hasSize(3)))
                .andExpect(jsonPath("$.data.settlements[0].amount").value(30000))
                .andExpect(jsonPath("$.data.settlements[1].amount").value(20000))
                .andExpect(jsonPath("$.data.settlements[2].amount").value(10000))
                .andExpect(jsonPath("$.data.settlements[0].status").value("NOTIFIED"))
                .andExpect(jsonPath("$.message").value(containsString("3명")));
    }

    @Test
    @DisplayName("POST split/custom 합계 != 총액 → 400")
    void customSplit_sumMismatch_returns400() throws Exception {
        Map<String, Object> body = Map.of(
                "payerId", 1,
                "totalAmount", 60000,
                "description", "저녁",
                "type", "IMMEDIATE",
                "debtorAmounts", List.of(
                        Map.of("debtorId", 10, "amount", 30000),
                        Map.of("debtorId", 20, "amount", 20000) // 합계 50000 ≠ 60000
                )
        );

        mockMvc.perform(post("/api/trips/" + TRIP_6 + "/settlements/split/custom")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST split/custom 나중에 정산 → 채무자별 개별 금액 잔액 누적")
    void customSplit_deferred_accumulatesCustomAmounts() throws Exception {
        Map<String, Object> body = Map.of(
                "payerId", 1,
                "totalAmount", 60000,
                "description", "숙박비",
                "type", "DEFERRED",
                "debtorAmounts", List.of(
                        Map.of("debtorId", 10, "amount", 40000),
                        Map.of("debtorId", 20, "amount", 20000)
                )
        );

        mockMvc.perform(post("/api/trips/" + TRIP_7 + "/settlements/split/custom")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/trips/" + TRIP_7 + "/settlements/balances/debtor/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].totalAmount").value(40000));

        mockMvc.perform(get("/api/trips/" + TRIP_7 + "/settlements/balances/debtor/20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].totalAmount").value(20000));
    }

    @Test
    @DisplayName("POST split/custom debtorAmounts 없음 → 400")
    void customSplit_emptyDebtorAmounts_returns400() throws Exception {
        Map<String, Object> body = Map.of(
                "payerId", 1,
                "totalAmount", 60000,
                "description", "저녁",
                "type", "IMMEDIATE",
                "debtorAmounts", List.of()
        );

        mockMvc.perform(post("/api/trips/" + TRIP_8 + "/settlements/split/custom")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }
}
