package com.cryptobot.backtest;

import com.cryptobot.marketdata.model.Candle;

import java.util.List;

public interface Strategy {
    Signal generateSignal(List<Candle> candles, int index);
}
