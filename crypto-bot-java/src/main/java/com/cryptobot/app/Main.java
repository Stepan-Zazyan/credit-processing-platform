package com.cryptobot.app;

import com.cryptobot.backtest.BacktestEngine;
import com.cryptobot.backtest.BacktestResult;
import com.cryptobot.backtest.Signal;
import com.cryptobot.backtest.Strategy;
import com.cryptobot.marketdata.csv.CsvCandleReader;
import com.cryptobot.marketdata.model.Candle;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;

public class Main {
    private static final Path DEFAULT_CSV_PATH = Path.of("data", "BTCUSDT", "1m", "2024", "01", "BTC_1m_2024-01.csv");

    public static void main(String[] args) {
        Path csvPath = resolveCsvPath(args);
        CsvCandleReader reader = new CsvCandleReader();

        try {
            List<Candle> candles = reader.read(csvPath);

            System.out.println("Source CSV: " + csvPath.toAbsolutePath());
            System.out.println("Candles count: " + candles.size());

            if (candles.isEmpty()) {
                System.out.println("First candle: n/a");
                System.out.println("Last candle: n/a");
                return;
            }

            System.out.println("First candle: " + candles.getFirst());
            System.out.println("Last candle: " + candles.getLast());

            Strategy testStrategy = (allCandles, currentIndex) -> Signal.HOLD;
            BacktestEngine backtestEngine = new BacktestEngine(new BigDecimal("0.001"), BigDecimal.ONE);
            BacktestResult result = backtestEngine.run(candles, testStrategy);

            System.out.println("Backtest metrics:");
            System.out.println("totalPnl=" + result.getTotalPnl());
            System.out.println("totalTrades=" + result.getTotalTrades());
            System.out.println("winRate=" + result.getWinRate());
            System.out.println("maxDrawdown=" + result.getMaxDrawdown());
            System.out.println("profitFactor=" + result.getProfitFactor());
        } catch (Exception ex) {
            System.err.println("Failed to read candles from: " + csvPath.toAbsolutePath());
            System.err.println("Reason: " + ex.getMessage());
            System.exit(1);
        }
    }

    private static Path resolveCsvPath(String[] args) {
        if (args != null && args.length > 0 && args[0] != null && !args[0].isBlank()) {
            return Path.of(args[0]);
        }
        return DEFAULT_CSV_PATH;
    }
}
