package com.arkell.entity.auth

import com.arkell.entity.UserEntity
import com.arkell.util.IDGenerator
import com.fasterxml.jackson.annotation.JsonIgnore

class AccessToken(refreshToken: RefreshToken) {

	var id = IDGenerator.longId()
	@JsonIgnore
	var user: UserEntity = refreshToken.user
	val userId: String
		get() = user.id
	var timeout: Long = System.currentTimeMillis() + defaultTimeout

	val parentRefreshToken = refreshToken.id

	val isValid: Boolean
		get() = System.currentTimeMillis() < timeout

	fun updateTimeout(timeLeft: Long = defaultTimeout) {
		this.timeout = System.currentTimeMillis() + timeLeft
	}

	companion object {
		val defaultTimeout = 3_600_000L
	}

}