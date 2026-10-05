package com.example.app.scheduler;

import com.example.app.service.InstanceAdminService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 实例生命周期调度器（wp-5 超时/归档调度）。
 *
 * <p>周期执行：</p>
 * <ul>
 *   <li>心跳超时检测与下线（req-18）——按固定间隔高频执行；</li>
 *   <li>实例归档（req-19）与历史实例删除（req-20）——按配置 Cron 低频执行。</li>
 * </ul>
 *
 * <p>阈值取自 {@code system_config}（心跳间隔/超时倍数/归档天数/历史删除天数）。
 * 调度周期为可配置默认值，便于测试与运维调整。</p>
 */
@Component
@EnableScheduling
public class InstanceLifecycleScheduler {

    private static final Logger log = LoggerFactory.getLogger(InstanceLifecycleScheduler.class);

    private final InstanceAdminService instanceAdminService;

    public InstanceLifecycleScheduler(InstanceAdminService instanceAdminService) {
        this.instanceAdminService = instanceAdminService;
    }

    /**
     * 心跳超时检测（req-18）。默认每 30 秒执行一次。
     */
    @Scheduled(fixedDelayString = "${instance.timeout.detect.interval.ms:30000}")
    public void detectTimeouts() {
        int marked = instanceAdminService.detectTimeouts();
        if (marked > 0) {
            log.info("心跳超时检测：{} 个实例下线", marked);
        }
    }

    /**
     * 实例归档 + 历史清理（req-19 / req-20）。默认每 10 分钟执行一次。
     */
    @Scheduled(fixedDelayString = "${instance.archive.interval.ms:600000}")
    public void archiveAndPurge() {
        int archived = instanceAdminService.archiveOfflineInstances();
        int purged = instanceAdminService.deleteExpiredHistory();
        if (archived > 0 || purged > 0) {
            log.info("实例归档：{} 条；历史清理：{} 条", archived, purged);
        }
    }
}