package com.cryptobot.marketdata.csv;

import com.cryptobot.marketdata.model.Candle;

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

public class CsvCandleReader {
    private static final int EXPECTED_COLUMN_COUNT = 11;
    private static final DateTimeFormatter FALLBACK_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public List<Candle> read(Path csvPath) throws IOException {
        if (!Files.exists(csvPath)) {
            throw new IllegalArgumentException("CSV file does not exist: " + csvPath);
        }

        List<String> lines = Files.readAllLines(csvPath);
        if (lines.isEmpty()) {
            return List.of();
        }

        List<Candle> candles = new ArrayList<>();

        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line == null || line.isBlank()) {
                continue;
            }

            String[] values = line.split(",", -1);
            if (values.length != EXPECTED_COLUMN_COUNT) {
                throw new IllegalArgumentException(
                        "Invalid CSV format at line " + (i + 1) + ": expected "
                                + EXPECTED_COLUMN_COUNT + " columns but got " + values.length
                );
            }

            candles.add(parseCandle(values, i + 1));
        }

        return candles;
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
