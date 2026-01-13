package com.roomiesplit.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.roomiesplit.backend.common.Result;
import com.roomiesplit.backend.domain.Ledger;
import com.roomiesplit.backend.domain.LedgerMember;
import com.roomiesplit.backend.domain.SysUser;
import com.roomiesplit.backend.dto.CreateLedgerRequest;
import com.roomiesplit.backend.mapper.LedgerMapper;
import com.roomiesplit.backend.mapper.LedgerMemberMapper;
import com.roomiesplit.backend.mapper.SysUserMapper;
import com.roomiesplit.backend.service.LedgerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 账本服务实现
 */
@Service
public class LedgerServiceImpl extends ServiceImpl<LedgerMapper, Ledger> implements LedgerService {

    @Autowired
    private LedgerMemberMapper ledgerMemberMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    /**
     * 创建新账本
     *
     * @param request 创建请求，包含账本名称、描述、默认货币及初始成员邮箱
     * @param userId  创建者ID
     * @return 包含新创建账本信息的 Result 对象
     */
    @Override
    @Transactional
    public Result<?> createLedger(CreateLedgerRequest request, Long userId) {
        // 1. 创建账本实体并保存基本信息
        Ledger ledger = new Ledger();
        ledger.setName(request.getName());
        ledger.setDescription(request.getDescription());
        ledger.setOwnerId(userId);
        ledger.setDefaultCurrency(request.getDefaultCurrency() != null ? request.getDefaultCurrency() : "CNY");
        ledger.setCreatedAt(LocalDateTime.now());

        this.save(ledger);

        // 2. 将创建者添加为初始成员，角色为 OWNER (拥有最高权限)
        LedgerMember ownerMember = new LedgerMember();
        ownerMember.setLedgerId(ledger.getId());
        ownerMember.setUserId(userId);
        ownerMember.setRole("OWNER");
        ownerMember.setStatus("ACTIVE");
        ownerMember.setJoinedAt(LocalDateTime.now());
        ledgerMemberMapper.insert(ownerMember);

        // 3. 处理初始邀请的成员
        // 如果请求中包含初始成员邮箱，尝试查找用户并直接添加为成员
        if (request.getMemberEmails() != null && !request.getMemberEmails().isEmpty()) {
            for (String email : request.getMemberEmails()) {
                LambdaQueryWrapper<SysUser> userQuery = new LambdaQueryWrapper<>();
                userQuery.eq(SysUser::getEmail, email);
                SysUser inviteUser = sysUserMapper.selectOne(userQuery);

                if (inviteUser != null && !inviteUser.getId().equals(userId)) {
                    LedgerMember member = new LedgerMember();
                    member.setLedgerId(ledger.getId());
                    member.setUserId(inviteUser.getId());
                    member.setRole("MEMBER");
                    member.setStatus("ACTIVE"); // 简化逻辑：直接激活，实际业务可能需要发送邀请改为 PENDING 状态
                    member.setJoinedAt(LocalDateTime.now());
                    ledgerMemberMapper.insert(member);
                }
            }
        }

        return Result.success("账本创建成功", ledger);
    }

    /**
     * 获取用户参与的所有账本
     *
     * @param userId 用户ID
     * @return 账本列表
     */
    @Override
    public Result<?> getMyLedgers(Long userId) {
        // 1. 查询该用户在 ledger_member 表中的所有记录
        LambdaQueryWrapper<LedgerMember> memberQuery = new LambdaQueryWrapper<>();
        memberQuery.eq(LedgerMember::getUserId, userId);
        List<LedgerMember> memberships = ledgerMemberMapper.selectList(memberQuery);

        if (memberships.isEmpty()) {
            return Result.success(new ArrayList<>());
        }

        // 2. 提取所有账本ID
        List<Long> ledgerIds = memberships.stream().map(LedgerMember::getLedgerId).collect(Collectors.toList());

        // 3. 根据ID列表批量查询账本详情
        LambdaQueryWrapper<Ledger> ledgerQuery = new LambdaQueryWrapper<>();
        ledgerQuery.in(Ledger::getId, ledgerIds);
        List<Ledger> ledgers = this.list(ledgerQuery);

        return Result.success(ledgers);
    }

    /**
     * 获取账本详细信息（包括成员列表）
     *
     * @param ledgerId 账本ID
     * @param userId   请求用户ID（用于鉴权）
     * @return 包含账本信息、成员列表及当前用户角色的 Map
     */
    @Override
    public Result<?> getLedgerDetail(Long ledgerId, Long userId) {
        // 1. 鉴权：检查请求用户是否为该账本成员
        LambdaQueryWrapper<LedgerMember> memberQuery = new LambdaQueryWrapper<>();
        memberQuery.eq(LedgerMember::getLedgerId, ledgerId)
                .eq(LedgerMember::getUserId, userId);
        LedgerMember membership = ledgerMemberMapper.selectOne(memberQuery);

        if (membership == null) {
            return Result.error(403, "您不是该账本的成员，无法查看详情");
        }

        // 2. 获取账本基本信息
        Ledger ledger = this.getById(ledgerId);
        if (ledger == null) {
            return Result.error(404, "账本不存在");
        }

        // 3. 获取该账本的所有成员列表
        LambdaQueryWrapper<LedgerMember> allMembersQuery = new LambdaQueryWrapper<>();
        allMembersQuery.eq(LedgerMember::getLedgerId, ledgerId);
        List<LedgerMember> members = ledgerMemberMapper.selectList(allMembersQuery);

        // 4. 填充成员的用户详情（如昵称、头像）
        List<Long> userIds = members.stream().map(LedgerMember::getUserId).collect(Collectors.toList());
        List<SysUser> users = sysUserMapper.selectBatchIds(userIds);
        Map<Long, SysUser> userMap = users.stream().collect(Collectors.toMap(SysUser::getId, u -> u));

        // 组装成员详情列表
        List<Map<String, Object>> memberDetails = members.stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("role", m.getRole());
            map.put("status", m.getStatus());
            map.put("memberStatus", m.getMemberStatus());
            map.put("joinedAt", m.getJoinedAt());
            SysUser u = userMap.get(m.getUserId());
            if (u != null) {
                map.put("userId", u.getId());
                map.put("displayName", u.getDisplayName());
                map.put("avatarUrl", u.getAvatarUrl());
                map.put("email", u.getEmail());
            }
            return map;
        }).collect(Collectors.toList());

        // 5. 构建最终返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("ledger", ledger);
        result.put("members", memberDetails);
        result.put("currentUserRole", membership.getRole());

        return Result.success(result);
    }

    @Autowired
    private com.roomiesplit.backend.service.InvitationService invitationService;

    /**
     * 邀请成员加入账本
     *
     * @param ledgerId  账本ID
     * @param email     被邀请人邮箱
     * @param inviterId 邀请人ID
     * @return 邀请结果
     */
    @Override
    @Transactional
    public Result<?> inviteMember(Long ledgerId, String email, Long inviterId) {
        // 1. 权限检查：确保邀请人是该账本的成员
        LambdaQueryWrapper<LedgerMember> memberQuery = new LambdaQueryWrapper<>();
        memberQuery.eq(LedgerMember::getLedgerId, ledgerId).eq(LedgerMember::getUserId, inviterId);
        if (ledgerMemberMapper.selectCount(memberQuery) == 0) {
            return Result.error(403, "您没有权限邀请成员");
        }

        // 2. 委托给 InvitationService 处理具体邀请逻辑（生成邀请码、发送通知等）
        com.roomiesplit.backend.dto.InviteRequest req = new com.roomiesplit.backend.dto.InviteRequest();
        req.setEmail(email);
        return invitationService.invite(ledgerId, req, inviterId);
    }

    @Override
    public Result<?> updateMemberStatus(Long ledgerId, Long userId, String status) {
        LambdaQueryWrapper<LedgerMember> query = new LambdaQueryWrapper<>();
        query.eq(LedgerMember::getLedgerId, ledgerId)
                .eq(LedgerMember::getUserId, userId);
        LedgerMember member = ledgerMemberMapper.selectOne(query);
        if (member == null) {
            return Result.error(404, "Member not found");
        }
        member.setMemberStatus(status);
        ledgerMemberMapper.updateById(member);
        return Result.success("Status updated");
    }
}
