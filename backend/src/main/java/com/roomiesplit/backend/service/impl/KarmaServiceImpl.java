package com.roomiesplit.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.roomiesplit.backend.common.Result;
import com.roomiesplit.backend.domain.KarmaRecord;
import com.roomiesplit.backend.domain.LedgerMember;
import com.roomiesplit.backend.domain.SysUser;
import com.roomiesplit.backend.dto.KarmaDrawRequest;
import com.roomiesplit.backend.dto.KarmaWorkRequest;
import com.roomiesplit.backend.mapper.KarmaRecordMapper;
import com.roomiesplit.backend.mapper.LedgerMemberMapper;
import com.roomiesplit.backend.mapper.SysUserMapper;
import com.roomiesplit.backend.service.KarmaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class KarmaServiceImpl extends ServiceImpl<KarmaRecordMapper, KarmaRecord> implements KarmaService {

    @Autowired
    private LedgerMemberMapper ledgerMemberMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    /**
     * 获取账本内的人品统计及抽签概率
     *
     * @param ledgerId 账本ID
     * @return 包含每位成员积分和抽中概率的统计列表
     */
    @Override
    public Result<?> getKarmaStats(Long ledgerId) {
        // 1. 获取账本所有成员
        LambdaQueryWrapper<LedgerMember> memberQuery = new LambdaQueryWrapper<>();
        memberQuery.eq(LedgerMember::getLedgerId, ledgerId);
        List<LedgerMember> members = ledgerMemberMapper.selectList(memberQuery);
        List<Long> userIds = members.stream().map(LedgerMember::getUserId).collect(Collectors.toList());

        if (userIds.isEmpty()) {
            return Result.success(Collections.emptyList());
        }

        // 2. 获取用户基本信息
        List<SysUser> users = sysUserMapper.selectBatchIds(userIds);
        Map<Long, SysUser> userMap = users.stream().collect(Collectors.toMap(SysUser::getId, u -> u));

        // 3. 计算每位用户的积分
        LambdaQueryWrapper<KarmaRecord> karmaQuery = new LambdaQueryWrapper<>();
        karmaQuery.eq(KarmaRecord::getLedgerId, ledgerId);
        List<KarmaRecord> records = this.list(karmaQuery);

        Map<Long, Integer> userPoints = new HashMap<>();
        for (Long uid : userIds) {
            userPoints.put(uid, 0);
        }
        for (KarmaRecord r : records) {
            userPoints.put(r.getUserId(), userPoints.getOrDefault(r.getUserId(), 0) + r.getPoints());
        }

        // 4. 计算权重 (概率)
        // 逻辑：权重 = 100 - TotalPoints (最小权重为 10)。积分越高，权重越低，被抽中干活的概率越低。
        // 即：干活多(积分高)的人，下次再干活的概率小。
        double totalWeight = 0;
        Map<Long, Double> userWeights = new HashMap<>();

        for (Long uid : userIds) {
            int points = userPoints.get(uid);
            // 简单的线性衰减模型：Weight = Max(5, 100 - points)
            double weight = Math.max(5.0, 100.0 - points);
            userWeights.put(uid, weight);
            totalWeight += weight;
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Long uid : userIds) {
            Map<String, Object> map = new HashMap<>();
            SysUser u = userMap.get(uid);
            if (u == null)
                continue;

            double weight = userWeights.get(uid);
            double prob = totalWeight > 0 ? (weight / totalWeight) * 100 : 0;

            map.put("userId", u.getId());
            map.put("username", u.getUsername());
            map.put("displayName", u.getDisplayName());
            map.put("avatarUrl", u.getAvatarUrl());
            map.put("accumulatedPoints", userPoints.get(uid));
            map.put("finalWeight", weight);
            map.put("probability", prob);

            result.add(map);
        }

        // 按被抽中概率从高到低排序 (大概率干活的人排前面)
        result.sort((a, b) -> Double.compare((Double) b.get("probability"), (Double) a.get("probability")));

        return Result.success(result);
    }

    /**
     * 记录人品事件 (干活加分)
     *
     * @param ledgerId 账本ID
     * @param request  记录请求，包含增加的积分数
     * @param userId   操作用户ID
     * @return 记录结果
     */
    @Override
    public Result<?> recordWork(Long ledgerId, KarmaWorkRequest request, Long userId) {
        KarmaRecord record = new KarmaRecord();
        record.setLedgerId(ledgerId);
        record.setUserId(userId);
        record.setPoints(request.getPoints());
        record.setDescription(request.getDescription());
        record.setCreatedAt(LocalDateTime.now());

        this.save(record);

        return Result.success("人品值已更新", record);
    }

    /**
     * 命运轮盘抽签 (Do or Not Do)
     *
     * @param ledgerId 账本ID
     * @param request  抽签请求
     * @param userId   发起抽签的用户ID
     * @return 抽签结果 (中奖者信息)
     */
    @Override
    @Transactional
    public Result<?> draw(Long ledgerId, KarmaDrawRequest request, Long userId) {
        // 先获取最新的统计权重
        Result<?> statsResult = getKarmaStats(ledgerId);
        List<Map<String, Object>> stats = (List<Map<String, Object>>) statsResult.getData();

        if (stats == null || stats.isEmpty()) {
            return Result.error(404, "No members found");
        }

        // 根据权重进行加权随机选择
        double totalWeight = stats.stream().mapToDouble(m -> (Double) m.get("finalWeight")).sum();
        double random = new Random().nextDouble() * totalWeight;

        Long winnerId = null;
        String winnerName = "";

        double current = 0;
        for (Map<String, Object> m : stats) {
            current += (Double) m.get("finalWeight");
            if (random <= current) {
                winnerId = (Long) m.get("userId");
                winnerName = (String) m.get("displayName");
                break;
            }
        }

        // 兜底逻辑 (理论上不应触发，除非精度问题)
        if (winnerId == null) {
            winnerId = (Long) stats.get(stats.size() - 1).get("userId");
            winnerName = (String) stats.get(stats.size() - 1).get("displayName");
        }

        // 注意：此API仅作随机选择，并不直接修改积分。
        // 被选中的用户应当在完成任务后调用 recordWork 增加积分。

        Map<String, Object> result = new HashMap<>();
        result.put("winnerId", winnerId);
        result.put("winnerName", winnerName);
        result.put("task", request.getTask());
        result.put("message", "命运做出了选择: " + winnerName);

        return Result.success(result);
    }
}
