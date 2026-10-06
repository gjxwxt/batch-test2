# 签名规范契约 (Signature Canonical Contract)

> 共享契约条目：`infra:signature`
> 承载基线 commit：`448ca97f482f6593bac1321a3f7827027c0921a4`
> 分支：`feature/t-20261004-fu91es`

本文档冻结 IAS 授权中心的授权文件签名规范。所有涉及授权签发、校验、注册与心跳的工作包必须严格遵循。

## 1. 算法

- **非对称加密**：RSA-2048
- **签名算法**：SHA256withRSA
- **编码**：签名结果 Base64 编码

## 2. 双密钥对（O6）

签发密钥对与通信密钥对**互不复用**：

- **签发密钥对**：用于签发 license 文件（服务端私钥签名，客户端公钥验签）。
- **通信密钥对**：用于注册 / 心跳通信（客户端私钥签名，服务端公钥验签）。

## 3. 盐前缀

- 盐前缀：`InforSuiteAuth2026_`

## 4. 规范化字段顺序 (Canonical Field Order)

签名前，将以下字段按给定顺序拼接为规范化字符串（字段间以 `&` 连接，`key=value` 形式，空值省略）：

```text
component
version
licensee
mode
formal
expiration
userinfor
proname
serial
center-required
max-instances
max-cpus
max-memory
```

即规范化串形如：

```text
component=<component>&version=<version>&licensee=<licensee>&mode=<mode>&formal=<formal>&expiration=<expiration>&userinfor=<userinfor>&proname=<proname>&serial=<serial>&center-required=<center-required>&max-instances=<max-instances>&max-cpus=<max-cpus>&max-memory=<max-memory>
```

## 5. 校验流程

1. 按上述字段顺序构造规范化字符串。
2. 使用对应公钥对签名值执行 `SHA256withRSA` 验签。
3. 验签失败返回 `LICENSE_004`（授权签名校验失败）。

## 6. 承载载体

- 可执行载体位于 `server/`。
- 契约文档位于 `contract/`。