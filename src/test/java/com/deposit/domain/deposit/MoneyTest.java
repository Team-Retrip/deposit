package com.deposit.domain.deposit;

import com.deposit.domain.deposit.vo.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Money VO")
class MoneyTest {

    @Test
    @DisplayName("정상 금액으로 생성")
    void create_success() {
        Money money = Money.wons(50000);
        assertThat(money.getAmount()).isEqualByComparingTo("50000");
    }

    @Test
    @DisplayName("음수 금액으로 생성하면 예외")
    void create_negative_throws() {
        assertThatThrownBy(() -> Money.of(BigDecimal.valueOf(-1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("수수료율로 수수료 금액 계산 (3.5%)")
    void multiply_fee() {
        Money deposit = Money.wons(100000);
        Money fee = deposit.multiply(new BigDecimal("0.035"));
        assertThat(fee.getAmount()).isEqualByComparingTo("3500");
    }

    @Test
    @DisplayName("수수료 공제 후 환불 금액 계산")
    void subtract_fee() {
        Money deposit = Money.wons(100000);
        Money fee = deposit.multiply(new BigDecimal("0.035"));
        Money refunded = deposit.subtract(fee);
        assertThat(refunded.getAmount()).isEqualByComparingTo("96500");
    }

    @Test
    @DisplayName("소수점은 절사(원 단위)")
    void truncate_fraction() {
        Money money = Money.of(new BigDecimal("999.9"));
        assertThat(money.getAmount()).isEqualByComparingTo("999");
    }
}
