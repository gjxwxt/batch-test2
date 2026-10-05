package com.example.app.controller;

import com.example.app.exception.ApiErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 共享契约 · API 骨架（infra:api-skeleton）— 实例监控端点。
 *
 * <p>路径冻结（6.2 映射）：/api/v1/admin/instances/**。业务逻辑由并行工作包 wp-5（实例生命周期与监控）实现。</p>
 */
@RestController
@RequestMapping("/api/v1/admin/instances")
public class InstanceAdminController {

    @GetMapping
    public ResponseEntity<ApiErrorResponse> listInstances() {
        // TODO(wp-5): IAS_AUTH_INST_LIST — 在线实例查询
        throw new UnsupportedOperationException("wp-5: IAS_AUTH_INST_LIST not yet implemented");
    }

    @GetMapping("/offline")
    public ResponseEntity<ApiErrorResponse> listOfflineInstances() {
        // TODO(wp-5): IAS_AUTH_INST_OFFLINE — 下线实例查询
        throw new UnsupportedOperationException("wp-5: IAS_AUTH_INST_OFFLINE not yet implemented");
    }

    @GetMapping("/history")
    public ResponseEntity<ApiErrorResponse> listHistoryInstances() {
        // TODO(wp-5): IAS_AUTH_INST_HISTORY — 历史实例查询
        throw new UnsupportedOperationException("wp-5: IAS_AUTH_INST_HISTORY not yet implemented");
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiErrorResponse> getInstance(@PathVariable Long id) {
        // TODO(wp-5): IAS_AUTH_INST_DETAIL — 实例详情查看
        throw new UnsupportedOperationException("wp-5: IAS_AUTH_INST_DETAIL not yet implemented");
    }
}