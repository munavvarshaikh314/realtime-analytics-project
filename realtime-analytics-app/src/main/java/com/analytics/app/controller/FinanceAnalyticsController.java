package com.analytics.app.controller;

import com.analytics.app.service.FinanceAdminSecurityService;
import com.analytics.app.service.FinanceMLService;
import com.analytics.app.service.FinanceReportService;
import com.analytics.app.service.FinanceStreamProcessingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/finance")
@CrossOrigin(origins = "*")
@ConditionalOnProperty(value = "analytics.finance.enabled", havingValue = "true")
public class FinanceAnalyticsController {

    @Autowired
    private FinanceStreamProcessingService financeStreamProcessingService;

    @Autowired
    private FinanceReportService financeReportService;

    @Autowired
    private FinanceMLService financeMLService;

    @Autowired
    private FinanceAdminSecurityService financeAdminSecurityService;

    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getFinanceOverview() {
        return ResponseEntity.ok(financeStreamProcessingService.getFinanceOverview());
    }

    @GetMapping("/latest")
    public ResponseEntity<?> getLatestPredictions(@RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(financeStreamProcessingService.getLatestPredictions(limit));
    }

    @GetMapping("/history/{symbol}")
    public ResponseEntity<?> getHistory(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return ResponseEntity.ok(financeStreamProcessingService.getHistory(symbol, limit));
    }

    @GetMapping("/predictions/{symbol}")
    public ResponseEntity<?> getPredictions(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return ResponseEntity.ok(financeStreamProcessingService.getPredictionsForSymbol(symbol, limit));
    }

    @GetMapping("/insights")
    public ResponseEntity<Map<String, Object>> getInsights(
            @RequestParam(required = false) String symbol,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return ResponseEntity.ok(financeReportService.buildInsights(symbol, limit));
    }

    @PostMapping("/retrain")
    public ResponseEntity<Map<String, Object>> retrainModels(
            @RequestHeader(value = "X-Analytics-Admin-Key", required = false) String adminKey,
            @RequestParam(required = false) String assetId
    ) {
        financeAdminSecurityService.validateAdminKey(adminKey);
        return ResponseEntity.accepted().body(financeMLService.retrainAsync(assetId));
    }

    @PostMapping(path = "/import/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> importCsv(
            @RequestHeader(value = "X-Analytics-Admin-Key", required = false) String adminKey,
            @RequestPart("file") MultipartFile file
    ) throws Exception {
        financeAdminSecurityService.validateAdminKey(adminKey);
        int importedCount = financeStreamProcessingService.importTicksFromCsv(file.getInputStream());
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Finance CSV imported successfully",
                "importedCount", importedCount
        ));
    }

    @GetMapping(value = "/reports/export.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportPdfReport(
            @RequestHeader(value = "X-Analytics-Admin-Key", required = false) String adminKey,
            @RequestParam(required = false) String symbol,
            @RequestParam(defaultValue = "50") int limit
    ) {
        financeAdminSecurityService.validateAdminKey(adminKey);
        byte[] pdf = financeReportService.exportPdfReport(symbol, limit);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("finance-report.pdf").build().toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping(value = "/reports/export.csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportCsvReport(
            @RequestHeader(value = "X-Analytics-Admin-Key", required = false) String adminKey,
            @RequestParam(required = false) String symbol,
            @RequestParam(defaultValue = "50") int limit
    ) {
        financeAdminSecurityService.validateAdminKey(adminKey);
        byte[] csv = financeReportService.exportCsvReport(symbol, limit);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("finance-report.csv").build().toString())
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
