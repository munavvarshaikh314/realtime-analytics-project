package com.analytics.app.service;

import com.analytics.app.model.FinanceMarketTick;
import com.analytics.app.model.FinancePrediction;
import com.analytics.app.repository.FinanceMarketTickRepository;
import com.analytics.app.repository.FinancePredictionRepository;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(value = "analytics.finance.enabled", havingValue = "true")
public class FinanceReportService {

    @Autowired
    private FinanceMarketTickRepository financeMarketTickRepository;

    @Autowired
    private FinancePredictionRepository financePredictionRepository;

    @Value("${analytics.finance.report-max-records:500}")
    private int reportMaxRecords;

    public byte[] exportPdfReport(String symbol, int limit) {
        List<FinancePrediction> predictions = getPredictions(symbol, limit);
        Map<String, Object> insights = buildInsights(symbol, limit);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, outputStream);
            document.open();

            document.add(new Paragraph("Finance Streaming Report", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18)));
            document.add(new Paragraph("Generated at: " + LocalDateTime.now()));
            document.add(new Paragraph("Scope: " + (symbol == null || symbol.isBlank() ? "All tracked assets" : symbol.toUpperCase())));
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Insights", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)));
            document.add(new Paragraph("Latest signal: " + insights.get("latestSignal")));
            document.add(new Paragraph("Average current price: " + insights.get("averageCurrentPrice")));
            document.add(new Paragraph("Average predicted move %: " + insights.get("averagePredictedMovePercentage")));
            document.add(new Paragraph("Max anomaly score: " + insights.get("maxAnomalyScore")));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            addHeaderCell(table, "Symbol");
            addHeaderCell(table, "Current");
            addHeaderCell(table, "Predicted");
            addHeaderCell(table, "Move %");
            addHeaderCell(table, "Signal");
            addHeaderCell(table, "Anomaly");

            for (FinancePrediction prediction : predictions) {
                table.addCell(value(prediction.getSymbol()));
                table.addCell(value(prediction.getCurrentPrice()));
                table.addCell(value(prediction.getPredictedPrice()));
                table.addCell(value(prediction.getPredictedChangePercentage()));
                table.addCell(value(prediction.getTrendSignal()));
                table.addCell(value(prediction.getAnomalyScore()));
            }

            document.add(table);
            document.close();
            return outputStream.toByteArray();
        } catch (IOException | DocumentException e) {
            throw new IllegalStateException("Unable to generate finance PDF report", e);
        }
    }

    public byte[] exportCsvReport(String symbol, int limit) {
        List<FinancePrediction> predictions = getPredictions(symbol, limit);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             CSVPrinter csvPrinter = new CSVPrinter(
                     new OutputStreamWriter(outputStream, StandardCharsets.UTF_8),
                     CSVFormat.DEFAULT.withHeader(
                             "symbol",
                             "currentPrice",
                             "predictedPrice",
                             "predictedChangePercentage",
                             "trendSignal",
                             "anomalyScore",
                             "predictionConfidence",
                             "createdAt"
                     )
             )) {
            for (FinancePrediction prediction : predictions) {
                csvPrinter.printRecord(
                        prediction.getSymbol(),
                        prediction.getCurrentPrice(),
                        prediction.getPredictedPrice(),
                        prediction.getPredictedChangePercentage(),
                        prediction.getTrendSignal(),
                        prediction.getAnomalyScore(),
                        prediction.getPredictionConfidence(),
                        prediction.getCreatedAt()
                );
            }
            csvPrinter.flush();
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to generate finance CSV report", e);
        }
    }

    public Map<String, Object> buildInsights(String symbol, int limit) {
        List<FinancePrediction> predictions = getPredictions(symbol, limit);
        Map<String, Object> insights = new HashMap<>();

        double averageCurrentPrice = predictions.stream()
                .mapToDouble(prediction -> prediction.getCurrentPrice() != null ? prediction.getCurrentPrice() : 0.0)
                .average()
                .orElse(0.0);

        double averagePredictedMove = predictions.stream()
                .mapToDouble(prediction -> prediction.getPredictedChangePercentage() != null ? prediction.getPredictedChangePercentage() : 0.0)
                .average()
                .orElse(0.0);

        double maxAnomalyScore = predictions.stream()
                .mapToDouble(prediction -> prediction.getAnomalyScore() != null ? prediction.getAnomalyScore() : 0.0)
                .max()
                .orElse(0.0);

        String latestSignal = predictions.isEmpty() ? "NO_DATA" : value(predictions.get(0).getTrendSignal());
        insights.put("latestSignal", latestSignal);
        insights.put("averageCurrentPrice", averageCurrentPrice);
        insights.put("averagePredictedMovePercentage", averagePredictedMove);
        insights.put("maxAnomalyScore", maxAnomalyScore);
        insights.put("records", predictions.size());
        insights.put("latestPrediction", predictions.isEmpty() ? null : predictions.get(0));
        insights.put("latestTicks", getTicks(symbol, Math.min(limit, 20)));
        insights.put("generatedAt", LocalDateTime.now());
        return insights;
    }

    public List<FinancePrediction> getPredictions(String symbol, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), reportMaxRecords);
        if (symbol == null || symbol.isBlank()) {
            return financePredictionRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, safeLimit)).getContent();
        }
        return financePredictionRepository.findBySymbolOrderByCreatedAtDesc(symbol.toUpperCase(), PageRequest.of(0, safeLimit)).getContent();
    }

    public List<FinanceMarketTick> getTicks(String symbol, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), reportMaxRecords);
        if (symbol == null || symbol.isBlank()) {
            return financeMarketTickRepository.findAllByOrderByEventTimestampDesc(PageRequest.of(0, safeLimit)).getContent();
        }
        return financeMarketTickRepository.findBySymbolOrderByEventTimestampDesc(symbol.toUpperCase(), PageRequest.of(0, safeLimit)).getContent();
    }

    private void addHeaderCell(PdfPTable table, String label) {
        PdfPCell cell = new PdfPCell(new Phrase(label, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
        table.addCell(cell);
    }

    private String value(Object object) {
        return object == null ? "-" : String.valueOf(object);
    }
}
