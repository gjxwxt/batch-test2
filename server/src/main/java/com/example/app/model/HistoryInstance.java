package com.example.app.model;

import java.time.Instant;

/**
 * 历史实例（history_instance）领域模型。
 *
 * <p>对应共享契约 6 表 DDL 中的 {@code history_instance} 表（infra:ddl）。
 * 由归档调度（req-19）将下线超过 {@code archive.after.days} 天的实例从 {@code instance} 迁移而来；
 * 超过 {@code history.delete.after.days} 天后被永久删除（req-20）。</p>
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
 * @param status              状态（归档后固定 OFFLINE）
 * @param onlineTime          上线时间
 * @param lastHeartbeatTime   最近心跳时间
 * @param offlineTime         下线时间
 * @param archivedTime        归档时间
 * @param createTime          创建时间
 * @param updateTime          更新时间
 */
public record HistoryInstance(
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
        Instant archivedTime,
        Instant createTime,
        Instant updateTime
) {

    public static final String STATUS_OFFLINE = "OFFLINE";

    /**
     * 从在线实例构造历史实例（归档迁移，req-19）。
     */
    public static HistoryInstance from(Instance instance, Instant archivedTime) {
        return new HistoryInstance(
                null,
                instance.instanceId(),
                instance.licenseId(),
                instance.clientUuid(),
                instance.proname(),
                instance.productType(),
                instance.productVersion(),
                instance.productSpec(),
                instance.hostname(),
                instance.ipAddress(),
                instance.mac(),
                instance.machineType(),
                instance.currentCpus(),
                instance.currentMemory(),
                instance.extendedAttributes(),
                STATUS_OFFLINE,
                instance.onlineTime(),
                instance.lastHeartbeatTime(),
                instance.offlineTime(),
                archivedTime,
                instance.createTime(),
                archivedTime
        );
    }
}