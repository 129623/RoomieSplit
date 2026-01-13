package com.roomiesplit.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.roomiesplit.backend.common.Result;
import com.roomiesplit.backend.domain.KarmaRecord;
import com.roomiesplit.backend.dto.KarmaDrawRequest;
import com.roomiesplit.backend.dto.KarmaWorkRequest;

public interface KarmaService extends IService<KarmaRecord> {
    Result<?> getKarmaStats(Long ledgerId);

    Result<?> recordWork(Long ledgerId, KarmaWorkRequest request, Long userId);

    Result<?> draw(Long ledgerId, KarmaDrawRequest request, Long userId);
}
