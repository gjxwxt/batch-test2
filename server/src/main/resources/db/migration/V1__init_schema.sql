-- =====================================================================
-- V1__init_schema.sql
-- 共享契约 infra:ddl —— IAS 授权中心初始 Schema（Flyway / PostgreSQL 12+）
-- 表：admin_user, license, instance, history_instance, audit_log, system_config
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. 管理员账号表
-- ---------------------------------------------------------------------
CREATE TABLE admin_user (
    id               BIGSERIAL PRIMARY KEY,
    username         VARCHAR(64)  NOT NULL,
    password_hash    VARCHAR(128) NOT NULL,
    status           SMALLINT     NOT NULL DEFAULT 1,
    last_login_time  TIMESTAMPTZ,
    last_login_ip    VARCHAR(64),
    create_time      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    update_time      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

COMMENT ON TABLE  admin_user IS '管理员账号';
COMMENT ON COLUMN admin_user.status IS '1=启用 0=禁用';

-- 初始管理员账号：admin / Admin@123456（BCrypt 哈希，O4）
INSERT INTO admin_user (username, password_hash, status)
VALUES ('admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 1);

-- ---------------------------------------------------------------------
-- 2. 授权表
-- ---------------------------------------------------------------------
CREATE TABLE license (
    id                  BIGSERIAL PRIMARY KEY,
    serial              VARCHAR(128) NOT NULL,
    license_name        VARCHAR(128),
    proname             VARCHAR(128),
    component           VARCHAR(128),
    version             VARCHAR(64),
    licensee            VARCHAR(128),
    license_mode        VARCHAR(32),
    formal              BOOLEAN      NOT NULL DEFAULT FALSE,
    expiration          TIMESTAMPTZ,
    userinfor           VARCHAR(512),
    max_instances       INTEGER      NOT NULL DEFAULT 0,
    max_cpus            INTEGER      NOT NULL DEFAULT 0,
    max_memory          BIGINT       NOT NULL DEFAULT 0,
    used_instances      INTEGER      NOT NULL DEFAULT 0,
    remaining_instances INTEGER      NOT NULL DEFAULT 0,
    used_cpus           INTEGER      NOT NULL DEFAULT 0,
    used_memory         BIGINT       NOT NULL DEFAULT 0,
    bxb_file            TEXT,
    status              SMALLINT     NOT NULL DEFAULT 1,
    source              VARCHAR(32),
    create_time         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    update_time         TIMESTAMPTZ  NOT NULL DEFAULT now()
);

COMMENT ON TABLE  license IS '授权';
COMMENT ON COLUMN license.serial IS '授权序列号（唯一）';
COMMENT ON COLUMN license.status IS '1=启用 0=禁用';

CREATE UNIQUE INDEX uk_license_serial ON license (serial);

-- ---------------------------------------------------------------------
-- 3. 在线实例表
-- ---------------------------------------------------------------------
CREATE TABLE instance (
    id                    BIGSERIAL PRIMARY KEY,
    instance_id           VARCHAR(128) NOT NULL,
    license_id            BIGINT       NOT NULL,
    client_uuid           VARCHAR(128),
    proname               VARCHAR(128),
    product_type          VARCHAR(64),
    product_version       VARCHAR(64),
    product_spec          VARCHAR(128),
    hostname              VARCHAR(128),
    ip_address            VARCHAR(64),
    mac                   VARCHAR(64),
    machine_type          VARCHAR(64),
    current_cpus          INTEGER      NOT NULL DEFAULT 0,
    current_memory        BIGINT       NOT NULL DEFAULT 0,
    extended_attributes   JSONB,
    status                SMALLINT     NOT NULL DEFAULT 1,
    online_time           TIMESTAMPTZ,
    last_heartbeat_time   TIMESTAMPTZ,
    offline_time          TIMESTAMPTZ,
    create_time           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    update_time           TIMESTAMPTZ  NOT NULL DEFAULT now()
);

COMMENT ON TABLE  instance IS '在线实例';
COMMENT ON COLUMN instance.status IS '1=在线 0=离线';

CREATE UNIQUE INDEX uk_instance_instance_id ON instance (instance_id);
CREATE INDEX idx_instance_license_id ON instance (license_id);

-- ---------------------------------------------------------------------
-- 4. 历史实例表（归档）
-- ---------------------------------------------------------------------
CREATE TABLE history_instance (
    id                    BIGSERIAL PRIMARY KEY,
    instance_id           VARCHAR(128) NOT NULL,
    license_id            BIGINT       NOT NULL,
    client_uuid           VARCHAR(128),
    proname               VARCHAR(128),
    product_type          VARCHAR(64),
    product_version       VARCHAR(64),
    product_spec          VARCHAR(128),
    hostname              VARCHAR(128),
    ip_address            VARCHAR(64),
    mac                   VARCHAR(64),
    machine_type          VARCHAR(64),
    current_cpus          INTEGER      NOT NULL DEFAULT 0,
    current_memory        BIGINT       NOT NULL DEFAULT 0,
    extended_attributes   JSONB,
    status                SMALLINT     NOT NULL DEFAULT 0,
    online_time           TIMESTAMPTZ,
    last_heartbeat_time   TIMESTAMPTZ,
    offline_time          TIMESTAMPTZ,
    archived_time         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    create_time           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    update_time           TIMESTAMPTZ  NOT NULL DEFAULT now()
);

COMMENT ON TABLE  history_instance IS '历史实例（归档）';
COMMENT ON COLUMN history_instance.status IS '0=离线归档';

CREATE INDEX idx_history_instance_instance_id ON history_instance (instance_id);
CREATE INDEX idx_history_instance_license_id ON history_instance (license_id);

-- ---------------------------------------------------------------------
-- 5. 审计日志表
-- ---------------------------------------------------------------------
CREATE TABLE audit_log (
    log_id          BIGSERIAL PRIMARY KEY,
    operation_type  VARCHAR(64)  NOT NULL,
    operation_desc  VARCHAR(512),
    operator        VARCHAR(128),
    operator_ip     VARCHAR(64),
    target_id       VARCHAR(128),
    result          SMALLINT     NOT NULL DEFAULT 1,
    detail          TEXT,
    operate_time    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

COMMENT ON TABLE  audit_log IS '审计日志';
COMMENT ON COLUMN audit_log.result IS '1=成功 0=失败';

CREATE INDEX idx_audit_log_operate_time ON audit_log (operate_time);

-- ---------------------------------------------------------------------
-- 6. 系统配置表（8 项默认配置）
-- ---------------------------------------------------------------------
CREATE TABLE system_config (
    id            BIGSERIAL PRIMARY KEY,
    config_key    VARCHAR(128) NOT NULL,
    config_value  VARCHAR(512),
    config_desc   VARCHAR(512),
    update_time   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

COMMENT ON TABLE system_config IS '系统配置';

CREATE UNIQUE INDEX uk_system_config_key ON system_config (config_key);

-- 8 项默认配置：心跳 30s / 超时倍数 3 / 归档 30 天 / 历史删除 90 天 / Cron / 令牌 TTL / 弹性配额倍数 2
INSERT INTO system_config (config_key, config_value, config_desc) VALUES
    ('heartbeat.interval.seconds', '30',  '心跳上报间隔（秒）'),
    ('heartbeat.timeout.multiplier', '3', '心跳超时判定倍数'),
    ('archive.retention.days', '30',      '在线实例归档保留天数'),
    ('history.retention.days', '90',      '历史实例删除保留天数'),
    ('archive.cron', '0 0 2 * * *',       '归档任务 Cron 表达式'),
    ('token.ttl.seconds', '7200',         '令牌有效期（秒）'),
    ('elastic.quota.multiplier', '2',     '弹性配额倍数'),
    ('license.verify.interval.seconds', '300', '授权校验间隔（秒）');