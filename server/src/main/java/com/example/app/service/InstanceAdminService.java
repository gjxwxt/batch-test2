package com.example.app.service;

import com.example.app.model.HistoryInstance;
import com.example.app.model.Instance;

import java.util.List;

/**
 * 实例生命周期与监控服务（wp-5）。
 *
 * <p>覆盖需求锚点：</p>
 * <ul>
 *   <li>req-18 心跳超时检测与下线（IAS_AUTH_TIMEOUT）</li>
 *   <li>req-19 实例归档（IAS_AUTH_ARCHIVE）</li>
 *   <li>req-20 历史实例删除（IAS_AUTH_HISTORY_DELETE）</li>
 *   <li>req-21 在线实例查询（IAS_AUTH_INST_LIST）</li>
 *   <li>req-22 下线实例查询（IAS_AUTH_INST_OFFLINE）</li>
 *   <li>req-23 实例详情查看（IAS_AUTH_INST_DETAIL）</li>
 *   <li>历史实例查询（IAS_AUTH_INST_HISTORY）</li>
 * </ul>
 */
public interface InstanceAdminService {

    /** 在线实例查询（req-21 / IAS_AUTH_INST_LIST）。 */
    List<Instance> listOnlineInstances();

    /** 下线实例查询（req-22 / IAS_AUTH_INST_OFFLINE）。 */
    List<Instance> listOfflineInstances();

    /** 历史实例查询（IAS_AUTH_INST_HISTORY）。 */
    List<HistoryInstance> listHistoryInstances();

    /** 实例详情查看（req-23 / IAS_AUTH_INST_DETAIL）。不存在抛 INSTANCE_002。 */
    Instance getInstance(Long id);

    /**
     * 心跳超时检测与下线（req-18 / IAS_AUTH_TIMEOUT）。
     *
     * <p>将最近心跳时间早于 {@code heartbeat.interval × heartbeat.timeout.count} 秒的
     * ONLINE 实例标记为 OFFLINE 并记录下线时间。返回本次下线实例数。</p>
     */
    int detectTimeouts();

    /**
     * 实例归档（req-19 / IAS_AUTH_ARCHIVE）。
     *
     * <p>将下线时间早于 {@code archive.after.days} 天的 OFFLINE 实例迁移到历史表。
     * 返回本次归档实例数。</p>
     */
    int archiveOfflineInstances();

    /**
     * 历史实例删除（req-20 / IAS_AUTH_HISTORY_DELETE）。
     *
     * <p>将归档时间早于 {@code history.delete.after.days} 天的历史实例永久删除。
     * 返回本次删除条数。</p>
     */
    int deleteExpiredHistory();
}