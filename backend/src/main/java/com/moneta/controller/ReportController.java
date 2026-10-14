package com.moneta.controller;

import com.moneta.dto.MonthlyReportResponse;
import com.moneta.dto.YearlyReportResponse;
import com.moneta.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/monthly/{year}/{month}")
    public ResponseEntity<MonthlyReportResponse> getMonthlyReport(@PathVariable("year") int year,
                                                                 @PathVariable("month") int month) {
        MonthlyReportResponse response = reportService.getMonthlyReport(year, month);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/yearly/{year}")
    public ResponseEntity<YearlyReportResponse> getYearlyReport(@PathVariable("year") int year) {
        YearlyReportResponse response = reportService.getYearlyReport(year);
        return ResponseEntity.ok(response);
    }
}
