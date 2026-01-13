package com.roomiesplit.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomiesplit.backend.common.Result;
import com.roomiesplit.backend.domain.Poll;
import com.roomiesplit.backend.domain.PollVote;
import com.roomiesplit.backend.dto.CreatePollRequest;
import com.roomiesplit.backend.mapper.PollMapper;
import com.roomiesplit.backend.mapper.PollVoteMapper;
import com.roomiesplit.backend.service.NotificationService;
import com.roomiesplit.backend.service.PollService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class PollServiceImpl extends ServiceImpl<PollMapper, Poll> implements PollService {

    @Autowired
    private PollVoteMapper pollVoteMapper;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 创建新投票
     *
     * @param ledgerId 账本ID
     * @param request  投票请求，包含标题、选项、模式
     * @param userId   创建者ID
     * @return 结果
     */
    @Override
    public Result<?> createPoll(Long ledgerId, CreatePollRequest request, Long userId) {
        Poll poll = new Poll();
        poll.setLedgerId(ledgerId);
        poll.setTitle(request.getTitle());
        poll.setMode(request.getMode());
        try {
            // 将选项列表序列化为 JSON 字符串存储
            poll.setOptions(objectMapper.writeValueAsString(request.getOptions()));
        } catch (JsonProcessingException e) {
            return Result.error(500, "选项序列化失败");
        }
        poll.setStatus("ACTIVE");
        poll.setCreatedAt(LocalDateTime.now());

        this.save(poll);
        return Result.success("投票创建成功", poll);
    }

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private com.roomiesplit.backend.mapper.LedgerMemberMapper ledgerMemberMapper;

    /**
     * 投票
     *
     * @param pollId      投票ID
     * @param optionIndex 选项索引
     * @param userId      投票用户ID
     * @return 结果
     */
    @Override
    @Transactional
    public Result<?> vote(Long pollId, Integer optionIndex, Long userId) {
        Poll poll = this.getById(pollId);
        if (poll == null) {
            return Result.error(404, "投票不存在");
        }
        if ("COMPLETED".equals(poll.getStatus())) {
            return Result.error(400, "投票已结束");
        }

        // 检查是否重复投票
        LambdaQueryWrapper<PollVote> query = new LambdaQueryWrapper<>();
        query.eq(PollVote::getPollId, pollId).eq(PollVote::getUserId, userId);
        if (pollVoteMapper.selectCount(query) > 0) {
            return Result.error(409, "您已投过票");
        }

        PollVote vote = new PollVote();
        vote.setPollId(pollId);
        vote.setUserId(userId);
        vote.setOptionIndex(optionIndex);
        vote.setCreatedAt(LocalDateTime.now());
        pollVoteMapper.insert(vote);

        // === Auto-settlement logic ===
        // Check if all members have voted
        LambdaQueryWrapper<com.roomiesplit.backend.domain.LedgerMember> memberQuery = new LambdaQueryWrapper<>();
        memberQuery.eq(com.roomiesplit.backend.domain.LedgerMember::getLedgerId, poll.getLedgerId());
        long totalMembers = ledgerMemberMapper.selectCount(memberQuery);

        LambdaQueryWrapper<PollVote> voteQuery = new LambdaQueryWrapper<>();
        voteQuery.eq(PollVote::getPollId, pollId);
        long totalVotes = pollVoteMapper.selectCount(voteQuery);

        System.out.println("=== Auto-settlement Check ===");
        System.out.println("Total members: " + totalMembers + ", Total votes: " + totalVotes);

        if (totalVotes >= totalMembers && totalMembers > 0) {
            // All members have voted, settle the poll
            poll.setStatus("COMPLETED");
            this.updateById(poll);

            // Calculate final result (reuse getPollResult logic)
            Result<?> resultObj = getPollResult(pollId);
            if (resultObj.getCode() == 200) {
                @SuppressWarnings("unchecked")
                Map<String, Object> resultData = (Map<String, Object>) resultObj.getData();
                String winner = (String) resultData.get("winner");

                System.out.println("✓ Poll auto-settled! Winner: " + winner);

                // Broadcast notification to all members
                List<com.roomiesplit.backend.domain.LedgerMember> members = ledgerMemberMapper.selectList(memberQuery);
                for (com.roomiesplit.backend.domain.LedgerMember member : members) {
                    notificationService.sendNotification(
                            member.getUserId(),
                            "POLL_RESULT",
                            poll.getTitle(),
                            "投票已结束！结果是: " + winner,
                            "/decision");
                }
                System.out.println("✓ Notifications sent to " + members.size() + " members");
            }
        }

        return Result.success("投票成功");
    }

    @Autowired
    private com.roomiesplit.backend.mapper.TransactionRecordMapper transactionRecordMapper;

    /**
     * 获取投票详情及实时结果
     *
     * @param pollId 投票ID
     * @return 结果详情 (含各选项票数)
     */
    @Override
    public Result<?> getPollResult(Long pollId) {
        Poll poll = this.getById(pollId);
        if (poll == null) {
            return Result.error(404, "投票不存在");
        }

        List<String> optionsList;
        try {
            optionsList = objectMapper.readValue(poll.getOptions(), new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException e) {
            return Result.error(500, "选项解析失败");
        }

        // 1. Identify "Gold Payer" (Highest spender in this ledger)
        // Weight: Gold Payer = 1.5, Others = 1.0
        Long goldPayerId = -1L;
        try {
            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.roomiesplit.backend.domain.TransactionRecord> tq = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            tq.select("payer_id", "sum(amount) as total")
                    .eq("ledger_id", poll.getLedgerId())
                    .groupBy("payer_id")
                    .orderByDesc("total");

            List<Map<String, Object>> res = transactionRecordMapper.selectMaps(tq);
            System.out.println("=== Gold Payer Detection for Ledger: " + poll.getLedgerId() + " ===");
            System.out.println("Query results count: " + (res != null ? res.size() : 0));

            if (res != null && !res.isEmpty()) {
                // Log all results for debugging
                for (int i = 0; i < res.size(); i++) {
                    Map<String, Object> r = res.get(i);
                    System.out.println("Rank #" + (i + 1) + ": " + r);
                }

                // Determine max (first one since ordered by desc)
                Map<String, Object> row = res.get(0);
                for (String key : row.keySet()) {
                    if ("payer_id".equalsIgnoreCase(key)) {
                        Object pidObj = row.get(key);
                        if (pidObj != null) {
                            goldPayerId = Long.valueOf(pidObj.toString());
                            System.out.println(
                                    "✓ Gold Payer ID identified: " + goldPayerId + " with total: " + row.get("total"));
                        }
                        break;
                    }
                }
            } else {
                System.out.println("✗ No transaction records found - all votes will have equal weight");
            }
        } catch (Exception e) {
            System.err.println("✗ Error calculating Gold Payer: " + e.getMessage());
            e.printStackTrace(); // Fail safe to equal weights
        }

        // 2. Tally Votes with Weights
        LambdaQueryWrapper<PollVote> query = new LambdaQueryWrapper<>();
        query.eq(PollVote::getPollId, pollId);
        List<PollVote> votes = pollVoteMapper.selectList(query);

        System.out.println("=== Vote Tallying (Gold Payer: " + goldPayerId + ") ===");

        Map<Integer, Double> weightedCounts = new HashMap<>(); // Using double for weights
        Map<Integer, Integer> rawCounts = new HashMap<>();

        for (PollVote v : votes) {
            double weight = 1.0;
            if (v.getUserId().equals(goldPayerId)) {
                weight = 1.5;
                System.out.println("Vote from User " + v.getUserId() + " (GOLD PAYER) -> Option " + v.getOptionIndex()
                        + " [Weight: " + weight + "]");
            } else {
                System.out.println("Vote from User " + v.getUserId() + " -> Option " + v.getOptionIndex() + " [Weight: "
                        + weight + "]");
            }
            weightedCounts.put(v.getOptionIndex(), weightedCounts.getOrDefault(v.getOptionIndex(), 0.0) + weight);
            rawCounts.put(v.getOptionIndex(), rawCounts.getOrDefault(v.getOptionIndex(), 0) + 1);
        }

        List<Map<String, Object>> details = new ArrayList<>();
        double maxScore = -1.0;
        String winner = "暂无结果";

        if (votes.isEmpty()) {
            winner = "等待投票...";
        }

        for (int i = 0; i < optionsList.size(); i++) {
            Map<String, Object> detail = new HashMap<>();
            String opt = optionsList.get(i);
            Double score = weightedCounts.getOrDefault(i, 0.0);
            Integer raw = rawCounts.getOrDefault(i, 0);

            detail.put("option", opt);
            detail.put("score", score); // Weighted score
            detail.put("count", raw); // Head count
            details.add(detail);

            if (score > maxScore) {
                maxScore = score;
                winner = opt;
            } else if (Math.abs(score - maxScore) < 0.001) {
                if (votes.size() > 0)
                    winner = "平局 (Tie)";
            }
        }

        if ("RANDOM".equals(poll.getMode())) {
            // 随机模式实现：不完全依赖票数，而是随机选择
            // 此处简化：只要未结束，每次请求可能返回不同随机值，或者设定一个逻辑来锁定
            if (!"COMPLETED".equals(poll.getStatus())) {
                int randomIdx = new Random().nextInt(optionsList.size());
                winner = optionsList.get(randomIdx);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("poll", poll);
        result.put("winner", winner);
        result.put("details", details);
        result.put("goldPayerId", goldPayerId); // Inform frontend who determines the fate

        return Result.success(result);
    }

    /**
     * 获取账本下的所有投票
     *
     * @param ledgerId 账本ID
     * @return 投票列表
     */
    @Override
    public Result<?> getPollsByLedger(Long ledgerId) {
        LambdaQueryWrapper<Poll> query = new LambdaQueryWrapper<>();
        query.eq(Poll::getLedgerId, ledgerId).orderByDesc(Poll::getCreatedAt);
        return Result.success(this.list(query));
    }
}
