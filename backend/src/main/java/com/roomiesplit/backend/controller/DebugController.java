package com.roomiesplit.backend.controller;

import com.roomiesplit.backend.common.Result;
import com.roomiesplit.backend.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/debug")
@CrossOrigin(origins = "*")
public class DebugController {

    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private LedgerMapper ledgerMapper;
    @Autowired
    private LedgerMemberMapper ledgerMemberMapper;

    @GetMapping("/dump")
    public Result<?> dump() {
        Map<String, Object> map = new HashMap<>();
        map.put("users", sysUserMapper.selectList(null));
        map.put("ledgers", ledgerMapper.selectList(null));
        map.put("members", ledgerMemberMapper.selectList(null));
        return Result.success(map);
    }
}
