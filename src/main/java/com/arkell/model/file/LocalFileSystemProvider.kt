package com.arkell.model.file

import org.apache.poi.util.IOUtils
import java.io.File
import java.io.InputStream

class LocalFileSystemProvider(var parentPath: String) : FileSystemProvider {

	init {
		if (!parentPath.endsWith('/')) {
			parentPath += '/'
		}
	}

	override fun exists(name: String): Boolean = File(parentPath + name).exists()

	override fun list(prefix: String): List<String> = File(parentPath + prefix).list().asList()

	override fun getInStream(name: String): InputStream = File(parentPath + name).inputStream()

	override fun putObject(name: String, stream: InputStream) {
		val file = File(parentPath + name)
		file.createNewFile()
		IOUtils.copy(stream, file.outputStream())
	}

	override fun delete(name: String): Boolean = File(parentPath + name).delete()

}