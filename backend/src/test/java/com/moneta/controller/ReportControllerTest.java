package com.moneta.controller;

import com.moneta.dto.MonthlyReportResponse;
import com.moneta.dto.YearlyReportResponse;
import com.moneta.service.ReportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportControllerTest {

    @Mock
    private ReportService reportService;

    @InjectMocks
    private ReportController reportController;

    @Test
    void getMonthlyReport() {
        when(reportService.getMonthlyReport(2024, 1))
                .thenReturn(new MonthlyReportResponse(1, 2024, Collections.emptyMap(), Collections.emptyMap(), BigDecimal.ZERO));

        ResponseEntity<MonthlyReportResponse> res = reportController.getMonthlyReport(2024, 1);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(1, res.getBody().getMonth());
    }

    @Test
    void getYearlyReport() {
        when(reportService.getYearlyReport(2024))
                .thenReturn(new YearlyReportResponse(2024, Collections.emptyMap(), Collections.emptyMap(), BigDecimal.ZERO));

        ResponseEntity<YearlyReportResponse> res = reportController.getYearlyReport(2024);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(2024, res.getBody().getYear());
    }
}
