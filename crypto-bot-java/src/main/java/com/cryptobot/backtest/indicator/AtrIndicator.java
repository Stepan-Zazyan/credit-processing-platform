package com.cryptobot.backtest.indicator;

import com.cryptobot.marketdata.model.Candle;

import java.util.Arrays;
import java.util.List;

public class AtrIndicator {

    public double[] calculate(List<Candle> candles, int period) {
        if (period <= 0) {
            throw new IllegalArgumentException("ATR period must be positive");
        }
        if (candles == null || candles.isEmpty()) {
            return new double[0];
        }

        int size = candles.size();
        double[] atr = new double[size];
        Arrays.fill(atr, Double.NaN);

        double[] trueRanges = new double[size];
        for (int i = 0; i < size; i++) {
            Candle current = candles.get(i);
            double high = current.getHigh().doubleValue();
            double low = current.getLow().doubleValue();

            if (i == 0) {
                trueRanges[i] = high - low;
                continue;
            }

            double prevClose = candles.get(i - 1).getClose().doubleValue();
            double highLow = high - low;
            double highPrevClose = Math.abs(high - prevClose);
            double lowPrevClose = Math.abs(low - prevClose);
            trueRanges[i] = Math.max(highLow, Math.max(highPrevClose, lowPrevClose));
        }

        if (size < period) {
            return atr;
        }

        double firstAtrSum = 0.0;
        for (int i = 0; i < period; i++) {
            firstAtrSum += trueRanges[i];
        }

        int firstAtrIndex = period - 1;
        atr[firstAtrIndex] = firstAtrSum / period;

        for (int i = period; i < size; i++) {
            atr[i] = ((atr[i - 1] * (period - 1)) + trueRanges[i]) / period;
        }

        return atr;
    }
}
