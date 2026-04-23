package com.cryptobot.app;

import com.cryptobot.backtest.strategy.SimpleBreakoutStrategy;
import com.cryptobot.backtest.strategy.SimpleBreakoutStrategy.StrategyResult;
import com.cryptobot.marketdata.csv.CsvCandleReader;
import com.cryptobot.marketdata.model.Candle;

import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    private static final String DEFAULT_CSV_PATH = "data/BTCUSDT/1m/2024/01/BTC_1m_2024-01.csv";

    public static void main(String[] args) {
        Path csvPath = resolveCsvPath(args);
        CsvCandleReader reader = new CsvCandleReader();

        int breakoutLookback = 20;
        int atrPeriod = 14;
        double atrMultiplier = 2.0;
        int maxHoldingCandles = 30;

        SimpleBreakoutStrategy strategy = new SimpleBreakoutStrategy(
                breakoutLookback,
                atrPeriod,
                atrMultiplier,
                maxHoldingCandles
        );

        try {
            List<Candle> candles = reader.read(csvPath);

            if (candles.isEmpty()) {
                LOGGER.warning("CSV file was read successfully, but no candle rows were found: " + csvPath.toAbsolutePath());
                return;
            }

            StrategyResult result = strategy.run(candles);

            System.out.println("CSV: " + csvPath.toAbsolutePath());
            System.out.println("Candles count: " + candles.size());
            System.out.println("First candle: " + candles.get(0));
            System.out.println("Last candle: " + candles.get(candles.size() - 1));
            System.out.println("Total PnL: " + format(result.totalPnl()));
            System.out.println("Total trades: " + result.totalTrades());
            System.out.println("Win rate (%): " + format(result.winRate()));
            System.out.println("Max drawdown: " + format(result.maxDrawdown()));
            System.out.println("Profit factor: " + format(result.profitFactor()));
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Failed to run backtest for CSV: " + csvPath.toAbsolutePath(), ex);
            System.exit(1);
        }
    }

    private static Path resolveCsvPath(String[] args) {
        if (args != null && args.length > 0 && args[0] != null && !args[0].isBlank()) {
            return Path.of(args[0]);
        }
        return Path.of(DEFAULT_CSV_PATH);
    }

    private static String format(double value) {
        if (Double.isInfinite(value)) {
            return "INF";
        }
        return String.format("%.4f", value);
    }
}
