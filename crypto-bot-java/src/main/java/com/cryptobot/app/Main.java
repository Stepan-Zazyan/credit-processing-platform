package com.cryptobot.app;

import com.cryptobot.backtest.strategy.SimpleBreakoutStrategy;
import com.cryptobot.backtest.strategy.SimpleBreakoutStrategy.StrategyResult;
import com.cryptobot.marketdata.csv.CsvCandleReader;
import com.cryptobot.marketdata.model.Candle;

import java.nio.file.Path;
import java.util.List;

public class Main {
    private static final String DEFAULT_CSV_PATH = "data/BTCUSDT/1m/2024/01/BTC_1m_2024-01.csv";

    public static void main(String[] args) {
        String csvPathArg = (args.length > 0 && args[0] != null && !args[0].isBlank()) ? args[0] : DEFAULT_CSV_PATH;
        Path csvPath = Path.of(csvPathArg);

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
            StrategyResult result = strategy.run(candles);

            System.out.println("CSV: " + csvPath.toAbsolutePath());
            System.out.println("Candles: " + candles.size());
            System.out.println("Total PnL: " + format(result.totalPnl()));
            System.out.println("Total trades: " + result.totalTrades());
            System.out.println("Win rate (%): " + format(result.winRate()));
            System.out.println("Max drawdown: " + format(result.maxDrawdown()));
            System.out.println("Profit factor: " + format(result.profitFactor()));
        } catch (Exception ex) {
            System.err.println("Failed to run backtest: " + ex.getMessage());
            System.exit(1);
        }
    }

    private static String format(double value) {
        if (Double.isInfinite(value)) {
            return "INF";
        }
        return String.format("%.4f", value);
    }
}
