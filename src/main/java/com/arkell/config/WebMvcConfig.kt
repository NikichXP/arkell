package com.arkell.config

import com.arkell.auth.AccessInterceptor
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.servlet.ViewResolver
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.thymeleaf.spring5.ISpringTemplateEngine
import org.thymeleaf.spring5.SpringTemplateEngine
import org.thymeleaf.spring5.templateresolver.SpringResourceTemplateResolver
import org.thymeleaf.spring5.view.ThymeleafViewResolver
import org.thymeleaf.templatemode.TemplateMode
import org.thymeleaf.templateresolver.ITemplateResolver


@Configuration
@EnableWebMvc
//@EnableCaching
@EnableScheduling
class WebMvcConfig(
		private val context: ApplicationContext,
		private val accessInterceptor: AccessInterceptor) : WebMvcConfigurer { //, CachingConfigurer {

	//	override fun cacheManager(): CacheManager {
	//		return SimpleCacheManager().also {
	//			it.setCaches(mutableListOf(
	//					ConcurrentMapCache("category")
	//			))
	//		}
	//	}
	//
	//	override fun keyGenerator(): KeyGenerator {
	//		return SimpleKeyGenerator()
	//	}
	//
	//	override fun cacheResolver(): CacheResolver {
	//		return SimpleCacheResolver()
	//	}
	//
	//	override fun errorHandler(): CacheErrorHandler {
	//		return SimpleCacheErrorHandler()
	//	}

	@Bean
	fun viewResolver(): ViewResolver {
		val resolver = ThymeleafViewResolver()
		resolver.templateEngine = templateEngine()
		resolver.characterEncoding = "UTF-8"
		return resolver
	}

	override fun configureMessageConverters(converters: MutableList<HttpMessageConverter<*>>) {
		super.configureMessageConverters(converters)
		converters.add(0, MappingJackson2HttpMessageConverter())
	}

	@Bean
	fun templateEngine(): ISpringTemplateEngine {
		val engine = SpringTemplateEngine()
		engine.enableSpringELCompiler = true
		engine.setTemplateResolver(templateResolver())
		return engine
	}

	private fun templateResolver(): ITemplateResolver {
		val resolver = SpringResourceTemplateResolver()
		resolver.setApplicationContext(context)
		resolver.prefix = "/templates/"
		resolver.templateMode = TemplateMode.HTML
		return resolver
	}

	override fun addInterceptors(registry: InterceptorRegistry) {
		registry.addInterceptor(accessInterceptor).addPathPatterns("/api/**")
	}

}