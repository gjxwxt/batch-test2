# 共享契约 · 签名 canonical 规范（infra:signature）

> 冻结基线（wp-0）。授权文件签发/验签、注册/心跳请求签名的统一规范。
> 需求来源：`center模式-服务端需求文档(1).md` 4.1.1 / 4.2.1 / 4.5。

## 两套独立 RSA 密钥（O6 裁决）

| 密钥对 | 持有方 | 用途 |
|--------|--------|------|
| **签发密钥对** | 私钥：授权生成工具；公钥：客户端 SDK JAR 硬编码 + 服务端 | 签名/验签 license.infor 授权文件 |
| **通信密钥对** | 私钥：服务端首次启动自动生成存库；公钥：通过接口分发 | 加密注册响应、签名注册/心跳请求与公钥分发响应 |

两套密钥**互不复用**。

## 授权文件签名（license.infor）

- 算法：**RSA-2048 + SHA256withRSA**。
- 签名内容：授权文件中所有关键字段按**固定顺序**拼接为 canonical 字符串，加盐前缀 `InforSuiteAuth2026_`。
- canonical 字段顺序（基础字段 + center 模式追加）：
  `component, version, licensee, mode, formal, expiration, userinfor, proname, serial, center-required, max-instances, max-cpus, max-memory`
- 签名值 Base64 编码存入 XML `<signature>` 节点。
- 任何字段被修改都会导致验签失败。

## 验签公钥加载

- 优先从 JVM 系统属性 `-Dlicense.company.pub.key` 读取。
- 未设置则使用客户端 SDK JAR 包中硬编码的公钥。

## 服务端自检（IAS_AUTH_SELF_CHECK）

顺序执行，任一步失败即终止启动：
1. 文件存在性检查（`license.self.path`，默认 `classpath:test_license/auth-center-local-license.infor`）
2. 数字签名验证（SHA256withRSA）
3. 组件标识匹配（`license.self.expected-component`，默认 `Server`，大小写不敏感）
4. 产品标识匹配（`license.self.expected-proname`，默认 `AS`）
5. 有效期校验（`never` 或日期格式，过期则失败）

## 变更控制

canonical 字段顺序、盐值、算法为冻结契约。任何变更须经 contract_review 评审。