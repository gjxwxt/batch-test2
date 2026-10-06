package com.example.app.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus Mapper 扫描配置。
 *
 * <p>仅在存在 {@link SqlSessionFactory}（即已配置数据源并加载 MyBatis 自动配置）时注册
 * {@code com.example.app.mapper} 下的 Mapper Bean。这样 {@code @WebMvcTest} 切片上下文
 * （不含 MyBatis 自动配置、无 SqlSessionFactory）不会因 Mapper Bean 创建失败而中断，
 * 而完整 {@code @SpringBootTest} / 生产上下文正常注册 Mapper。</p>
 */
@Configuration
@ConditionalOnBean(SqlSessionFactory.class)
@MapperScan("com.example.app.mapper")
public class MyBatisConfig {
}