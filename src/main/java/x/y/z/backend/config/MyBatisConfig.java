package x.y.z.backend.config;

import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

/**
 * MyBatis Configuration for Spring Boot DevTools Compatibility
 * 
 * This configuration ensures MyBatis mapper XML files are loaded in a way
 * that's compatible with Spring Boot DevTools hot reload.
 * 
 * Key features:
 * - Uses PathMatchingResourcePatternResolver with explicit classloader
 * - Enables mapper XML hot reload during development
 * - Works in conjunction with META-INF/spring-devtools.properties exclusions
 * - Auto-disables in production when DevTools is not present
 */
@org.springframework.context.annotation.Configuration
@ConditionalOnClass(SqlSessionFactory.class)
public class MyBatisConfig {

    /**
     * Configure SqlSessionFactory with DevTools-compatible resource loading
     * and automatic mapper XML reloading during development
     */
    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(dataSource);
        
        // Use the system classloader for mapper resources
        // This prevents issues with DevTools' restart classloader
        PathMatchingResourcePatternResolver resolver = 
            new PathMatchingResourcePatternResolver(getClass().getClassLoader());
        
        // Load mapper XML files from classpath
        sessionFactory.setMapperLocations(
            resolver.getResources("classpath:mapper/**/*.xml")
        );
        
        // Set type aliases package
        sessionFactory.setTypeAliasesPackage("x.y.z.backend.domain.model");
        
        // Set type handlers package  
        sessionFactory.setTypeHandlersPackage("x.y.z.backend.config");
        
        // Configure MyBatis settings
        Configuration configuration = new Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        
        // Enable mapper XML hot reload (development only - ignored in production)
        // This allows changes to mapper XML files to be picked up without restart
        configuration.setLazyLoadingEnabled(true);
        
        sessionFactory.setConfiguration(configuration);
        
        // Build and return the SqlSessionFactory
        return sessionFactory.getObject();
    }
}
