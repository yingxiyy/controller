package net.flex.dci.otn.controller.user.configuration;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.Properties;
import net.flex.dci.otn.db.jpa.dialect.MySQL5InnoDBDialectUtf8mb4;
import net.flex.dci.otn.db.jpa.configuration.DciDatabaseConfiguration;
import net.flex.dci.otn.db.jpa.properties.DciDruidProperties;
import net.flex.dci.otn.db.jpa.properties.JpaProperties;
import net.flex.dci.otn.db.jpa.service.dao.impl.PermissionDaoServiceImpl;
import net.flex.dci.otn.db.jpa.service.dao.impl.RoleDaoServiceImpl;
import net.flex.dci.otn.db.jpa.service.dao.impl.RolePermissionDaoServiceImpl;
import net.flex.dci.otn.db.jpa.service.dao.impl.UserDaoServiceImpl;
import net.flex.dci.otn.db.jpa.service.dao.impl.UserRoleDaoServiceImpl;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

/** Limits this application to security persistence without changing shared database defaults. */
@Configuration
@EnableJpaRepositories(basePackages = "net.flex.dci.otn.db.jpa.repository.security",
        entityManagerFactoryRef = "dciEntityManagerFactory",
        transactionManagerRef = "dciTransactionManager")
@EnableConfigurationProperties(DciDruidProperties.class)
@Import({JpaProperties.class, UserDaoServiceImpl.class, RoleDaoServiceImpl.class,
        PermissionDaoServiceImpl.class, UserRoleDaoServiceImpl.class, RolePermissionDaoServiceImpl.class})
public class UserDatabaseConfiguration {

    // Use composition: inheriting/importing the shared configuration would restore its broad scan.
    private final DciDatabaseConfiguration delegate = new DciDatabaseConfiguration();

    @Bean(name = "dciDataSource")
    public DataSource dataSource(DciDruidProperties properties) {
        return delegate.dataSource(properties);
    }

    @Primary
    @Bean(name = "dciEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            @Qualifier("dciDataSource") DataSource dataSource, JpaProperties jpa) {
        // Do not call the shared factory method: it eagerly initializes all entities.
        // Mirror its persistence settings, but let Spring initialize this scoped factory once.
        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setPackagesToScan("net.flex.dci.otn.db.jpa.entity.security");
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setDataSource(dataSource);
        factory.setPersistenceUnitName("dci");
        Properties properties = new Properties();
        properties.setProperty("hibernate.dialect", MySQL5InnoDBDialectUtf8mb4.class.getName());
        properties.setProperty("hibernate.hbm2ddl.auto", jpa.getHibernate().getDdlAuto());
        properties.setProperty("hibernate.show_sql", "false");
        properties.setProperty("format_sql", "true");
        properties.setProperty("open_in_view", "false");
        factory.setJpaProperties(properties);
        return factory;
    }

    @Primary
    @Bean(name = "dciTransactionManager")
    public PlatformTransactionManager transactionManager(
            @Qualifier("dciEntityManagerFactory") EntityManagerFactory emf,
            @Qualifier("dciDataSource") DataSource dataSource) {
        return delegate.transactionManager(emf, dataSource);
    }

    // Preserve optional Druid monitoring behavior from the shared configuration.
    @Bean
    @ConditionalOnProperty(prefix = "spring.datasource.druid.stat-view-servlet", name = "enabled",
            havingValue = "true", matchIfMissing = false)
    public ServletRegistrationBean<?> druidStatViewServlet(DciDruidProperties properties) {
        return delegate.druidStatViewServlet(properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "spring.datasource.druid.web-stat-filter", name = "enabled",
            havingValue = "true", matchIfMissing = false)
    public FilterRegistrationBean<?> druidWebStatFilter(DciDruidProperties properties) {
        return delegate.druidWebStatFilter(properties);
    }
}
