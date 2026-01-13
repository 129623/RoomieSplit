-- Create Database (if not exists, manual step usually, but good to have)
-- CREATE DATABASE roomiesplit CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE roomiesplit;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `email` varchar(100) NOT NULL COMMENT '邮箱',
  `password` varchar(100) NOT NULL COMMENT '密码',
  `display_name` varchar(50) DEFAULT NULL COMMENT '显示名称',
  `avatar_url` varchar(255) DEFAULT NULL COMMENT '头像URL',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_email` (`email`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ----------------------------
-- Table structure for ledger
-- ----------------------------
DROP TABLE IF EXISTS `ledger`;
CREATE TABLE `ledger` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) NOT NULL COMMENT '账本名称',
  `description` varchar(255) DEFAULT NULL COMMENT '描述',
  `owner_id` bigint(20) NOT NULL COMMENT '创建者/拥有者ID',
  `default_currency` varchar(10) DEFAULT 'CNY' COMMENT '默认货币',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账本/群组表';

-- ----------------------------
-- Table structure for ledger_member
-- ----------------------------
DROP TABLE IF EXISTS `ledger_member`;
CREATE TABLE `ledger_member` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `ledger_id` bigint(20) NOT NULL COMMENT '账本ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `role` varchar(20) DEFAULT 'MEMBER' COMMENT '角色: OWNER, ADMIN, MEMBER',
  `status` varchar(20) DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE, INVITED',
  `member_status` varchar(20) DEFAULT 'AVAILABLE' COMMENT '室友状态: AVAILABLE, BUSY, AWAY, ASLEEP',
  `joined_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ledger_user` (`ledger_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账本成员表';

-- ----------------------------
-- Table structure for transaction_record
-- ----------------------------
DROP TABLE IF EXISTS `transaction_record`;
CREATE TABLE `transaction_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `ledger_id` bigint(20) NOT NULL COMMENT '账本ID',
  `payer_id` bigint(20) NOT NULL COMMENT '付款人ID',
  `amount` decimal(10,2) NOT NULL COMMENT '总金额',
  `currency` varchar(10) DEFAULT 'CNY' COMMENT '货币',
  `category` varchar(50) DEFAULT NULL COMMENT '分类',
  `description` varchar(255) DEFAULT NULL COMMENT '描述',
  `transaction_date` datetime NOT NULL COMMENT '交易日期',
  `split_type` varchar(20) DEFAULT 'EQUAL' COMMENT '分摊方式: EQUAL, EXACT, WEIGHTED',
  `image_urls` text COMMENT '图片URL列表(JSON)',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交易记录表';

-- ----------------------------
-- Table structure for transaction_participant
-- ----------------------------
DROP TABLE IF EXISTS `transaction_participant`;
CREATE TABLE `transaction_participant` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `transaction_id` bigint(20) NOT NULL COMMENT '交易ID',
  `user_id` bigint(20) NOT NULL COMMENT '参与者ID',
  `owing_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '应付金额',
  `paid_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '实付金额(在本次交易中)',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交易参与者明细表';

-- ----------------------------
-- Table structure for settlement
-- ----------------------------
DROP TABLE IF EXISTS `settlement`;
CREATE TABLE `settlement` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `ledger_id` bigint(20) NOT NULL COMMENT '账本ID',
  `from_user_id` bigint(20) NOT NULL COMMENT '付款方',
  `to_user_id` bigint(20) NOT NULL COMMENT '收款方',
  `amount` decimal(10,2) NOT NULL COMMENT '金额',
  `currency` varchar(10) DEFAULT 'CNY' COMMENT '货币',
  `status` varchar(20) DEFAULT 'COMPLETED' COMMENT '状态',
  `settled_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '结算时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='结算记录表';

-- ----------------------------
-- Table structure for notification
-- ----------------------------
DROP TABLE IF EXISTS `notification`;
CREATE TABLE `notification` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint(20) NOT NULL COMMENT '接收用户ID',
  `type` varchar(50) NOT NULL COMMENT '类型: PAYMENT_REMINDER, INVITE_REQUEST',
  `title` varchar(100) NOT NULL COMMENT '标题',
  `message` varchar(255) DEFAULT NULL COMMENT '内容',
  `is_read` tinyint(1) DEFAULT 0 COMMENT '是否已读',
  `action_url` varchar(255) DEFAULT NULL COMMENT '跳转链接',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知表';

-- ----------------------------
-- Table structure for poll
-- ----------------------------
DROP TABLE IF EXISTS `poll`;
CREATE TABLE `poll` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `ledger_id` bigint(20) NOT NULL COMMENT '账本ID',
  `title` varchar(100) NOT NULL COMMENT '投票标题',
  `mode` varchar(20) DEFAULT 'VOTE' COMMENT '模式: VOTE, RANDOM',
  `options` text COMMENT '选项列表(JSON)',
  `status` varchar(20) DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE, COMPLETED',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='投票表';

-- ----------------------------
-- Table structure for poll_vote
-- ----------------------------
DROP TABLE IF EXISTS `poll_vote`;
CREATE TABLE `poll_vote` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `poll_id` bigint(20) NOT NULL COMMENT '投票ID',
  `user_id` bigint(20) NOT NULL COMMENT '投票用户ID',
  `option_index` int(11) NOT NULL COMMENT '选项索引',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_poll_user` (`poll_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='投票记录表';

-- ----------------------------
-- Table structure for karma_record
-- ----------------------------
DROP TABLE IF EXISTS `karma_record`;
CREATE TABLE `karma_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `ledger_id` bigint(20) NOT NULL COMMENT '账本ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `points` int(11) NOT NULL COMMENT '积分变动',
  `description` varchar(255) DEFAULT NULL COMMENT '描述',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人品值/Karma记录表';

-- ----------------------------
-- Table structure for invitation
-- ----------------------------
DROP TABLE IF EXISTS `invitation`;
CREATE TABLE `invitation` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `ledger_id` bigint(20) NOT NULL COMMENT '账本ID',
  `sender_id` bigint(20) NOT NULL COMMENT '邀请人ID',
  `email` varchar(100) NOT NULL COMMENT '被邀请人邮箱',
  `token` varchar(64) NOT NULL COMMENT '邀请Token',
  `status` varchar(20) DEFAULT 'PENDING' COMMENT '状态: PENDING, ACCEPTED, REJECTED, EXPIRED',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `expires_at` datetime DEFAULT NULL COMMENT '过期时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邀请表';

SET FOREIGN_KEY_CHECKS = 1;
