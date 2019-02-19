package com.arkell.entity.auth

enum class AuthPermission(val level: Int) {
	BLOCKED(0), NONE(1), USER(2), CLIENT(3), PARTNER(4), MODERATOR(5), ADMIN(6)
}
