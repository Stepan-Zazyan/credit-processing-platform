package com.example.cryptobot;

import com.example.cryptobot.io.CsvCandleReader;
import com.example.cryptobot.model.Candle;

import java.nio.file.Path;
import java.util.List;

public class Main {

    private static final String DEFAULT_CSV_PATH = "data/BTCUSDT/1m/2024/01/BTC_1m_2024-01.csv";

    public static void main(String[] args) {
        String csvPathArg = (args.length > 0 && args[0] != null && !args[0].isBlank()) ? args[0] : DEFAULT_CSV_PATH;
        Path csvPath = Path.of(csvPathArg);

        CsvCandleReader reader = new CsvCandleReader();

        try {
            List<Candle> candles = reader.read(csvPath);

            Candle first = candles.get(0);
            Candle last = candles.get(candles.size() - 1);

            System.out.println("Loaded candles: " + candles.size());
            System.out.println("Source CSV: " + csvPath.toAbsolutePath());
            System.out.println("First candle: " + first);
            System.out.println("Last candle: " + last);
        } catch (Exception ex) {
            System.err.println("Failed to load candles from CSV: " + csvPath.toAbsolutePath());
            System.err.println("Reason: " + ex.getMessage());
            System.exit(1);
        }
    }
}
