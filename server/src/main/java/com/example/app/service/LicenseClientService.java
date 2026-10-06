package com.example.app.service;

import com.example.app.model.FileApplyRequest;
import com.example.app.model.FileApplyResponse;
import com.example.app.model.HeartbeatConfigResponse;
import com.example.app.model.HeartbeatRequest;
import com.example.app.model.HeartbeatResponse;
import com.example.app.model.PublicKeyResponse;
import com.example.app.model.RegisterRequest;
import com.example.app.model.RegisterResponse;

/**
 * 客户端交互服务（wp-4 客户端交互）。
 *
 * <p>承载通信公钥分发、实例注册、心跳参数获取、客户端心跳、重注册、local/site 授权文件申请等业务规则。</p>
 */
public interface LicenseClientService {

    /**
     * 通信公钥分发（req-13 / IAS_AUTH_PUBLIC_KEY）。
     */
    PublicKeyResponse getPublicKey();

    /**
     * 心跳参数获取（req-15 / IAS_AUTH_HB_CONFIG）。
     */
    HeartbeatConfigResponse getHeartbeatConfig();

    /**
     * 实例注册（req-14 / IAS_AUTH_REGISTER）。
     * 已存在实例时执行重注册（req-17 / IAS_AUTH_REREGISTER）。
     */
    RegisterResponse register(RegisterRequest request);

    /**
     * 客户端心跳（req-16 / IAS_AUTH_HEARTBEAT）。
     */
    HeartbeatResponse heartbeat(HeartbeatRequest request);

    /**
     * 授权文件申请（req-37 / IAS_AUTH_FILE_APPLY，local/site 模式）。
     */
    FileApplyResponse fileApply(FileApplyRequest request);
}