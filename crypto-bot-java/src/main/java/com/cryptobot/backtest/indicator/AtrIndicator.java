package com.cryptobot.backtest.indicator;

import com.cryptobot.marketdata.model.Candle;

import java.util.List;

public class AtrIndicator {

    public double[] calculate(List<Candle> candles, int period) {
        if (period <= 0) {
            throw new IllegalArgumentException("ATR period must be positive");
        }
        if (candles == null || candles.isEmpty()) {
            return new double[0];
        }

        double[] atr = new double[candles.size()];
        double[] tr = new double[candles.size()];

        for (int i = 0; i < candles.size(); i++) {
            Candle current = candles.get(i);
            double high = current.getHigh().doubleValue();
            double low = current.getLow().doubleValue();

            if (i == 0) {
                tr[i] = high - low;
                atr[i] = tr[i];
                continue;
            }

            double prevClose = candles.get(i - 1).getClose().doubleValue();
            double range = high - low;
            double gapUp = Math.abs(high - prevClose);
            double gapDown = Math.abs(low - prevClose);

            tr[i] = Math.max(range, Math.max(gapUp, gapDown));

            if (i < period) {
                double sum = 0.0;
                for (int j = 0; j <= i; j++) {
                    sum += tr[j];
                }
                atr[i] = sum / (i + 1);
            } else {
                atr[i] = ((atr[i - 1] * (period - 1)) + tr[i]) / period;
            }
        }

        return atr;
    }
}
