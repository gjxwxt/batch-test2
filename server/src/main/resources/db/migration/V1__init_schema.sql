-- ============================================================================
-- IAS Auth Center (Center mode) — Shared Contract DDL (wp-0 / infra:ddl)
-- ----------------------------------------------------------------------------
-- Frozen baseline for all parallel work packages. Six shared tables:
--   admin_user / license / instance / history_instance / audit_log / system_config
-- Target: PostgreSQL 12+ (multi-node shared DB). Seed admin account per O4.
-- Canonical source of truth: contract/ddl/6-tables-ddl.md
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. admin_user — 管理员账号
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS admin_user (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(100) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,          -- BCrypt hash
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE / LOCKED
    last_login_time TIMESTAMP,
    last_login_ip   VARCHAR(50),
    create_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------------------
-- 2. license — 授权
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS license (
    id                  BIGSERIAL PRIMARY KEY,
    serial              VARCHAR(64)  NOT NULL UNIQUE,       -- 全局唯一序列号 (UUID)
    license_name        VARCHAR(200),                       -- 自定义授权名称
    proname             VARCHAR(100) NOT NULL,              -- 产品标识
    component           VARCHAR(100),
    version             VARCHAR(50),
    licensee            VARCHAR(200),
    license_mode        VARCHAR(20)  NOT NULL,              -- center / local / site
    formal              VARCHAR(20),
    expiration          DATE,                               -- 过期时间
    userinfor           VARCHAR(500),
    max_instances       INTEGER,                            -- 实例配额上限
    max_cpus            INTEGER,                            -- CPU 配额（可空）
    max_memory          INTEGER,                            -- 内存配额 MB（可空）
    used_instances      INTEGER NOT NULL DEFAULT 0,         -- 已使用实例数
    remaining_instances INTEGER NOT NULL DEFAULT 0,         -- 剩余可用
    used_cpus           INTEGER NOT NULL DEFAULT 0,
    used_memory         INTEGER NOT NULL DEFAULT 0,
    bxb_file            TEXT,                               -- 原始 XML 文本（防篡改校验）
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE / DISABLED / EXPIRED
    source              VARCHAR(20) NOT NULL DEFAULT 'FILE',    -- FILE / POOL / GENERATED
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_license_proname   ON license (proname);
CREATE INDEX IF NOT EXISTS idx_license_status    ON license (status);
CREATE INDEX IF NOT EXISTS idx_license_mode      ON license (license_mode);

-- ----------------------------------------------------------------------------
-- 3. instance — 在线实例
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS instance (
    id                  BIGSERIAL PRIMARY KEY,
    instance_id         VARCHAR(64)  NOT NULL UNIQUE,       -- 客户端唯一实例标识
    license_id          BIGINT       NOT NULL REFERENCES license(id),
    client_uuid         VARCHAR(64),
    proname             VARCHAR(100),
    product_type        VARCHAR(100),
    product_version     VARCHAR(100),
    product_spec        VARCHAR(100),
    hostname            VARCHAR(200),
    ip_address          VARCHAR(50),
    mac                 VARCHAR(100),
    machine_type        VARCHAR(50),
    current_cpus        INTEGER,
    current_memory      INTEGER,
    extended_attributes TEXT,                               -- 扩展属性 JSON
    status              VARCHAR(20) NOT NULL DEFAULT 'ONLINE',  -- ONLINE / OFFLINE
    online_time         TIMESTAMP,
    last_heartbeat_time TIMESTAMP,
    offline_time        TIMESTAMP,
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_instance_license_id   ON instance (license_id);
CREATE INDEX IF NOT EXISTS idx_instance_status       ON instance (status);
CREATE INDEX IF NOT EXISTS idx_instance_last_hb      ON instance (last_heartbeat_time);

-- ----------------------------------------------------------------------------
-- 4. history_instance — 历史实例（归档）
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS history_instance (
    id                  BIGSERIAL PRIMARY KEY,
    instance_id         VARCHAR(64)  NOT NULL UNIQUE,
    license_id          BIGINT,
    client_uuid         VARCHAR(64),
    proname             VARCHAR(100),
    product_type        VARCHAR(100),
    product_version     VARCHAR(100),
    product_spec        VARCHAR(100),
    hostname            VARCHAR(200),
    ip_address          VARCHAR(50),
    mac                 VARCHAR(100),
    machine_type        VARCHAR(50),
    current_cpus        INTEGER,
    current_memory      INTEGER,
    extended_attributes TEXT,
    status              VARCHAR(20) NOT NULL DEFAULT 'OFFLINE',
    online_time         TIMESTAMP,
    last_heartbeat_time TIMESTAMP,
    offline_time        TIMESTAMP,
    archived_time       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_history_license_id   ON history_instance (license_id);
CREATE INDEX IF NOT EXISTS idx_history_offline_time ON history_instance (offline_time);

-- ----------------------------------------------------------------------------
-- 5. audit_log — 审计日志
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS audit_log (
    log_id          BIGSERIAL PRIMARY KEY,
    operation_type  VARCHAR(50)  NOT NULL,                  -- 操作类型（13 种审计事件）
    operation_desc  VARCHAR(500),
    operator        VARCHAR(100),
    operator_ip     VARCHAR(50),
    target_id       VARCHAR(200),
    result          VARCHAR(20)  NOT NULL,                  -- SUCCESS / FAILURE / WARNING
    detail          TEXT,                                   -- 详细 JSON
    operate_time    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_audit_operate_time ON audit_log (operate_time);
CREATE INDEX IF NOT EXISTS idx_audit_operation_type ON audit_log (operation_type);

-- ----------------------------------------------------------------------------
-- 6. system_config — 系统配置（8 项）
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS system_config (
    id           BIGSERIAL PRIMARY KEY,
    config_key   VARCHAR(100) NOT NULL UNIQUE,
    config_value VARCHAR(500) NOT NULL,
    config_desc  VARCHAR(500),
    update_time  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------------------
-- Seed data
-- ----------------------------------------------------------------------------
-- 初始管理员账号（O4 裁决）：admin / Admin@123456（BCrypt 哈希）
-- 审计记录初始化事件由服务端启动初始化（req-3）写入。
-- 使用 INSERT ... SELECT ... WHERE NOT EXISTS 以兼容 PostgreSQL 与 H2（测试内存库）。
INSERT INTO admin_user (username, password_hash, status)
SELECT 'admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM admin_user WHERE username = 'admin');

-- 8 项系统配置（4.10.1.1）
INSERT INTO system_config (config_key, config_value, config_desc)
SELECT * FROM (VALUES
    ('heartbeat.interval',        '30',  '心跳间隔（秒），范围 10~86400'),
    ('heartbeat.timeout.count',   '3',   '超时倍数（心跳间隔 × 此倍数为超时阈值），范围 2~10'),
    ('archive.after.days',        '30',  '下线实例超过此天数后归档到历史表'),
    ('history.delete.after.days', '90',  '历史记录超过此天数后永久删除'),
    ('archive.cron',              '0 22 10 * * ?', '实例归档执行时间（Cron 表达式）'),
    ('expire.check.cron',         '0 0 0 * * ?',   '授权过期检查时间（Cron 表达式）'),
    ('login.token.ttl.minutes',   '120', '登录令牌有效期（分钟）'),
    ('elastic.quota.warning.ratio','2',  '弹性配额警告阈值倍数（max < used <= max*ratio 警告）')
) AS seed(config_key, config_value, config_desc)
WHERE NOT EXISTS (SELECT 1 FROM system_config WHERE config_key = seed.config_key);