package com.example.cryptobot.io;

import com.example.cryptobot.model.Candle;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class CsvCandleReader {

    private static final String EXPECTED_FIRST_HEADER = "open_time";
    private static final int MIN_COLUMNS = 6;

    public List<Candle> read(Path csvPath) throws IOException {
        if (!Files.exists(csvPath)) {
            throw new IOException("CSV file not found: " + csvPath.toAbsolutePath());
        }

        List<Candle> candles = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(csvPath)) {
            String headerLine = reader.readLine();
            validateHeader(headerLine, csvPath);

            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }
                candles.add(parseLine(line, lineNumber, csvPath));
            }
        }

        if (candles.isEmpty()) {
            throw new IOException("CSV contains no candle data rows: " + csvPath.toAbsolutePath());
        }

        return candles;
    }

    private static void validateHeader(String headerLine, Path csvPath) throws IOException {
        if (headerLine == null || headerLine.isBlank()) {
            throw new IOException("CSV header is missing: " + csvPath.toAbsolutePath());
        }

        String[] header = headerLine.split(",");
        if (header.length < MIN_COLUMNS || !EXPECTED_FIRST_HEADER.equalsIgnoreCase(header[0].trim())) {
            throw new IOException("Unexpected CSV header in " + csvPath.toAbsolutePath()
                    + ". Expected first column '" + EXPECTED_FIRST_HEADER + "' and at least " + MIN_COLUMNS + " columns, got: "
                    + headerLine);
        }
    }

    private static Candle parseLine(String line, int lineNumber, Path csvPath) throws IOException {
        String[] parts = line.split(",");
        if (parts.length < MIN_COLUMNS) {
            throw new IOException("Invalid CSV row at line " + lineNumber + " in " + csvPath.toAbsolutePath()
                    + ": expected at least " + MIN_COLUMNS + " columns, got " + parts.length + ". Row: " + line);
        }

        try {
            return Candle.builder()
                    .openTime(Long.parseLong(parts[0].trim()))
                    .open(Double.parseDouble(parts[1].trim()))
                    .high(Double.parseDouble(parts[2].trim()))
                    .low(Double.parseDouble(parts[3].trim()))
                    .close(Double.parseDouble(parts[4].trim()))
                    .volume(Double.parseDouble(parts[5].trim()))
                    .build();
        } catch (NumberFormatException ex) {
            throw new IOException("Failed to parse numeric value at line " + lineNumber + " in "
                    + csvPath.toAbsolutePath() + ". Row: " + line, ex);
        }
    }
}
