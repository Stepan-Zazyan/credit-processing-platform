package com.cryptobot.marketdata.csv;

import com.cryptobot.marketdata.model.Candle;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

public class CsvCandleReader {
    private static final Logger LOGGER = Logger.getLogger(CsvCandleReader.class.getName());

    private static final int EXPECTED_COLUMN_COUNT = 11;
    private static final DateTimeFormatter FALLBACK_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String[] EXPECTED_HEADER = {
            "timestamp", "dateTimeUtc", "open", "high", "low", "close",
            "volume", "quoteVolume", "trades", "buyVolume", "sellVolume"
    };

    public List<Candle> read(Path csvPath) throws IOException {
        if (!Files.exists(csvPath)) {
            String message = "CSV file does not exist: " + csvPath.toAbsolutePath();
            LOGGER.severe(message);
            throw new IllegalArgumentException(message);
        }

        List<Candle> candles = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(csvPath)) {
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                LOGGER.warning("CSV file is empty: " + csvPath.toAbsolutePath());
                return List.of();
            }

            validateHeader(headerLine, csvPath);

            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }

                String[] values = line.split(",", -1);
                if (values.length != EXPECTED_COLUMN_COUNT) {
                    throw new IllegalArgumentException(
                            "Invalid CSV format at line " + lineNumber + ": expected "
                                    + EXPECTED_COLUMN_COUNT + " columns but got " + values.length
                    );
                }

                candles.add(parseCandle(values, lineNumber));
            }
        } catch (IOException | RuntimeException ex) {
            LOGGER.severe("Failed to read CSV file " + csvPath.toAbsolutePath() + ": " + ex.getMessage());
            throw ex;
        }

        return candles;
    }

    private void validateHeader(String headerLine, Path csvPath) {
        String[] headerValues = headerLine.split(",", -1);
        if (headerValues.length != EXPECTED_COLUMN_COUNT) {
            throw new IllegalArgumentException(
                    "Invalid CSV header in " + csvPath.toAbsolutePath() + ": expected "
                            + EXPECTED_COLUMN_COUNT + " columns but got " + headerValues.length
            );
        }

        for (int i = 0; i < EXPECTED_HEADER.length; i++) {
            String actual = normalizeHeader(headerValues[i]);
            String expected = normalizeHeader(EXPECTED_HEADER[i]);
            if (!expected.equals(actual)) {
                throw new IllegalArgumentException(
                        "Invalid CSV header in " + csvPath.toAbsolutePath() + ": column " + (i + 1)
                                + " must be '" + EXPECTED_HEADER[i] + "' but was '" + headerValues[i].trim() + "'"
                );
            }
        }
    }

    private String normalizeHeader(String value) {
        return value == null
                ? ""
                : value.trim().replaceAll("[^a-zA-Z0-9]", "").toLowerCase(Locale.ROOT);
    }

    private Candle parseCandle(String[] values, int lineNumber) {
        try {
            return Candle.builder()
                    .timestamp(Long.parseLong(values[0].trim()))
                    .dateTimeUtc(parseDateTimeUtc(values[1].trim()))
                    .open(new BigDecimal(values[2].trim()))
                    .high(new BigDecimal(values[3].trim()))
                    .low(new BigDecimal(values[4].trim()))
                    .close(new BigDecimal(values[5].trim()))
                    .volume(new BigDecimal(values[6].trim()))
                    .quoteVolume(new BigDecimal(values[7].trim()))
                    .trades(Integer.parseInt(values[8].trim()))
                    .buyVolume(new BigDecimal(values[9].trim()))
                    .sellVolume(new BigDecimal(values[10].trim()))
                    .build();
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Failed to parse CSV line " + lineNumber + ": " + ex.getMessage(), ex);
        }
    }

    private LocalDateTime parseDateTimeUtc(String rawValue) {
        try {
            return Instant.parse(rawValue).atOffset(ZoneOffset.UTC).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            // Try common CSV datetime format without timezone suffix.
        }

        try {
            return LocalDateTime.parse(rawValue, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException ignored) {
            // Try fallback format.
        }

        return LocalDateTime.parse(rawValue, FALLBACK_DATE_TIME_FORMATTER);
    }
}
