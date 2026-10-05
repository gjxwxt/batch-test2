package com.example.app.model;

import java.time.Instant;

/**
 * 在线实例（instance）领域模型。
 *
 * <p>对应共享契约 6 表 DDL 中的 {@code instance} 表（infra:ddl）。
 * 由客户端注册（req-14）创建、心跳（req-16）保活、超时检测（req-18）下线。</p>
 *
 * @param id                  主键
 * @param instanceId          客户端唯一实例标识
 * @param licenseId           关联授权主键
 * @param clientUuid          客户端 UUID
 * @param proname             产品标识
 * @param productType         产品类型
 * @param productVersion      产品版本
 * @param productSpec         产品规格
 * @param hostname            主机名
 * @param ipAddress           IP 地址
 * @param mac                 MAC 地址
 * @param machineType         机器类型
 * @param currentCpus         当前 CPU 数
 * @param currentMemory       当前内存 MB
 * @param extendedAttributes  扩展属性 JSON
 * @param status              状态 ONLINE / OFFLINE
 * @param onlineTime          上线时间
 * @param lastHeartbeatTime   最近心跳时间
 * @param offlineTime         下线时间
 * @param createTime          创建时间
 * @param updateTime          更新时间
 */
public record Instance(
        Long id,
        String instanceId,
        Long licenseId,
        String clientUuid,
        String proname,
        String productType,
        String productVersion,
        String productSpec,
        String hostname,
        String ipAddress,
        String mac,
        String machineType,
        Integer currentCpus,
        Integer currentMemory,
        String extendedAttributes,
        String status,
        Instant onlineTime,
        Instant lastHeartbeatTime,
        Instant offlineTime,
        Instant createTime,
        Instant updateTime
) {

    public static final String STATUS_ONLINE = "ONLINE";
    public static final String STATUS_OFFLINE = "OFFLINE";

    public boolean isOnline() {
        return STATUS_ONLINE.equals(status);
    }
}