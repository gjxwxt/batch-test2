package com.example.app.license;

import com.example.app.exception.LicenseValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ServerSelfCheck 启动自检组件测试。
 * 验证默认授权文件通过自检，缺失文件时终止启动。
 */
class ServerSelfCheckTest {

    private final ResourceLoader resourceLoader = new DefaultResourceLoader();

    @Test
    @DisplayName("默认授权文件通过启动自检")
    void shouldPassSelfCheckWithDefaultLicense() {
        ServerSelfCheck selfCheck = new ServerSelfCheck(
                resourceLoader,
                "classpath:test_license/auth-center-local-license.infor",
                "Server",
                "AS",
                "");

        assertThatCode(selfCheck::runSelfCheck).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("授权文件缺失时启动自检失败")
    void shouldFailWhenLicenseFileMissing() {
        ServerSelfCheck selfCheck = new ServerSelfCheck(
                resourceLoader,
                "classpath:test_license/nonexistent.infor",
                "Server",
                "AS",
                "");

        assertThatThrownBy(selfCheck::runSelfCheck)
                .isInstanceOf(LicenseValidationException.class)
                .hasMessageContaining("不存在");
    }

    @Test
    @DisplayName("组件标识不匹配时启动自检失败")
    void shouldFailWhenComponentMismatch() {
        ServerSelfCheck selfCheck = new ServerSelfCheck(
                resourceLoader,
                "classpath:test_license/auth-center-local-license.infor",
                "WrongComponent",
                "AS",
                "");

        assertThatThrownBy(selfCheck::runSelfCheck)
                .isInstanceOf(LicenseValidationException.class)
                .hasMessageContaining("组件");
    }
}