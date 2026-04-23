package com.example.cryptobot.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Candle {
    long openTime;
    double open;
    double high;
    double low;
    double close;
    double volume;
}
