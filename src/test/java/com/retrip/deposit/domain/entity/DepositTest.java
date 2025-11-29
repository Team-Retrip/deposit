package com.retrip.deposit.domain.entity;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

class DepositTest {
    @Test
    @DisplayName("보증금 생성 테스트")
    public void createDepositTest() {
        //GiVEN


        //WHEN
        Deposit deposit = Deposit.create(200_000L, "CARD", LocalDateTime.now().plusDays(3), List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()));

        //THEN
        Assertions.assertNotNull(deposit);
        Assertions.assertNotNull(deposit.getDepositMembers());
        Assertions.assertEquals(3, deposit.getDepositMembers().getValues().size());
    }

    @Test
    @DisplayName("보증금 환불 안내 문구 테스트")
    public void getRefundFeeMessageTest() {
        //GiVEN
        Deposit deposit = Deposit.create(200_000L, "CARD", LocalDateTime.now().minusMinutes(2), List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()));


        //WHEN
        String message = deposit.getRefundFeeMessage();


        //THEN
        Assertions.assertEquals("입금일 기준 30일이 초과되어 보증금 3.5%를 돌려 받을수 없습니다.", message);
    }

    @Test
    @DisplayName("보증금 일자 이전이라 여행 시작이 불가능")
    public void canNotStartTravelByBeforeDepositDate() {
        //GiVEN
        Deposit deposit = Deposit.create(200_000L, "CARD", LocalDateTime.now().plusDays(1), List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()));


        //WHEN
        boolean isStartTravel = deposit.isStartTravel();


        //THEN
        Assertions.assertFalse(isStartTravel);
    }

    @Test
    @DisplayName("모두 보증금을 납부하지 않아서, 여행 시작 불가능")
    public void canNotStartTravelByNotDepositPaid() {
        //GiVEN
        Deposit deposit = Deposit.create(200_000L, "CARD", LocalDateTime.now().minusDays(1), List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()));


        //WHEN
        boolean isStartTravel = deposit.isStartTravel();


        //THEN
        Assertions.assertFalse(isStartTravel);
    }

    @Test
    @DisplayName("보증금을 모두 납부해서, 여행 가능")
    public void canStartTravel() {
        //GiVEN
        //보증금이 0원이라 모두 보증한 상태
        Deposit deposit = Deposit.create(0L, "CARD", LocalDateTime.now().minusDays(1), List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()));


        //WHEN
        boolean isStartTravel = deposit.isStartTravel();


        //THEN
        Assertions.assertFalse(isStartTravel);
    }

}
