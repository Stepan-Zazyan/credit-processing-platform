package com.cryptobot.backtest;

import com.cryptobot.marketdata.model.Candle;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BacktestEngine {
    private static final int SCALE = 8;

    private final BigDecimal feeRate;
    private final BigDecimal quantity;

    public BacktestEngine(BigDecimal feeRate, BigDecimal quantity) {
        this.feeRate = feeRate;
        this.quantity = quantity;
    }

    public BacktestResult run(List<Candle> candles, Strategy strategy) {
        if (candles == null || candles.isEmpty()) {
            return BacktestResult.builder()
                    .totalPnl(BigDecimal.ZERO)
                    .totalTrades(0)
                    .winRate(BigDecimal.ZERO)
                    .maxDrawdown(BigDecimal.ZERO)
                    .profitFactor(BigDecimal.ZERO)
                    .trades(List.of())
                    .build();
        }

        List<Trade> trades = new ArrayList<>();

        PositionSide side = PositionSide.FLAT;
        LocalDateTime entryTime = null;
        BigDecimal entryPrice = null;

        for (int i = 0; i < candles.size(); i++) {
            Signal signal = strategy.generateSignal(candles, i);
            Candle candle = candles.get(i);

            if (side == PositionSide.FLAT && signal == Signal.BUY) {
                side = PositionSide.LONG;
                entryTime = candle.getDateTimeUtc();
                entryPrice = candle.getClose();
                continue;
            }

            if (side == PositionSide.LONG && signal == Signal.SELL) {
                Trade trade = closeLong(entryTime, candle.getDateTimeUtc(), entryPrice, candle.getClose());
                trades.add(trade);
                side = PositionSide.FLAT;
                entryTime = null;
                entryPrice = null;
            }
        }

        if (side == PositionSide.LONG) {
            Candle last = candles.getLast();
            trades.add(closeLong(entryTime, last.getDateTimeUtc(), entryPrice, last.getClose()));
        }

        return buildResult(trades);
    }

    private Trade closeLong(LocalDateTime entryTime, LocalDateTime exitTime, BigDecimal entryPrice, BigDecimal exitPrice) {
        BigDecimal entryFee = entryPrice.multiply(quantity).multiply(feeRate);
        BigDecimal exitFee = exitPrice.multiply(quantity).multiply(feeRate);
        BigDecimal totalFee = entryFee.add(exitFee);

        BigDecimal grossPnl = exitPrice.subtract(entryPrice).multiply(quantity);
        BigDecimal netPnl = grossPnl.subtract(totalFee).setScale(SCALE, RoundingMode.HALF_UP);

        return Trade.builder()
                .entryTime(entryTime)
                .exitTime(exitTime)
                .entryPrice(entryPrice)
                .exitPrice(exitPrice)
                .quantity(quantity)
                .side(PositionSide.LONG)
                .pnl(netPnl)
                .fee(totalFee.setScale(SCALE, RoundingMode.HALF_UP))
                .build();
    }

    private BacktestResult buildResult(List<Trade> trades) {
        BigDecimal totalPnl = BigDecimal.ZERO;
        BigDecimal grossProfit = BigDecimal.ZERO;
        BigDecimal grossLoss = BigDecimal.ZERO;

        int wins = 0;
        BigDecimal equity = BigDecimal.ZERO;
        BigDecimal peak = BigDecimal.ZERO;
        BigDecimal maxDrawdown = BigDecimal.ZERO;

        for (Trade trade : trades) {
            BigDecimal pnl = trade.getPnl();
            totalPnl = totalPnl.add(pnl);

            if (pnl.compareTo(BigDecimal.ZERO) > 0) {
                wins++;
                grossProfit = grossProfit.add(pnl);
            } else if (pnl.compareTo(BigDecimal.ZERO) < 0) {
                grossLoss = grossLoss.add(pnl.abs());
            }

            equity = equity.add(pnl);
            if (equity.compareTo(peak) > 0) {
                peak = equity;
            }

            BigDecimal drawdown = peak.subtract(equity);
            if (drawdown.compareTo(maxDrawdown) > 0) {
                maxDrawdown = drawdown;
            }
        }

        int totalTrades = trades.size();
        BigDecimal winRate = totalTrades == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(wins)
                .divide(BigDecimal.valueOf(totalTrades), SCALE, RoundingMode.HALF_UP);

        BigDecimal profitFactor = grossLoss.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : grossProfit.divide(grossLoss, SCALE, RoundingMode.HALF_UP);

        return BacktestResult.builder()
                .totalPnl(totalPnl.setScale(SCALE, RoundingMode.HALF_UP))
                .totalTrades(totalTrades)
                .winRate(winRate)
                .maxDrawdown(maxDrawdown.setScale(SCALE, RoundingMode.HALF_UP))
                .profitFactor(profitFactor)
                .trades(List.copyOf(trades))
                .build();
    }
}
