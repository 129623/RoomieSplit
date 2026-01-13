package com.roomiesplit.backend.service;

import com.roomiesplit.backend.common.Result;

import java.time.LocalDate;

public interface ReportService {
    Result<?> getDebtGraph(Long ledgerId);

    Result<?> getReportData(Long ledgerId, LocalDate startDate, LocalDate endDate);
}
