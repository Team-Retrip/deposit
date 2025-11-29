package com.retrip.deposit.domain.entity;

import com.retrip.deposit.domain.vo.DepositMembers;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.util.List;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Deposit {
    @Id
    @Column(columnDefinition = "varbinary(16)")
    private UUID id;

    private Long amount;
    @Column(name = "payment_option", length = 50, nullable = false)
    private PaymentOption paymentOption;
    private LocalDateTime depositDate;
    private LocalDateTime withdrawalDate;

    @Embedded
    private DepositMembers depositMembers;

    private Deposit(Long amount, PaymentOption paymentOption, LocalDateTime depositDate, List<UUID> members) {
        this.amount = amount;
        this.paymentOption = paymentOption;
        this.depositDate = depositDate;
        this.depositMembers = new DepositMembers(this, members, amount / members.size());
    }

    //todo: 초기 MVP는 무조건 1/N 정산으로
    public static Deposit create(Long amount, String paymentOption, LocalDateTime depositDate, List<UUID> members) {
        return new Deposit(amount, PaymentOption.codeOf(paymentOption), depositDate, members);
    }

    public boolean isStartTravel() {
        if (depositDate.isBefore(LocalDateTime.now())) {
            return false; //정산 일자 이전
        }
        return this.depositMembers.isDepositPaid();
    }

    public String getRefundFeeMessage() {
        if (depositDate != null && depositDate.plusMonths(1).isAfter(LocalDateTime.now())) {
            return "입금일 기준 30일이 초과되어 보증금 3.5%를 돌려 받을수 없습니다.";
        }
        return null;
    }
}
