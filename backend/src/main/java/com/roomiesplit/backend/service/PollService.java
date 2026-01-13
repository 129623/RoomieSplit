package com.roomiesplit.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.roomiesplit.backend.common.Result;
import com.roomiesplit.backend.domain.Poll;
import com.roomiesplit.backend.dto.CreatePollRequest;

public interface PollService extends IService<Poll> {
    Result<?> createPoll(Long ledgerId, CreatePollRequest request, Long userId);

    Result<?> vote(Long pollId, Integer optionIndex, Long userId);

    Result<?> getPollResult(Long pollId);

    Result<?> getPollsByLedger(Long ledgerId);
}
