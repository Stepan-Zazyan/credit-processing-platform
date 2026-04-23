package com.cryptobot.backtest;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Value
@Builder
public class Trade {
    LocalDateTime entryTime;
    LocalDateTime exitTime;
    BigDecimal entryPrice;
    BigDecimal exitPrice;
    BigDecimal quantity;
    PositionSide side;
    BigDecimal pnl;
    BigDecimal fee;
}
