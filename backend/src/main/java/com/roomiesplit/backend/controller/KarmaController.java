package com.roomiesplit.backend.controller;

import com.roomiesplit.backend.common.Result;
import com.roomiesplit.backend.dto.KarmaDrawRequest;
import com.roomiesplit.backend.dto.KarmaWorkRequest;
import com.roomiesplit.backend.service.KarmaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ledgers/{ledgerId}/karma")
@CrossOrigin(origins = "*")
public class KarmaController {

    @Autowired
    private KarmaService karmaService;

    /**
     * 获取人品(Karma)统计信息
     *
     * @param ledgerId 账本ID
     * @return 统计列表 (包含用户积分、权重、抽中概率)
     */
    @GetMapping
    public Result<?> getKarmaStats(@PathVariable Long ledgerId) {
        return karmaService.getKarmaStats(ledgerId);
    }

    /**
     * 记录人品行为 (如完成家务)
     *
     * @param ledgerId 账本ID
     * @param request  请求体 (包含增加的积分和描述)
     * @param userId   操作用户ID
     * @return 记录结果
     */
    @PostMapping("/work")
    public Result<?> recordWork(@PathVariable Long ledgerId,
            @RequestBody KarmaWorkRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        return karmaService.recordWork(ledgerId, request, userId);
    }

    /**
     * 发起命运轮盘抽签
     *
     * @param ledgerId 账本ID
     * @param request  抽签请求
     * @param userId   发起用户ID (可选)
     * @return 抽签结果 (中奖者)
     */
    @PostMapping("/draw")
    public Result<?> draw(@PathVariable Long ledgerId,
            @RequestBody KarmaDrawRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        // userId is optional for drawing, but good for logging
        if (userId == null)
            userId = 0L;
        return karmaService.draw(ledgerId, request, userId);
    }
}
