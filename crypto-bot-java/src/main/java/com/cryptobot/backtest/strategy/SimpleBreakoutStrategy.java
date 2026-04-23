package com.cryptobot.backtest.strategy;

import com.cryptobot.backtest.indicator.AtrIndicator;
import com.cryptobot.marketdata.model.Candle;

import java.util.List;

public class SimpleBreakoutStrategy {

    private final int breakoutLookback;
    private final int atrPeriod;
    private final double atrMultiplier;
    private final int maxHoldingCandles;
    private final AtrIndicator atrIndicator;

    public SimpleBreakoutStrategy(int breakoutLookback, int atrPeriod, double atrMultiplier, int maxHoldingCandles) {
        if (breakoutLookback <= 0) {
            throw new IllegalArgumentException("breakoutLookback must be positive");
        }
        if (atrPeriod <= 0) {
            throw new IllegalArgumentException("atrPeriod must be positive");
        }
        if (atrMultiplier <= 0) {
            throw new IllegalArgumentException("atrMultiplier must be positive");
        }
        if (maxHoldingCandles <= 0) {
            throw new IllegalArgumentException("maxHoldingCandles must be positive");
        }

        this.breakoutLookback = breakoutLookback;
        this.atrPeriod = atrPeriod;
        this.atrMultiplier = atrMultiplier;
        this.maxHoldingCandles = maxHoldingCandles;
        this.atrIndicator = new AtrIndicator();
    }

    public StrategyResult run(List<Candle> candles) {
        if (candles == null || candles.isEmpty()) {
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

        int startIndex = Math.max(breakoutLookback, atrPeriod);

        for (int i = startIndex; i < candles.size(); i++) {
            Candle candle = candles.get(i);
            double close = candle.getClose().doubleValue();

            if (!inPosition) {
                double breakoutHigh = highestHigh(candles, i - breakoutLookback, i - 1);
                if (close > breakoutHigh) {
                    inPosition = true;
                    entryPrice = close;
                    stopPrice = entryPrice - atr[i] * atrMultiplier;
                    barsInTrade = 0;
                }
                continue;
            }

            barsInTrade++;

            double low = candle.getLow().doubleValue();
            double closePrice = candle.getClose().doubleValue();
            double reverseLevel = lowestLow(candles, i - breakoutLookback, i - 1);

            boolean exitByStop = low <= stopPrice;
            boolean exitByReverse = closePrice < reverseLevel;
            boolean exitByTime = barsInTrade >= maxHoldingCandles;

            if (exitByStop || exitByReverse || exitByTime) {
                double exitPrice = exitByStop ? stopPrice : closePrice;
                double tradePnl = exitPrice - entryPrice;

                totalPnl += tradePnl;
                totalTrades++;

                if (tradePnl > 0) {
                    winningTrades++;
                    grossProfit += tradePnl;
                } else {
                    grossLoss += Math.abs(tradePnl);
                }

                equity += tradePnl;
                if (equity > peakEquity) {
                    peakEquity = equity;
                }
                double drawdown = peakEquity - equity;
                if (drawdown > maxDrawdown) {
                    maxDrawdown = drawdown;
                }

                inPosition = false;
                continue;
            }

            double candidateStop = closePrice - atr[i] * atrMultiplier;
            if (candidateStop > stopPrice) {
                stopPrice = candidateStop;
            }
        }

        if (inPosition) {
            double finalClose = candles.get(candles.size() - 1).getClose().doubleValue();
            double tradePnl = finalClose - entryPrice;

            totalPnl += tradePnl;
            totalTrades++;

            if (tradePnl > 0) {
                winningTrades++;
                grossProfit += tradePnl;
            } else {
                grossLoss += Math.abs(tradePnl);
            }

            equity += tradePnl;
            if (equity > peakEquity) {
                peakEquity = equity;
            }
            double drawdown = peakEquity - equity;
            if (drawdown > maxDrawdown) {
                maxDrawdown = drawdown;
            }
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

    private double highestHigh(List<Candle> candles, int fromInclusive, int toInclusive) {
        double max = Double.NEGATIVE_INFINITY;
        for (int i = fromInclusive; i <= toInclusive; i++) {
            double high = candles.get(i).getHigh().doubleValue();
            if (high > max) {
                max = high;
            }
        }
        return max;
    }

    private double lowestLow(List<Candle> candles, int fromInclusive, int toInclusive) {
        double min = Double.POSITIVE_INFINITY;
        for (int i = fromInclusive; i <= toInclusive; i++) {
            double low = candles.get(i).getLow().doubleValue();
            if (low < min) {
                min = low;
            }
        }
        return min;
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
