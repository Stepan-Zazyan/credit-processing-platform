package com.cryptobot.backtest;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;

@Value
@Builder
public class BacktestResult {
    BigDecimal totalPnl;
    int totalTrades;
    BigDecimal winRate;
    BigDecimal maxDrawdown;
    BigDecimal profitFactor;
    List<Trade> trades;
}
