package devicemaintenance.config;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * MySQL配置属性加载类
 * 
 * 功能：
 * 1. 从 classpath:mysql.properties 和 file:config/mysql.properties 加载配置
 * 2. 将配置映射到 spring.datasource.* 属性
 * 3. 外部文件优先级高于classpath文件（后加载的覆盖先加载的）
 * 
 * 加载顺序：
 * 1. classpath:mysql.properties (JAR包内，默认值)
 * 2. file:${user.dir}/config/mysql.properties (外部配置，生产环境由install.sh生成)
 * 
 * 与其他微服务的一致性：
 * - taskInfo、neMgr等微服务通过 db-mysql 模块的 DciDruidProperties 实现
 * - device-maintenance 使用自定义配置类实现相同功能
 * - 最终效果完全相同：外部 mysql.properties 覆盖默认配置
 */
@Slf4j
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "spring.datasource")
@PropertySource(value = {
        "classpath:mysql.properties",
        "file:${user.dir}/config/mysql.properties"
}, ignoreResourceNotFound = true)
public class MySQLPropertiesConfig {

    /**
     * 数据库连接URL
     * 示例: jdbc:mysql://127.0.0.1:3307/sotn?autoReconnect=true&useUnicode=true&characterEncoding=UTF-8
     */
    private String url;

    /**
     * 数据库用户名
     */
    private String username;

    /**
     * 数据库密码
     */
    private String password;

    /**
     * JDBC驱动类名
     * 默认: com.mysql.cj.jdbc.Driver
     */
    private String driverClassName;

    /**
     * 数据库平台
     * 默认: mysql
     */
    private String platform;

    /**
     * 数据源类型
     * 默认: com.alibaba.druid.pool.DruidDataSource
     */
    private String type;

    // Druid连接池配置
    private Integer initialSize;
    private Integer minIdle;
    private Integer maxActive;
    private Long maxWait;
    private Long timeBetweenEvictionRunsMillis;
    private Long minEvictableIdleTimeMillis;
    private Boolean testWhileIdle;
    private Boolean testOnBorrow;
    private Boolean testOnReturn;
    private Boolean poolPreparedStatements;
    private Integer maxOpenPreparedStatements;
    private Boolean asyncInit;
    private String filters;

    /**
     * 配置加载完成后的回调
     * 用于调试：打印实际加载的配置信息
     */
    public MySQLPropertiesConfig() {
        log.info("MySQL配置加载器已初始化");
    }

    /**
     * 打印加载的配置（用于调试）
     */
    public void logConfiguration() {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("MySQL配置已加载:");
        log.info("  URL: {}", maskSensitiveInfo(url));
        log.info("  Username: {}", username);
        log.info("  Password: {}", password != null ? "****" : "null");
        log.info("  Driver: {}", driverClassName);
        log.info("  初始连接数: {}", initialSize);
        log.info("  最大活跃连接数: {}", maxActive);
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    /**
     * 屏蔽敏感信息（用于日志输出）
     */
    private String maskSensitiveInfo(String url) {
        if (url == null) {
            return "null";
        }
        // 只显示主机和端口，隐藏其他参数
        int questionMark = url.indexOf('?');
        if (questionMark > 0) {
            return url.substring(0, questionMark) + "?...";
        }
        return url;
    }
}

