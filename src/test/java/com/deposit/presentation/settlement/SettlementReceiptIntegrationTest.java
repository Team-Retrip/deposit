package com.deposit.presentation.settlement;

import com.deposit.application.settlement.dto.command.CreateSettlementReceiptCommand;
import com.deposit.application.settlement.port.in.CreateSettlementReceiptUseCase;
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
@DisplayName("정산 영수증 통합 테스트")
class SettlementReceiptIntegrationTest {

    private static final UUID TRIP_1  = UUID.fromString("bb000000-0000-0000-0000-000000000001");
    private static final UUID TRIP_2  = UUID.fromString("bb000000-0000-0000-0000-000000000002");
    private static final UUID TRIP_3  = UUID.fromString("bb000000-0000-0000-0000-000000000003");
    private static final UUID TRIP_4  = UUID.fromString("bb000000-0000-0000-0000-000000000004");
    private static final UUID TRIP_5  = UUID.fromString("bb000000-0000-0000-0000-000000000005");
    private static final UUID TRIP_6  = UUID.fromString("bb000000-0000-0000-0000-000000000006");
    private static final UUID TRIP_7  = UUID.fromString("bb000000-0000-0000-0000-000000000007");
    private static final UUID TRIP_8  = UUID.fromString("bb000000-0000-0000-0000-000000000008");
    private static final UUID TRIP_9  = UUID.fromString("bb000000-0000-0000-0000-000000000009");
    private static final UUID TRIP_10 = UUID.fromString("bb000000-0000-0000-0000-000000000010");

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired CreateSettlementReceiptUseCase createSettlementReceiptUseCase;

    // ──────────────────────────────────────────────
    // 생성
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("POST 영수증 생성 → 201, 인원별 금액 포함")
    void createReceipt_returns201WithItems() throws Exception {
        Map<String, Object> body = Map.of(
                "payerId", 1,
                "totalAmount", 60000,
                "description", "저녁 식사",
                "type", "IMMEDIATE",
                "items", List.of(
                        Map.of("debtorId", 10, "amount", 20000),
                        Map.of("debtorId", 20, "amount", 20000),
                        Map.of("debtorId", 30, "amount", 20000)
                )
        );

        mockMvc.perform(post("/api/trips/" + TRIP_1 + "/settlement-receipts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.totalAmount").value(60000))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.items", hasSize(3)))
                .andExpect(jsonPath("$.data.items[0].debtorId").value(10))
                .andExpect(jsonPath("$.data.items[0].amount").value(20000));
    }

    @Test
    @DisplayName("POST 합계 != 총액 → 400")
    void createReceipt_sumMismatch_returns400() throws Exception {
        Map<String, Object> body = Map.of(
                "payerId", 1,
                "totalAmount", 60000,
                "description", "저녁",
                "type", "IMMEDIATE",
                "items", List.of(
                        Map.of("debtorId", 10, "amount", 20000),
                        Map.of("debtorId", 20, "amount", 15000) // 합계 35000 ≠ 60000
                )
        );

        mockMvc.perform(post("/api/trips/" + TRIP_2 + "/settlement-receipts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST items 없음 → 400")
    void createReceipt_noItems_returns400() throws Exception {
        Map<String, Object> body = Map.of(
                "payerId", 1,
                "totalAmount", 60000,
                "description", "저녁",
                "type", "IMMEDIATE",
                "items", List.of()
        );

        mockMvc.perform(post("/api/trips/" + TRIP_3 + "/settlement-receipts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    // ──────────────────────────────────────────────
    // 조회
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("GET 영수증 상세 조회 → 200, 인원별 금액 포함")
    void getReceipt_returns200WithItems() throws Exception {
        Long receiptId = createReceipt(TRIP_4, 1L, 60000, "저녁",
                List.of(entry(10L, 20000), entry(20L, 20000), entry(30L, 20000)));

        mockMvc.perform(get("/api/trips/" + TRIP_4 + "/settlement-receipts/" + receiptId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(receiptId))
                .andExpect(jsonPath("$.data.totalAmount").value(60000))
                .andExpect(jsonPath("$.data.items", hasSize(3)));
    }

    @Test
    @DisplayName("GET 전체 영수증 목록 조회")
    void getReceipts_returnsAll() throws Exception {
        createReceipt(TRIP_5, 1L, 60000, "저녁", List.of(entry(10L, 30000), entry(20L, 30000)));
        createReceipt(TRIP_5, 1L, 30000, "커피", List.of(entry(10L, 15000), entry(20L, 15000)));

        mockMvc.perform(get("/api/trips/" + TRIP_5 + "/settlement-receipts"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    @DisplayName("GET 존재하지 않는 영수증 → 404")
    void getReceipt_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/trips/" + TRIP_6 + "/settlement-receipts/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ──────────────────────────────────────────────
    // 총액 수정
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("PATCH total 증가 → 비례 배분, 합계 == 새 총액")
    void updateTotal_increase_proportional() throws Exception {
        Long receiptId = createReceipt(TRIP_7, 1L, 60000, "저녁",
                List.of(entry(10L, 20000), entry(20L, 20000), entry(30L, 20000)));

        mockMvc.perform(patch("/api/trips/" + TRIP_7 + "/settlement-receipts/" + receiptId + "/total")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("totalAmount", 90000))))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAmount").value(90000))
                // 비례 배분이므로 합계(items sum) == 90000만 검증
                .andExpect(jsonPath("$.data.items", hasSize(3)))
                .andExpect(result -> {
                    int sum = 0;
                    com.fasterxml.jackson.databind.JsonNode data =
                            new com.fasterxml.jackson.databind.ObjectMapper()
                                    .readTree(result.getResponse().getContentAsString())
                                    .get("data");
                    for (com.fasterxml.jackson.databind.JsonNode item : data.get("items")) {
                        sum += item.get("amount").intValue();
                    }
                    org.assertj.core.api.Assertions.assertThat(sum).isEqualTo(90000);
                });
    }

    @Test
    @DisplayName("PATCH total 취소된 영수증 → 409")
    void updateTotal_cancelled_returns409() throws Exception {
        Long receiptId = createReceipt(TRIP_8, 1L, 60000, "저녁",
                List.of(entry(10L, 30000), entry(20L, 30000)));
        mockMvc.perform(delete("/api/trips/" + TRIP_8 + "/settlement-receipts/" + receiptId))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/trips/" + TRIP_8 + "/settlement-receipts/" + receiptId + "/total")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("totalAmount", 90000))))
                .andExpect(status().isConflict());
    }

    // ──────────────────────────────────────────────
    // 인원별 금액 수정
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("PATCH items 수정 후 합계 == 총액 → 200")
    void updateItems_sumMatches_returns200() throws Exception {
        Long receiptId = createReceipt(TRIP_9, 1L, 60000, "저녁",
                List.of(entry(10L, 20000), entry(20L, 20000), entry(30L, 20000)));

        // A: 30k, B: 10k (C 유지 20k → 합계 60k)
        Map<String, Object> body = Map.of("items", List.of(
                Map.of("debtorId", 10, "amount", 30000),
                Map.of("debtorId", 20, "amount", 10000)
        ));

        mockMvc.perform(patch("/api/trips/" + TRIP_9 + "/settlement-receipts/" + receiptId + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAmount").value(60000));
    }

    @Test
    @DisplayName("PATCH items 수정 후 합계 != 총액 → 400")
    void updateItems_sumMismatch_returns400() throws Exception {
        Long receiptId = createReceipt(TRIP_10, 1L, 60000, "저녁",
                List.of(entry(10L, 20000), entry(20L, 20000), entry(30L, 20000)));

        Map<String, Object> body = Map.of("items", List.of(
                Map.of("debtorId", 10, "amount", 50000) // 합계 90k ≠ 60k
        ));

        mockMvc.perform(patch("/api/trips/" + TRIP_10 + "/settlement-receipts/" + receiptId + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ──────────────────────────────────────────────
    // 취소
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("DELETE 영수증 취소 → 200, status=CANCELLED")
    void cancelReceipt_returns200() throws Exception {
        Long receiptId = createReceipt(TRIP_1, 1L, 30000, "커피",
                List.of(entry(10L, 15000), entry(20L, 15000)));

        mockMvc.perform(delete("/api/trips/" + TRIP_1 + "/settlement-receipts/" + receiptId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("DELETE 이미 취소된 영수증 재취소 → 409")
    void cancelReceipt_alreadyCancelled_returns409() throws Exception {
        Long receiptId = createReceipt(TRIP_2, 1L, 30000, "커피",
                List.of(entry(10L, 15000), entry(20L, 15000)));
        mockMvc.perform(delete("/api/trips/" + TRIP_2 + "/settlement-receipts/" + receiptId))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/trips/" + TRIP_2 + "/settlement-receipts/" + receiptId))
                .andExpect(status().isConflict());
    }

    // ──────────────────────────────────────────────
    // 헬퍼
    // ──────────────────────────────────────────────

    private Long createReceipt(UUID tripId, Long payerId, long total, String description,
                                List<CreateSettlementReceiptCommand.ItemEntry> items) {
        return createSettlementReceiptUseCase.create(
                CreateSettlementReceiptCommand.builder()
                        .tripId(tripId).payerId(payerId)
                        .totalAmount(Money.wons(total)).description(description)
                        .type(SettlementType.IMMEDIATE).items(items)
                        .build()
        ).getId();
    }

    private CreateSettlementReceiptCommand.ItemEntry entry(Long debtorId, long amount) {
        return CreateSettlementReceiptCommand.ItemEntry.builder()
                .debtorId(debtorId).amount(Money.wons(amount)).build();
    }
}
