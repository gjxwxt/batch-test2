package com.example.app.license;

import java.util.List;
import java.util.Map;

/**
 * 三层防篡改校验的增强分析结果（AUTH-016/017/018/019）。
 *
 * <p>在基础三层校验（SIGNATURE / DB_STATE / FILE_INTEGRITY）之上，额外携带
 * 供服务层执行「自动修复 / 禁用 / 警告」所需的结构化信息：</p>
 * <ul>
 *   <li><b>signatureValid</b> — 数字签名是否有效（AUTH-017 签名破坏判定）。</li>
 *   <li><b>dbStateValid</b> — 数据库状态是否正常（ACTIVE 且未过期）。</li>
 *   <li><b>parsedFields</b> — 授权文件解析出的原始字段（用于 AUTH-016 字段修复）。</li>
 *   <li><b>fieldMismatches</b> — 数据库与文件不一致的字段名列表（AUTH-016）。</li>
 *   <li><b>quotaConservationBroken</b> — 配额守恒是否被破坏（used+remaining≠max，AUTH-018）。</li>
 *   <li><b>onlineInstanceCount</b> — 该授权下实际在线实例数（AUTH-019 交叉验证）。</li>
 * </ul>
 */
public record TamperCheckResult(
        boolean signatureValid,
        boolean dbStateValid,
        Map<String, String> parsedFields,
        List<String> fieldMismatches,
        boolean quotaConservationBroken,
        long onlineInstanceCount
) {

    /** 三层是否全部通过（不含 AUTH-019 警告，警告不阻断 passed）。 */
    public boolean allLayersPassed() {
        return signatureValid && dbStateValid && fieldMismatches.isEmpty() && !quotaConservationBroken;
    }
}