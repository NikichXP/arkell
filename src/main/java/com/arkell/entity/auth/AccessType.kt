package com.arkell.entity.auth

enum class AccessTypeS(val value: Int) {
	BLOCKED(-1), NOT_CLIENT(1), CLIENT(2), MODERATOR(4), ADMIN(5)
}