package com.example.app.controller;

import com.example.app.model.HistoryInstance;
import com.example.app.model.Instance;
import com.example.app.service.InstanceAdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 实例监控控制器（wp-5 实例生命周期与监控）。
 *
 * <p>路径冻结（6.2 映射）：/api/v1/admin/instances/**（JWT 鉴权）。覆盖：</p>
 * <ul>
 *   <li>GET /instances      — 在线实例查询（req-21 / IAS_AUTH_INST_LIST）</li>
 *   <li>GET /instances/offline — 下线实例查询（req-22 / IAS_AUTH_INST_OFFLINE）</li>
 *   <li>GET /instances/history — 历史实例查询（IAS_AUTH_INST_HISTORY）</li>
 *   <li>GET /instances/{id}  — 实例详情查看（req-23 / IAS_AUTH_INST_DETAIL）</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/admin/instances")
public class InstanceAdminController {

    private final InstanceAdminService instanceAdminService;

    public InstanceAdminController(InstanceAdminService instanceAdminService) {
        this.instanceAdminService = instanceAdminService;
    }

    @GetMapping
    public ResponseEntity<List<Instance>> listOnlineInstances() {
        return ResponseEntity.ok(instanceAdminService.listOnlineInstances());
    }

    @GetMapping("/offline")
    public ResponseEntity<List<Instance>> listOfflineInstances() {
        return ResponseEntity.ok(instanceAdminService.listOfflineInstances());
    }

    @GetMapping("/history")
    public ResponseEntity<List<HistoryInstance>> listHistoryInstances() {
        return ResponseEntity.ok(instanceAdminService.listHistoryInstances());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Instance> getInstance(@PathVariable Long id) {
        return ResponseEntity.ok(instanceAdminService.getInstance(id));
    }
}