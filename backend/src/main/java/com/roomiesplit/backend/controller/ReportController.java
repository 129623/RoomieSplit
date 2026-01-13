package com.roomiesplit.backend.controller;

import com.roomiesplit.backend.common.Result;
import com.roomiesplit.backend.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/ledgers/{ledgerId}")
@CrossOrigin(origins = "*")
public class ReportController {

    @Autowired
    private ReportService reportService;

    /**
     * 获取债务关系图谱
     *
     * @param ledgerId 账本ID
     * @return 包含节点(Nodes)和边(Edges)的图数据
     */
    @GetMapping("/debt-graph")
    public Result<?> getDebtGraph(@PathVariable Long ledgerId) {
        return reportService.getDebtGraph(ledgerId);
    }

    /**
     * 获取报表数据 (交易列表)
     *
     * @param ledgerId  账本ID
     * @param startDate 开始时间 (可选, 格式 YYYY-MM-DD)
     * @param endDate   结束时间 (可选, 格式 YYYY-MM-DD)
     * @return 交易数据列表
     */
    @GetMapping("/reports/data")
    public Result<?> getReportData(@PathVariable Long ledgerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return reportService.getReportData(ledgerId, startDate, endDate);
    }
}
