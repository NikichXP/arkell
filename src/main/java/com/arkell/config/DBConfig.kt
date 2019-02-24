package com.arkell.config

import com.google.gson.JsonParser
import com.zaxxer.hikari.HikariDataSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import java.io.File
import java.util.*
import javax.sql.DataSource

@Configuration
@EnableTransactionManagement
class DBConfig {

	@Bean
	fun entityManagerFactory(dataSource: DataSource): LocalContainerEntityManagerFactoryBean {
		val em = LocalContainerEntityManagerFactoryBean()
		em.dataSource = dataSource
		em.setPackagesToScan("com.arkell")

		val vendorAdapter = HibernateJpaVendorAdapter()
		em.jpaVendorAdapter = vendorAdapter

		em.setJpaProperties(hibernateProperties())

		return em
	}

	@Bean
	fun transactionManager(dataSource: DataSource): PlatformTransactionManager {
		val transactionManager = JpaTransactionManager()
		transactionManager.entityManagerFactory = entityManagerFactory(dataSource).getObject()
		return transactionManager
	}

	//	@Bean
	//	fun sessionFactory(): LocalSessionFactoryBean {
	//		val sessionFactory = LocalSessionFactoryBean()
	//		sessionFactory.setDataSource(dataSource());
	//		sessionFactory.setPackagesToScan("com.arkell");
	//		sessionFactory.hibernateProperties = hibernateProperties();
	//		return sessionFactory;
	//	}

	//	val cacheBean = EhCacheManagerFactoryBean()
	//	val cacheManager = EhCacheCacheManager()
	//
	//	@Bean
	//	fun getCacheManager(): CacheManager {
	//		return cacheManager
	//	}
	//
	//	@Bean
	//	fun getCacheFactoryBean(): EhCacheManagerFactoryBean {
	//		return cacheBean
	//	}

	//	@Bean
	//	fun hibernateTransactionManager(): PlatformTransactionManager {
	//		val transactionManager = HibernateTransactionManager();
	//		transactionManager.sessionFactory = sessionFactory().getObject();
	//		return transactionManager;
	//	}

	fun hibernateProperties(): Properties {
		val properties = Properties()
		properties.setProperty("hibernate.hbm2ddl.auto", "update")
		properties.setProperty("hibernate.ddl-auto", "update")
		properties.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
		//				if (testMachine) "org.hibernate.dialect.PostgreSQLDialect"
		//				else "org.hibernate.dialect.MySQL57Dialect")
		properties.setProperty("hibernate.naming-strategy", "org.hibernate.cfg.ImprovedNamingStrategy")
		properties.setProperty("hibernate.jdbc.lob.non_contextual_creation=true", "true")
		properties.setProperty("hibernate.show_sql", "false")
		properties.setProperty("hibernate.cache.use_second_level_cache", "true")
		properties.setProperty("hibernate.jdbc.lob.non_contextual_creation", "true")
		properties.setProperty("hibernate.cache.region.factory_class",
				"org.hibernate.cache.ehcache.EhCacheRegionFactory")
		properties.setProperty("max-active", "5")

		return properties;
	}

	@Bean()
	fun dataSource(): DataSource {
		val dataSource = HikariDataSource()
		//
		dataSource.driverClassName = "org.postgresql.Driver" // if (testMachine) "org.postgresql.Driver" else "com.mysql.jdbc.Driver"

		val json = JsonParser().parse(File(System.getProperty("user.dir") + "/conf.json").readText())

		json.asJsonObject.getAsJsonObject("db").apply {
			dataSource.jdbcUrl = get("url").asString
			dataSource.username = get("user").asString
			dataSource.password = get("pass").asString
			dataSource.schema = try {
				get("schema").asString
			} catch (e: Exception) {
				"public"
			}

			if (has("maxPoolSize")) {
				dataSource.maximumPoolSize = get("maxPoolSize").asInt
			} else {
				dataSource.maximumPoolSize = 5
			}
		}

		val testDB = false

		if (testDB && File("C:/").exists()) {
			dataSource.jdbcUrl = "jdbc:postgresql://127.0.0.1:5432/arkell"
			dataSource.username = "postgres"
			dataSource.password = "root"
		}

		return dataSource
	}
}