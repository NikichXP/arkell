package com.arkell.test

import com.arkell.api.AuthAPI
import com.arkell.entity.exception.CustomExceptionCode
import com.arkell.entity.exception.NotLoggedInException
import com.arkell.model.auth.AuthService
import org.jboss.logging.Logger
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

@Service
class AccessUpdater(
		val authService: AuthService,
		val authAPI: AuthAPI) {
	
	@PostConstruct
	fun init() {
		login("abc@xyz.com", "1234", "admintoken")
		login("tester@test.com", "12345", "testuser")
	}
	
	fun login(mail: String, pass: String, needToken: String) {
		try {
			val refreshToken = authAPI.loginByMail(mail, pass, false)
			val token = authService.createAccessToken(refreshToken.refreshToken)
			authService.changeAccessTokenName(token.id, needToken)
		} catch (e: CustomExceptionCode) {
			Logger.getLogger(this::class.java).warn("Update accessUpdater: add new auth reason for $mail")
		}
	}
	
	fun updateTokens(vararg tokens: String) {
		for (token in tokens) {
			try {
				authService.renewToken(token)
			} catch (e: NotLoggedInException) {
				init()
			}
		}
	}
	
	@Scheduled(fixedRate = 60_000L, initialDelay = 60_000L)
	fun updateAdminToken() {
		updateTokens("admintoken", "testuser")
	}
	
}