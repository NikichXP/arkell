package com.arkell

import com.arkell.entity.UserEntity
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import kotlin.reflect.full.memberProperties

@SpringBootApplication
@Configuration
@ComponentScan
@EnableAutoConfiguration
class App

fun main(args: Array<String>) {
	runApplication<App>()
	UserEntity::class.memberProperties
}
