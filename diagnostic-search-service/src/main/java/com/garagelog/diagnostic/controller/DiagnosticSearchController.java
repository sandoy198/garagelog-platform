package com.garagelog.diagnostic.controller;

import com.garagelog.diagnostic.document.ServiceDiagnosticDocument;
import com.garagelog.diagnostic.service.DiagnosticSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/diagnostics")
@RequiredArgsConstructor
public class DiagnosticSearchController {

    private final DiagnosticSearchService diagnosticSearchService;

    @GetMapping("/search")
    public ResponseEntity<List<ServiceDiagnosticDocument>> searchDiagnostics(
            @RequestParam(value = "q", required = false) String keyword,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "urgentOnly", required = false) Boolean urgentOnly) {
        return ResponseEntity.ok(diagnosticSearchService.searchDiagnostics(keyword, category, urgentOnly));
    }

    @GetMapping("/urgent-alerts")
    public ResponseEntity<List<ServiceDiagnosticDocument>> getUrgentAlerts() {
        return ResponseEntity.ok(diagnosticSearchService.getUrgentAlerts());
    }
}
