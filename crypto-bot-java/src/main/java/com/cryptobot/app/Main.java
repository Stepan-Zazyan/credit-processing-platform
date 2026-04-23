package com.cryptobot.app;

import com.cryptobot.marketdata.csv.CsvCandleReader;
import com.cryptobot.marketdata.model.Candle;

import java.nio.file.Path;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        Path csvPath = Path.of("data", "BTCUSDT", "1m", "2024", "01", "BTC_1m_2024-01.csv");

        CsvCandleReader reader = new CsvCandleReader();

        try {
            List<Candle> candles = reader.read(csvPath);

            System.out.println("Candles count: " + candles.size());

            if (candles.isEmpty()) {
                System.out.println("First candle: n/a");
                System.out.println("Last candle: n/a");
                return;
            }

            System.out.println("First candle: " + candles.getFirst());
            System.out.println("Last candle: " + candles.getLast());
        } catch (Exception ex) {
            System.err.println("Failed to read candles: " + ex.getMessage());
        }
    }
}
