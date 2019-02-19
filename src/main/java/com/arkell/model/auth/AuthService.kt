package com.arkell.model.auth

import com.arkell.entity.UserEntity
import com.arkell.entity.auth.AccessToken
import com.arkell.entity.auth.AuthPermission
import com.arkell.entity.auth.AuthReason
import com.arkell.entity.auth.RefreshToken
import com.arkell.entity.exception.CustomExceptionCode
import com.arkell.entity.exception.NotLoggedInException
import com.arkell.model.UserService
import com.arkell.repo.auth.RefreshTokenRepo
import org.jboss.logging.Logger
import org.springframework.context.annotation.Lazy
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class AuthService(
		private val refreshTokenRepo: RefreshTokenRepo,
		@Lazy private val userService: UserService) {

	private val logger = Logger.getLogger(this::class.java)

	private val activeSessions = ConcurrentHashMap<String, AccessToken>()
	private val activeUsers = ConcurrentHashMap<String, UserEntity>()

	@Scheduled(fixedDelay = 60_000)
	fun checkTokens() {
		activeSessions.values.stream().filter { !it.isValid }.forEach { onTokenTimeout(it) }
	}

	fun login(reason: AuthReason): RefreshToken {
		val user = activeUsers.getOrPut(reason.user.id, { reason.user })
		val token = RefreshToken(user = user)
		return token.apply { refreshTokenRepo.save(this) }
	}

	fun createAccessToken(refreshToken: RefreshToken): AccessToken {
		val token = AccessToken(refreshToken)
		val user: UserEntity = activeUsers.getOrPut(token.user.id, { token.user })
		token.user = user
		activeUsers[user.id] = user
		activeSessions[token.id] = token
		return token
	}

	fun createAccessToken(refreshToken: String): AccessToken =
			refreshTokenRepo.findById(refreshToken)
				.map { createAccessToken(it) }
				.orElseThrow { throw CustomExceptionCode("Token is invalid", 999) }

	fun getAccessToken(accessToken: String): AccessToken {
		return activeSessions[accessToken] ?: if (accessToken.startsWith("force-user:")) {

			logger.info("force-user is: ${accessToken.substringAfter("force-user:")}")

			forceUserLogin(accessToken.substringAfter("force-user:"))
		} else {
			error("Access token wrong or expired")
		}
	}

	/** TODO Test feature to understand WHAT THE HELL IS WRONG with mobile apps. Remove it. */
	fun forceUserLogin(data: String): AccessToken {

		logger.info("User-force-login detected")

		val user = activeUsers.values.find { it.id == data || it.mail == data }
				?: userService.getByMail(data)
				?: userService.getById(data)

		activeUsers[user.id] = user

		return AccessToken(refreshToken = RefreshToken(user)).apply {
			activeSessions[id] = this
			activeSessions["force-user:$data"] = this
		}
	}

	@Throws(Exception::class)
	fun getUser(accessToken: String): UserEntity {
		val token = activeSessions[accessToken] ?: error("Access token wrong or expired")
		return activeUsers.getOrPut(token.userId, { token.user })
	}

	fun getUserOrNull(id: String): UserEntity? = activeUsers[id]

	fun getUserByTokenOrNull(id: String): UserEntity = getAccessToken(id).user

	fun renewToken(accessToken: String): AccessToken = (activeSessions[accessToken] ?: error())
		.also { it.updateTimeout() }

	fun onTokenTimeout(token: AccessToken) {
		val oldToken = activeSessions.remove(token.id)
		if (!activeSessions.values.any { it.user.id == oldToken?.user?.id }) {
			activeUsers.remove(oldToken?.userId)
		}
	}

	fun logout(token: String): Boolean {
		fun deleteAccessToken(token: String): Boolean {
			activeSessions[token]?.also {
				activeSessions.remove(token)
				refreshTokenRepo.deleteById(it.parentRefreshToken)
			} ?: return false
			return true
		}

		fun deleteRefreshToken(token: String): Boolean {
			var ret = false
			activeSessions.keys.forEach {
				if (activeSessions[it]?.parentRefreshToken == token) {
					deleteAccessToken(it)
					ret = true
				}
			}
			return ret
		}

		return if (deleteAccessToken(token))
			true
		else deleteRefreshToken(token)
	}

	private fun error(message: String = "Access denied."): AccessToken = throw NotLoggedInException(message)
	private fun error2(message: String = "Access denied."): RefreshToken = throw NotLoggedInException(message)

	// TEST BELOW

	fun changeAccessTokenName(oldToken: String, newToken: String): AccessToken {
		val token = activeSessions[oldToken]!!
		token.id = newToken
		activeSessions[newToken] = token
		activeSessions.remove(oldToken)
		return token
	}

	fun checkAccess(token: String, access: AuthPermission) {
		if (getUser(token).accessLevel.level <= access.level) {
			throw CustomExceptionCode(403, "You have no access to perform this action.")
		}
	}

	//	fun getAccessLevel(accessToken: String): Int {
	//		return activeSessions[accessToken]?.user?.accessLevel?.accessLevelValue ?: return 0
	//	}

}