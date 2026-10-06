# IAS Auth Center Client SDK (`sdk/`)

独立 Gradle 模块，产出**可嵌入 JAR**（`ias-auth-sdk-1.0.0.jar`），供客户端应用嵌入以对接
IAS 授权认证中心的客户端侧（无鉴权）接口。

## 构建与测试

```bash
cd sdk
./gradlew test   # 运行单元测试
./gradlew jar    # 产出可嵌入 JAR -> build/libs/ias-auth-sdk-1.0.0.jar
```

- 运行时依赖最小化：仅 `jackson-databind`；HTTP 使用 JDK 内置 `java.net.http.HttpClient`，
  不强制消费方引入 Spring / Web 框架。
- Java 21 toolchain。

## 功能

| 能力 | 说明 | 对应契约 |
|------|------|----------|
| 签名校验 | RSA-2048 + SHA256withRSA，规范字段序 + 盐前缀 `InforSuiteAuth2026_` | req-31 / `signature` |
| 实例注册 | `POST /api/v1/license/register` | req-30 / `IAS_AUTH_REGISTER` |
| 心跳上报 | `POST /api/v1/license/heartbeat` | req-30 / `IAS_AUTH_HEARTBEAT` |
| 公钥获取 | `GET /api/v1/license/public-key` | `IAS_AUTH_PUBLIC_KEY` |
| 心跳配置 | `GET /api/v1/license/heartbeat-config` | `IAS_AUTH_HB_CONFIG` |
| 授权文件申请 | `POST /api/v1/license/file-apply` | `IAS_AUTH_FILE_APPLY` |

## 使用示例

```java
SdkClient client = SdkClient.create("http://localhost:8080");

// 1. 获取签发公钥并校验 license 签名
PublicKeyResponse key = client.fetchPublicKey();
LicenseInfo license = LicenseInfo.fromMap(parsedLicenseFields);
boolean valid = LicenseSignatureVerifier.verify(license, key.publicKeyBase64());

// 2. 注册实例
RegisterResponse reg = client.register(RegisterRequest.builder()
        .clientUuid(uuid)
        .proname("InforSuite")
        .hostname(hostname)
        .build());

// 3. 周期心跳
HeartbeatResponse hb = client.heartbeat(HeartbeatRequest.builder()
        .instanceId(reg.instanceId())
        .clientUuid(uuid)
        .currentCpus(4)
        .build());
```

## 包结构

- `com.example.sdk` — 入口 `SdkClient`
- `com.example.sdk.model` — 不可变 DTO（Java 21 `record`）
- `com.example.sdk.crypto` — `LicenseSignatureVerifier` 签名校验
- `com.example.sdk.http` — JDK HTTP 传输与 JSON 编解码
- `com.example.sdk.exception` — `SdkException` + 共享错误码 `SdkErrorCode`