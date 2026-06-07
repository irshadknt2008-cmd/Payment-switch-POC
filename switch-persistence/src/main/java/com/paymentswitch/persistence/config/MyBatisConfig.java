package com.paymentswitch.persistence.config;

import com.paymentswitch.persistence.mapper.TransactionMapper;
import com.paymentswitch.persistence.mapper.BinTableMapper;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

import javax.sql.DataSource;

/**
 * Programmatic MyBatis {@link SqlSessionFactory} configuration.
 */
public class MyBatisConfig {

    private final DataSource dataSource;

    public MyBatisConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public SqlSessionFactory build() {
        Environment environment = new Environment("production",
                new JdbcTransactionFactory(), dataSource);

        Configuration config = new Configuration(environment);
        config.setMapUnderscoreToCamelCase(true);
        config.setLazyLoadingEnabled(false);
        config.setDefaultStatementTimeout(30);

        // Register mapper interfaces
        config.addMapper(TransactionMapper.class);
        config.addMapper(BinTableMapper.class);

        return new SqlSessionFactoryBuilder().build(config);
    }
}
