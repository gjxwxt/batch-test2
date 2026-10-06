package com.example.app.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 授权表（license）MyBatis-Plus Mapper。
 *
 * <p>提供 req-28 多节点配额强一致所需的原子 SQL/CAS 操作：单条 {@code UPDATE}
 * 在数据库（PostgreSQL 12+ / H2 PostgreSQL 兼容模式）中原子执行，将「配额上限校验 +
 * used_instances 递增 + remaining_instances 递减」合并为一次原子操作，杜绝多节点
 * 并发注册时 read-modify-write 竞态导致的配额超卖（O7：多节点共享同一库）。</p>
 *
 * <p>弹性配额（req-27）：允许实例数超过基础配额，上限为 {@code max_instances * elasticMultiplier}。
 * 当 {@code max_instances <= 0} 表示不限制实例数，恒可占用。</p>
 */
@Mapper
public interface LicenseMapper {

    /**
     * 原子占用一个实例配额（req-28 多节点一致性）。
     *
     * <p>WHERE 子句保证「配额校验 + 计数递增」不拆分：仅当配额未满时更新成功（返回 1），
     * 否则不更新（返回 0）。在 PostgreSQL 中该 UPDATE 以行级锁原子执行。</p>
     *
     * @param licenseId         授权主键
     * @param elasticMultiplier 弹性配额倍数（来自 system_config.elastic.quota.multiplier）
     * @return 占用成功返回 1；配额已满返回 0
     */
    @Select("SELECT id FROM license WHERE serial = #{serial}")
    Long selectIdBySerial(@Param("serial") String serial);

    @Update("""
            UPDATE license
            SET used_instances = used_instances + 1,
                remaining_instances = GREATEST(0, remaining_instances - 1),
                update_time = now()
            WHERE id = #{licenseId}
              AND (max_instances <= 0 OR used_instances < max_instances * #{elasticMultiplier})
            """)
    int tryAcquireInstanceQuotaAtomic(@Param("licenseId") Long licenseId,
                                      @Param("elasticMultiplier") int elasticMultiplier);

    /**
     * 原子占用 CPU/内存配额（AUTH-030/031）。
     *
     * <p>在实例配额占用成功后调用，将 used_cpus / used_memory 递增。单条 UPDATE 原子执行。</p>
     *
     * @param licenseId 授权主键
     * @param cpus      本次占用的 CPU 数（可空）
     * @param memory    本次占用的内存 MB（可空）
     * @return 更新成功返回 1
     */
    @Update("""
            UPDATE license
            SET used_cpus = used_cpus + #{cpus},
                used_memory = used_memory + #{memory},
                update_time = now()
            WHERE id = #{licenseId}
            """)
    int acquireCpuMemoryQuotaAtomic(@Param("licenseId") Long licenseId,
                                    @Param("cpus") int cpus,
                                    @Param("memory") int memory);
}