package edu.campus.loop.product;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 配置 spring.datasource.url 后启用商品持久化。
 * <p>复用交易模块提供的共享 DataSource / JdbcTemplate / 事务管理器，
 * 保证商品行锁、成交更新与交易记录位于同一事务；未配置数据库时商品服务保持骨架可启动。</p>
 */
@Configuration
@ConditionalOnProperty(name = "spring.datasource.url")
public class ProductPersistenceConfiguration {
    @Bean
    @ConditionalOnMissingBean(ProductRepository.class)
    public ProductRepository productRepository(JdbcTemplate jdbcTemplate) {
        return new ProductRepository(jdbcTemplate);
    }
}
