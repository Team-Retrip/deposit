package com.retrip.deposit.domain.vo;

import com.retrip.deposit.domain.entity.Deposit;
import com.retrip.deposit.domain.entity.DepositMember;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.OneToMany;

import java.util.ArrayList;
import java.util.List;

import java.util.UUID;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@Embeddable
public class DepositMembers {
    @OneToMany(mappedBy = "deposit", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DepositMember> values = new ArrayList<>();

    public DepositMembers(Deposit deposit, List<UUID> members, long amount) { // 1/N 정산
        values.addAll( members.stream().map(member -> new DepositMember(deposit, member,amount)).toList());
    }

    public boolean isDepositPaid() {
        return values.stream().filter(dm -> dm.isDepositPaid()).count() == values.size();
    }
}
