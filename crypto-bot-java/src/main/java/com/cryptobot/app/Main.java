package com.cryptobot.app;

import com.cryptobot.backtest.strategy.SimpleBreakoutStrategy;
import com.cryptobot.marketdata.csv.CsvCandleReader;
import com.cryptobot.marketdata.model.Candle;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    private static final String DEFAULT_CSV_PATH = "data/BTCUSDT/1m/2024/01/BTC_1m_2024-01.csv";
    private static final int DEFAULT_BREAKOUT_PERIOD = 20;
    private static final int DEFAULT_ATR_PERIOD = 14;
    private static final double DEFAULT_ATR_MULTIPLIER = 2.0;
    private static final int DEFAULT_MAX_HOLDING_BARS = 48;
    private static final int PNL_SCALE = 4;
    private static final int PERCENT_SCALE = 2;

    public static void main(String[] args) {
        Path csvPath = resolveCsvPath(args);
        int breakoutPeriod = resolveInt(args, 1, DEFAULT_BREAKOUT_PERIOD);
        int atrPeriod = resolveInt(args, 2, DEFAULT_ATR_PERIOD);
        double atrMultiplier = resolveDouble(args, 3, DEFAULT_ATR_MULTIPLIER);
        int maxHoldingBars = resolveInt(args, 4, DEFAULT_MAX_HOLDING_BARS);

        CsvCandleReader reader = new CsvCandleReader();
        SimpleBreakoutStrategy strategy = new SimpleBreakoutStrategy(
                breakoutPeriod,
                atrPeriod,
                atrMultiplier,
                maxHoldingBars
        );

        try {
            List<Candle> candles = reader.read(csvPath);

            if (candles.isEmpty()) {
                LOGGER.warning("CSV file was read successfully, but no candle rows were found: " + csvPath.toAbsolutePath());
                return;
            }
            int minimumCandlesRequired = strategy.minimumCandlesRequired();
            if (candles.size() <= minimumCandlesRequired) {
                LOGGER.warning("Not enough candles for strategy run. Required more than "
                        + minimumCandlesRequired + ", got: " + candles.size());
                return;
            }

            SimpleBreakoutStrategy.StrategyResult result = strategy.run(candles);

            System.out.println("CSV: " + csvPath.toAbsolutePath());
            System.out.println("Candles count: " + candles.size());
            System.out.println("Parameters: breakoutPeriod=" + breakoutPeriod
                    + ", atrPeriod=" + atrPeriod
                    + ", atrMultiplier=" + atrMultiplier
                    + ", maxHoldingBars=" + maxHoldingBars);
            System.out.printf("total pnl: %s%n", formatDecimal(result.totalPnl(), PNL_SCALE));
            System.out.println("total trades: " + result.totalTrades());
            System.out.printf("win rate: %s%n", formatPercent(result.winRate()));
            System.out.printf("max drawdown: %s%n", formatDecimal(result.maxDrawdown(), PNL_SCALE));
            System.out.printf("profit factor: %s%n", formatProfitFactor(result.profitFactor()));
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Failed to run strategy for CSV: " + csvPath.toAbsolutePath(), ex);
            System.exit(1);
        }
    }

    private static Path resolveCsvPath(String[] args) {
        if (args != null && args.length > 0 && args[0] != null && !args[0].isBlank()) {
            return Path.of(args[0]);
        }
        return Path.of(DEFAULT_CSV_PATH);
    }

    private static int resolveInt(String[] args, int index, int defaultValue) {
        if (args == null || args.length <= index || args[index] == null || args[index].isBlank()) {
            return defaultValue;
        }
        return Integer.parseInt(args[index]);
    }

    private static double resolveDouble(String[] args, int index, double defaultValue) {
        if (args == null || args.length <= index || args[index] == null || args[index].isBlank()) {
            return defaultValue;
        }
        return Double.parseDouble(args[index]);
    }

    private static String formatProfitFactor(double profitFactor) {
        if (Double.isInfinite(profitFactor)) {
            return "Infinity";
        }
        return formatDecimal(profitFactor, PNL_SCALE);
    }

    private static String formatDecimal(double value, int scale) {
        DecimalFormat decimalFormat = new DecimalFormat();
        decimalFormat.setGroupingUsed(false);
        decimalFormat.setMaximumFractionDigits(scale);
        decimalFormat.setMinimumFractionDigits(scale);
        decimalFormat.setRoundingMode(RoundingMode.HALF_UP);
        return decimalFormat.format(value);
    }

    private static String formatPercent(double value) {
        return formatDecimal(value, PERCENT_SCALE) + "%";
    }
}
