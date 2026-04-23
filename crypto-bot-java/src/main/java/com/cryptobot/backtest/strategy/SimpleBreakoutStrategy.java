package com.cryptobot.backtest.strategy;

import com.cryptobot.backtest.indicator.AtrIndicator;
import com.cryptobot.marketdata.model.Candle;

import java.util.List;

public class SimpleBreakoutStrategy {
    private static final double DEFAULT_FEE_RATE = 0.0;

    private final int breakoutPeriod;
    private final int atrPeriod;
    private final double atrMultiplier;
    private final int maxHoldingBars;
    private final double feeRate;
    private final AtrIndicator atrIndicator;

    public SimpleBreakoutStrategy(int breakoutPeriod, int atrPeriod, double atrMultiplier, int maxHoldingBars) {
        if (breakoutPeriod <= 0) {
            throw new IllegalArgumentException("breakoutPeriod must be positive");
        }
        if (atrPeriod <= 0) {
            throw new IllegalArgumentException("atrPeriod must be positive");
        }
        if (atrMultiplier <= 0) {
            throw new IllegalArgumentException("atrMultiplier must be positive");
        }
        if (maxHoldingBars <= 0) {
            throw new IllegalArgumentException("maxHoldingBars must be positive");
        }

        this.breakoutPeriod = breakoutPeriod;
        this.atrPeriod = atrPeriod;
        this.atrMultiplier = atrMultiplier;
        this.maxHoldingBars = maxHoldingBars;
        this.feeRate = DEFAULT_FEE_RATE;
        this.atrIndicator = new AtrIndicator();
    }

    public StrategyResult run(List<Candle> candles) {
        if (candles == null || candles.isEmpty()) {
            return new StrategyResult(0.0, 0, 0.0, 0.0, 0.0);
        }

        int minimumCandlesRequired = minimumCandlesRequired();
        if (candles.size() <= minimumCandlesRequired) {
            return new StrategyResult(0.0, 0, 0.0, 0.0, 0.0);
        }

        double[] atr = atrIndicator.calculate(candles, atrPeriod);

        boolean inPosition = false;
        double entryPrice = 0.0;
        double stopPrice = 0.0;
        int barsInTrade = 0;

        double totalPnl = 0.0;
        double grossProfit = 0.0;
        double grossLoss = 0.0;
        int totalTrades = 0;
        int winningTrades = 0;

        double equity = 0.0;
        double peakEquity = 0.0;
        double maxDrawdown = 0.0;

        int startIndex = Math.max(breakoutPeriod, atrPeriod - 1);

        for (int i = startIndex; i < candles.size(); i++) {
            Candle candle = candles.get(i);
            double close = candle.getClose().doubleValue();

            if (!inPosition) {
                double currentAtr = atr[i];
                if (Double.isNaN(currentAtr)) {
                    continue;
                }

                double breakoutHigh = highestHigh(candles, i - breakoutPeriod, i - 1);
                if (close > breakoutHigh) {
                    inPosition = true;
                    entryPrice = close;
                    stopPrice = entryPrice - (currentAtr * atrMultiplier);
                    barsInTrade = 0;
                }
                continue;
            }

            barsInTrade++;

            boolean exitByStop = candle.getLow().doubleValue() <= stopPrice;
            boolean exitByTime = barsInTrade >= maxHoldingBars;

            if (exitByStop || exitByTime) {
                double exitPrice = exitByStop ? stopPrice : close;
                double tradePnl = calculateTradePnl(entryPrice, exitPrice);

                totalPnl += tradePnl;
                totalTrades++;
                if (tradePnl > 0.0) {
                    winningTrades++;
                    grossProfit += tradePnl;
                } else if (tradePnl < 0.0) {
                    grossLoss += Math.abs(tradePnl);
                }
                equity += tradePnl;
                peakEquity = Math.max(peakEquity, equity);
                maxDrawdown = Math.max(maxDrawdown, peakEquity - equity);

                inPosition = false;
            }
        }

        if (inPosition) {
            double finalClose = candles.get(candles.size() - 1).getClose().doubleValue();
            double tradePnl = calculateTradePnl(entryPrice, finalClose);

            totalPnl += tradePnl;
            totalTrades++;

            if (tradePnl > 0.0) {
                winningTrades++;
                grossProfit += tradePnl;
            } else if (tradePnl < 0.0) {
                grossLoss += Math.abs(tradePnl);
            }

            equity += tradePnl;
            peakEquity = Math.max(peakEquity, equity);
            maxDrawdown = Math.max(maxDrawdown, peakEquity - equity);
        }

        double winRate = totalTrades == 0 ? 0.0 : (winningTrades * 100.0) / totalTrades;
        double profitFactor;
        if (grossLoss == 0.0 && grossProfit > 0.0) {
            profitFactor = Double.POSITIVE_INFINITY;
        } else if (grossLoss == 0.0) {
            profitFactor = 0.0;
        } else {
            profitFactor = grossProfit / grossLoss;
        }

        return new StrategyResult(totalPnl, totalTrades, winRate, maxDrawdown, profitFactor);
    }

    public int minimumCandlesRequired() {
        return Math.max(breakoutPeriod, atrPeriod - 1);
    }

    private double highestHigh(List<Candle> candles, int fromInclusive, int toInclusive) {
        double max = Double.NEGATIVE_INFINITY;
        for (int i = fromInclusive; i <= toInclusive; i++) {
            double high = candles.get(i).getHigh().doubleValue();
            max = Math.max(max, high);
        }
        return max;
    }

    private double calculateTradePnl(double entryPrice, double exitPrice) {
        double grossPnl = exitPrice - entryPrice;
        double fees = (entryPrice + exitPrice) * feeRate;
        return grossPnl - fees;
    }

    public record StrategyResult(
            double totalPnl,
            int totalTrades,
            double winRate,
            double maxDrawdown,
            double profitFactor
    ) {
    }
}
