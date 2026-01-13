package com.roomiesplit.backend.controller;

import com.roomiesplit.backend.common.Result;
import com.roomiesplit.backend.dto.CreatePollRequest;
import com.roomiesplit.backend.dto.VoteRequest;
import com.roomiesplit.backend.service.PollService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class PollController {

    @Autowired
    private PollService pollService;

    /**
     * 创建投票
     *
     * @param ledgerId 账本ID
     * @param request  投票详情 (标题、选项、模式)
     * @param userId   创建用户ID
     * @return 创建结果
     */
    @PostMapping("/ledgers/{ledgerId}/polls")
    public Result<?> createPoll(@PathVariable Long ledgerId,
            @RequestBody CreatePollRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        return pollService.createPoll(ledgerId, request, userId);
    }

    /**
     * 获取账本内的投票列表
     *
     * @param ledgerId 账本ID
     * @return 投票列表
     */
    @GetMapping("/ledgers/{ledgerId}/polls")
    public Result<?> getPolls(@PathVariable Long ledgerId) {
        return pollService.getPollsByLedger(ledgerId);
    }

    /**
     * 提交投票
     *
     * @param pollId  投票ID
     * @param request 投票选择 (选项索引)
     * @param userId  投票用户ID
     * @return 操作结果
     */
    @PostMapping("/polls/{pollId}/vote")
    public Result<?> vote(@PathVariable Long pollId,
            @RequestBody VoteRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        return pollService.vote(pollId, request.getOptionIndex(), userId);
    }

    /**
     * 获取投票结果
     *
     * @param pollId 投票ID
     * @return 结果详情
     */
    @GetMapping("/polls/{pollId}/result")
    public Result<?> getPollResult(@PathVariable Long pollId) {
        return pollService.getPollResult(pollId);
    }
}
