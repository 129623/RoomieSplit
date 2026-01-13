package com.roomiesplit.backend;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.roomiesplit.backend.domain.Ledger;
import com.roomiesplit.backend.domain.LedgerMember;
import com.roomiesplit.backend.domain.SysUser;
import com.roomiesplit.backend.mapper.LedgerMapper;
import com.roomiesplit.backend.mapper.LedgerMemberMapper;
import com.roomiesplit.backend.mapper.SysUserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class DebugDatabaseTest {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private LedgerMapper ledgerMapper;

    @Autowired
    private LedgerMemberMapper ledgerMemberMapper;

    @Test
    public void dumpDatabaseInfo() {
        System.out.println("================= DEBUG DATABASE START =================");

        List<SysUser> users = sysUserMapper.selectList(null);
        System.out.println("--- All Users (" + users.size() + ") ---");
        users.forEach(u -> System.out
                .println("User ID: " + u.getId() + ", Email: " + u.getEmail() + ", Name: " + u.getDisplayName()));

        List<Ledger> ledgers = ledgerMapper.selectList(null);
        System.out.println("\n--- All Ledgers (" + ledgers.size() + ") ---");
        ledgers.forEach(l -> System.out
                .println("Ledger ID: " + l.getId() + ", Name: " + l.getName() + ", OwnerID: " + l.getOwnerId()));

        List<LedgerMember> members = ledgerMemberMapper.selectList(null);
        System.out.println("\n--- All Ledger Members (" + members.size() + ") ---");
        members.forEach(m -> System.out.println("Member ID: " + m.getId() + ", LedgerID: " + m.getLedgerId()
                + ", UserID: " + m.getUserId() + ", Role: " + m.getRole()));

        System.out.println("\n--- Relationship Details ---");
        ledgers.forEach(l -> {
            System.out.println("In Ledger [" + l.getName() + " (ID: " + l.getId() + ")]: ");
            members.stream().filter(m -> m.getLedgerId().equals(l.getId())).forEach(m -> {
                String userName = users.stream().filter(u -> u.getId().equals(m.getUserId())).findFirst()
                        .map(SysUser::getDisplayName).orElse("Unknown");
                String userEmail = users.stream().filter(u -> u.getId().equals(m.getUserId())).findFirst()
                        .map(SysUser::getEmail).orElse("Unknown");
                System.out.println("  - User: " + userName + " (ID: " + m.getUserId() + ", Email: " + userEmail + ")");
            });
        });

        System.out.println("================= DEBUG DATABASE END =================");
    }
}
