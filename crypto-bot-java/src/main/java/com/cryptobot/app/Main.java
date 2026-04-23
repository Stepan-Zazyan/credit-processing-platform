package com.cryptobot.app;

import com.cryptobot.backtest.BacktestEngine;
import com.cryptobot.backtest.BacktestResult;
import com.cryptobot.backtest.Signal;
import com.cryptobot.backtest.Strategy;
import com.cryptobot.marketdata.csv.CsvCandleReader;
import com.cryptobot.marketdata.model.Candle;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

        BigDecimal feeRate = new BigDecimal("0.001");
        BigDecimal quantity = BigDecimal.ONE;

        BacktestEngine engine = new BacktestEngine(feeRate, quantity);
        Strategy strategy = createSimpleStubStrategy();

        try {
            List<Candle> candles = reader.read(csvPath);

            if (candles.isEmpty()) {
                LOGGER.warning("CSV file was read successfully, but no candle rows were found: " + csvPath.toAbsolutePath());
                return;
            }

            BacktestResult result = engine.run(candles, strategy);

            System.out.println("CSV: " + csvPath.toAbsolutePath());
            System.out.println("Candles count: " + candles.size());
            System.out.println("Total PnL: " + format(result.getTotalPnl()));
            System.out.println("Total trades: " + result.getTotalTrades());
            System.out.println("Win rate (%): " + format(result.getWinRate().multiply(new BigDecimal("100"))));
            System.out.println("Max drawdown: " + format(result.getMaxDrawdown()));
            System.out.println("Profit factor: " + format(result.getProfitFactor()));
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Failed to run backtest for CSV: " + csvPath.toAbsolutePath(), ex);
            System.exit(1);
        }
    }

    private static Strategy createSimpleStubStrategy() {
        return (candles, index) -> {
            if (index == 0) {
                return Signal.HOLD;
            }

            BigDecimal previousClose = candles.get(index - 1).getClose();
            BigDecimal currentClose = candles.get(index).getClose();
            int comparison = currentClose.compareTo(previousClose);

            if (comparison > 0) {
                return Signal.BUY;
            }
            if (comparison < 0) {
                return Signal.SELL;
            }
            return Signal.HOLD;
        };
    }

    private static Path resolveCsvPath(String[] args) {
        if (args != null && args.length > 0 && args[0] != null && !args[0].isBlank()) {
            return Path.of(args[0]);
        }
        return Path.of(DEFAULT_CSV_PATH);
    }

    private static String format(BigDecimal value) {
        return value.setScale(4, RoundingMode.HALF_UP).toPlainString();
    }
}
