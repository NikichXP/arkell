package com.arkell.model.file

import java.io.InputStream

interface FileSystemProvider {

	fun list() = list("")
	fun list(prefix: String = ""): List<String>
	fun exists(name: String): Boolean
	fun getInStream(name: String): InputStream
	fun putObject(name: String, stream: InputStream)
	fun delete(name: String): Boolean

}