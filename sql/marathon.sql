-- 校园马拉松报名系统 初始化脚本
-- 使用方法：mysql -u root -p < marathon.sql

CREATE DATABASE IF NOT EXISTS campus_marathon
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE campus_marathon;

DROP TABLE IF EXISTS marathon_runner;

CREATE TABLE marathon_runner (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '报名编号',
    name            VARCHAR(50)  NOT NULL                COMMENT '参赛者姓名',
    student_id      VARCHAR(20)  NOT NULL                COMMENT '学号',
    phone           VARCHAR(20)  NOT NULL                COMMENT '联系电话',
    college         VARCHAR(60)  DEFAULT NULL            COMMENT '所属学院',
    event_category  VARCHAR(20)  NOT NULL                COMMENT '参赛项目：HALF / MINI / FUN',
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '报名状态：PENDING / CONFIRMED / REJECTED / CANCELLED',
    pay_status      VARCHAR(20)  NOT NULL DEFAULT 'UNPAID' COMMENT '支付状态：UNPAID / PAID / REFUNDED',
    out_trade_no    VARCHAR(64)  DEFAULT NULL            COMMENT '支付订单号（支付宝商户单号）',
    pay_amount      DECIMAL(10,2) NOT NULL DEFAULT 30.00 COMMENT '报名费（元）',
    pay_time        DATETIME     DEFAULT NULL            COMMENT '支付完成时间',
    remark          VARCHAR(200) DEFAULT NULL            COMMENT '备注',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '报名时间',
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_student_id (student_id),
    KEY idx_category_status (event_category, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '马拉松报名记录';

DROP TABLE IF EXISTS runner_account;

CREATE TABLE runner_account (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '账号编号',
    student_id  VARCHAR(20)  NOT NULL                COMMENT '学号（登录账号）',
    name        VARCHAR(50)  NOT NULL                COMMENT '姓名',
    phone       VARCHAR(20)  NOT NULL                COMMENT '手机号',
    college     VARCHAR(60)                           COMMENT '所属学院（选填）',
    credential  VARCHAR(255) NOT NULL                COMMENT '密码（PBKDF2 加盐哈希）',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_account_student (student_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '参赛者账号（注册后登录报名）';

INSERT INTO marathon_runner
    (name, student_id, phone, college, event_category, status, pay_status, out_trade_no, pay_time, remark, created_at)
VALUES
    ('林一舟', '2023010203', '13800138001', '计算机科学与技术学院', 'HALF', 'CONFIRMED', 'PAID', 'SEED202610200001', '2026-10-20 09:13:00', '半马完赛目标 1 小时 45 分', '2026-10-20 09:12:00'),
    ('苏晓棠', '2023020511', '13900139002', '外国语学院', 'MINI', 'CONFIRMED', 'PAID', 'SEED202610200002', '2026-10-20 10:06:00', '首次参赛，希望有配速员带跑', '2026-10-20 10:05:00'),
    ('陈子昂', '2022040817', '15900159003', '机械工程学院', 'HALF', 'PENDING', 'UNPAID', NULL, NULL, NULL, '2026-10-21 08:40:00'),
    ('周清和', '2024010326', '13700137004', '新闻与传播学院', 'FUN', 'CONFIRMED', 'PAID', 'SEED202610210001', '2026-10-21 11:23:00', '欢乐跑方阵报名', '2026-10-21 11:22:00'),
    ('何知远', '2023050742', '18600186005', '数学与统计学院', 'MINI', 'PENDING', 'UNPAID', NULL, NULL, '对花生过敏，请医护组留意', '2026-10-22 14:18:00'),
    ('沈星遥', '2023060138', '13500135006', '艺术设计学院', 'FUN', 'REJECTED', 'UNPAID', NULL, NULL, '课程冲突，无法参赛', '2026-10-22 16:03:00'),
    ('顾青禾', '2024020955', '18800188007', '经济管理学院', 'HALF', 'PENDING', 'UNPAID', NULL, NULL, '需要寄存大件行李', '2026-10-23 09:47:00'),
    ('许南风', '2022071240', '15000150008', '材料科学与工程学院', 'MINI', 'PENDING', 'UNPAID', NULL, NULL, NULL, '2026-10-23 15:26:00');

-- AI 知识库：文档与切片
DROP TABLE IF EXISTS kb_chunk;
DROP TABLE IF EXISTS kb_document;

CREATE TABLE kb_document (
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '文档编号',
    owner_student_id  VARCHAR(40)  NOT NULL                COMMENT '归属用户（学号，管理员为账号名）',
    owner_name        VARCHAR(50)  DEFAULT NULL            COMMENT '归属用户姓名',
    title             VARCHAR(200) NOT NULL                COMMENT '文档标题（取文件名去扩展名）',
    file_name         VARCHAR(200) NOT NULL                COMMENT '原始文件名',
    content_type      VARCHAR(80)  DEFAULT NULL            COMMENT '原始 MIME 类型',
    char_count        INT          NOT NULL DEFAULT 0      COMMENT '提取后的字符数',
    chunk_count       INT          NOT NULL DEFAULT 0      COMMENT '切片数量',
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '导入时间',
    is_admin_visible  TINYINT      NOT NULL DEFAULT 0      COMMENT '管理员导入的文档标记',
    PRIMARY KEY (id),
    KEY idx_kb_owner (owner_student_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '知识库文档';

CREATE TABLE kb_chunk (
    id          BIGINT NOT NULL AUTO_INCREMENT COMMENT '切片编号',
    doc_id      BIGINT NOT NULL                COMMENT '所属文档',
    seq         INT    NOT NULL                COMMENT '文档内顺序（从 0 开始）',
    content     TEXT   NOT NULL                COMMENT '切片正文',
    char_count  INT    NOT NULL DEFAULT 0      COMMENT '切片字符数',
    PRIMARY KEY (id),
    KEY idx_kb_chunk_doc (doc_id, seq)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '知识库切片';

-- ==============================================================
-- 竞赛管理系统（contest / contestant / judge / criterion / score）
-- ==============================================================

DROP TABLE IF EXISTS score;
DROP TABLE IF EXISTS criterion;
DROP TABLE IF EXISTS judge;
DROP TABLE IF EXISTS contestant;
DROP TABLE IF EXISTS contest;

CREATE TABLE contest (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '竞赛编号',
    name        VARCHAR(200) NOT NULL                COMMENT '竞赛名称',
    description TEXT         DEFAULT NULL            COMMENT '竞赛描述',
    status      VARCHAR(20)  NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT / JUDGING / FINISHED',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_contest_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '竞赛';

CREATE TABLE contestant (
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '参赛者编号',
    contest_id   BIGINT      NOT NULL                COMMENT '所属竞赛',
    name         VARCHAR(50) DEFAULT NULL            COMMENT '姓名',
    student_id   VARCHAR(40) DEFAULT NULL            COMMENT '学号',
    team_name    VARCHAR(100) DEFAULT NULL           COMMENT '团队名称',
    submitted_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    PRIMARY KEY (id),
    KEY idx_contestant_contest (contest_id),
    CONSTRAINT fk_contestant_contest FOREIGN KEY (contest_id) REFERENCES contest(id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '参赛者';

CREATE TABLE judge (
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '评审编号',
    contest_id   BIGINT      NOT NULL                COMMENT '所属竞赛',
    username     VARCHAR(50) NOT NULL                COMMENT '登录用户名',
    display_name VARCHAR(50) DEFAULT NULL            COMMENT '展示名',
    credential   VARCHAR(128) NOT NULL               COMMENT '密码哈希（SHA-256(salt+password) hex）',
    salt         VARCHAR(64) NOT NULL                COMMENT '盐',
    created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_judge_contest_username (contest_id, username),
    KEY idx_judge_username (username),
    CONSTRAINT fk_judge_contest FOREIGN KEY (contest_id) REFERENCES contest(id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '评审账号';

CREATE TABLE criterion (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '维度编号',
    contest_id  BIGINT       NOT NULL                COMMENT '所属竞赛',
    name        VARCHAR(100) NOT NULL                COMMENT '维度名称',
    weight      INT          NOT NULL DEFAULT 0      COMMENT '权重（0-100，权重和为 100）',
    description VARCHAR(500) DEFAULT NULL            COMMENT '维度说明',
    PRIMARY KEY (id),
    KEY idx_criterion_contest (contest_id),
    CONSTRAINT fk_criterion_contest FOREIGN KEY (contest_id) REFERENCES contest(id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '评分维度';

CREATE TABLE score (
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '评分记录编号',
    contestant_id BIGINT     NOT NULL                COMMENT '参赛者',
    judge_id     BIGINT      NOT NULL                COMMENT '评审',
    criterion_id BIGINT      NOT NULL                COMMENT '评分维度',
    score        DECIMAL(4,2) NOT NULL               COMMENT '分数（0-10）',
    comment      VARCHAR(500) DEFAULT NULL            COMMENT '评语',
    created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评分时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_score_unique (judge_id, contestant_id, criterion_id),
    KEY idx_score_contestant (contestant_id),
    KEY idx_score_judge (judge_id),
    KEY idx_score_criterion (criterion_id),
    CONSTRAINT fk_score_contestant FOREIGN KEY (contestant_id) REFERENCES contestant(id) ON DELETE CASCADE,
    CONSTRAINT fk_score_judge FOREIGN KEY (judge_id) REFERENCES judge(id) ON DELETE CASCADE,
    CONSTRAINT fk_score_criterion FOREIGN KEY (criterion_id) REFERENCES criterion(id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '评分记录';

-- 种子数据：一场示例竞赛（DRAFT 状态）+ 2 参赛者 + 1 评审（judge1 / judge1）+ 3 维度
INSERT INTO contest (name, description, status) VALUES
    ('2026 校园程序设计大赛', '面向全校本科生的算法与工程综合竞赛，分评审维度加权排名。', 'DRAFT');

INSERT INTO contestant (contest_id, name, student_id, team_name) VALUES
    (1, '林小白', '2023010101', '灵犀小组'),
    (1, '陈大力', '2022020202', '愚公移山队');

-- 评审密码：credential = SHA-256(salt + "judge1") hex，salt = "seed0000000000000001"
-- 与 PasswordUtil.hash(salt, password) 算法一致，评审登录 judge1 / judge1
INSERT INTO judge (contest_id, username, display_name, credential, salt) VALUES
    (1, 'judge1', '示范评审', '7c7cd9121f33d9b85be0e3cf570e02aa391157bb716ebceda700931ece441b50', 'seed0000000000000001');

INSERT INTO criterion (contest_id, name, weight, description) VALUES
    (1, '技术难度', 30, '算法复杂度、数据结构运用、边界条件处理'),
    (1, '创新性',   40, '解法新颖性、工程巧思、文档表达'),
    (1, '完成度',   30, '功能完备度、鲁棒性、性能与代码规范');
