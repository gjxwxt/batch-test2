# IAS Auth Center 授权生成工具 (License Generation Tool)

独立 Gradle 模块，用于生成授权（license）签名。产出可执行 JAR。

## 构建

```bash
cd tools
./gradlew test        # 运行单元测试
./gradlew jar         # 构建可执行 JAR (build/libs/tools-1.0.0.jar)
```

## 使用

```bash
# 1. 生成双 RSA-2048 密钥对（签发密钥对 + 通信密钥对，互不复用 O6）
java -jar build/libs/tools-1.0.0.jar genkey <outDir>

# 2. 签发授权（基于规范字段顺序 + SHA256withRSA）
java -jar build/libs/tools-1.0.0.jar sign <signing-private.pem> \
    <component> <version> <licensee> <mode> <formal> <expiration> \
    <userinfor> <proname> <serial> \
    [center-required] [max-instances] [max-cpus] [max-memory]

# 3. 校验授权签名（有效退出码 0，无效退出码 1）
java -jar build/libs/tools-1.0.0.jar verify <signing-public.pem> <signature> \
    <component> <version> <licensee> <mode> <formal> <expiration> \
    <userinfor> <proname> <serial> \
    [center-required] [max-instances] [max-cpus] [max-memory]
```

## 契约要点

- **算法**：RSA-2048 + SHA256withRSA
- **规范字段顺序**（冻结）：`component, version, licensee, mode, formal,
  expiration, userinfor, proname, serial, center-required, max-instances,
  max-cpus, max-memory`
- **盐前缀**：`InforSuiteAuth2026_`
- **双密钥**：签发密钥对（签 license）与通信密钥对（注册/心跳）互不复用（O6）