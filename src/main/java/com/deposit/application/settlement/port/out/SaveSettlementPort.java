package com.deposit.application.settlement.port.out;

import com.deposit.domain.settlement.Settlement;

public interface SaveSettlementPort {
    Settlement save(Settlement settlement);
}
