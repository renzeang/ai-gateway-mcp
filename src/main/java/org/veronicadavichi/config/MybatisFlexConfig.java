package org.veronicadavichi.config;

import com.mybatisflex.spring.FlexSqlSessionFactoryBean;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;

/**
 * MyBatis-Flex 配置类 - 不使用自定义 Configuration
 */
@Configuration
public class MybatisFlexConfig {

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        FlexSqlSessionFactoryBean factoryBean = new FlexSqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        // 不使用 setConfiguration，让 MyBatis-Flex 使用默认配置
        return factoryBean.getObject();
    }

    @Bean
    public SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    @Bean
    public DataSourceTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
