package com.roomiesplit.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.roomiesplit.backend.common.Result;
import com.roomiesplit.backend.domain.Invitation;
import com.roomiesplit.backend.dto.InviteRequest;

public interface InvitationService extends IService<Invitation> {
    Result<?> invite(Long ledgerId, InviteRequest request, Long userId);

    Result<?> accept(String token, Long userId);

    Result<?> reject(String token, Long userId);
}
