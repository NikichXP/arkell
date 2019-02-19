package com.arkell.entity.exception

class CustomExceptionCode(message: String = "Exception with custom code", var code: Int) : Exception(message) {
	constructor(code: Int, message: String = "Expected error happened") : this(message, code)
}