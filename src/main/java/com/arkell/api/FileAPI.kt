package com.arkell.api

import com.arkell.entity.exception.CustomExceptionCode
import com.arkell.model.file.FileModel
import com.arkell.util.blockAwait
import com.google.api.client.util.IOUtils
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@RestController
@RequestMapping("/api/file")
class FileAPI(
		val fileModel: FileModel) {

	/**
	 * You will be redirected to file URL
	 * @param token is optional. in future, when some file could have access labels
	 */
	@GetMapping("/**")
	fun getFile(request: HttpServletRequest, response: HttpServletResponse, @RequestHeader token: String?) {
		returnFile(request.requestURI.substringAfter("api/file/"), response)
	}

	/**
	 * Non-API method. Returns just what you need
	 */
	fun returnFile(path: String, response: HttpServletResponse) {
		val ext = path.substringAfterLast('.')
		response.contentType = when (ext.toLowerCase()) {
			"jpg", "jpeg" -> "image/jpeg"
			"png" -> "image/png"
			"gif" -> "image/gif"
			else -> "application/octet-stream"
		}
		response.addHeader("Access-Control-Allow-Methods", "GET")
		response.addHeader("Access-Control-Allow-Credentials", "*")
		response.addHeader("Access-Control-Max-Age", "3600")
		response.addHeader("Access-Control-Allow-Headers", "*")
		response.addHeader("Access-Control-Allow-Origin", "*")

		try {
			IOUtils.copy(fileModel.getFileStream(path), response.outputStream)
		} catch (e: Exception) {
			throw CustomExceptionCode(404, "Object not found")
		}
	}

	/**
	 * Resizes image (shown in size) to specified size.
	 * @param size is size of minimal side. if original image was 600x400 and the size arg = 200, result will be 300x200 px
	 * @param file is filename, like abcd.jpg
	 */
	@GetMapping("/getimg/{size}")
	fun getimg(@PathVariable size: Int, @RequestParam file: String,
	           response: HttpServletResponse, request: HttpServletRequest) {
		fileModel.resize(file, size)

		return returnFile("resized/$size$file", response)
	}

	/**
	 * Uploads file. Returns file ID in response
	 * @param token this could be request header OR request param.
	 */
	@PostMapping("/upload")
	fun upload(request: HttpServletRequest,
	           @RequestHeader @RequestParam token: String?): String {
		val token_ = token ?: request.getHeader("token") ?: request.getParameter("token")
		?: throw CustomExceptionCode("Token not provided", 401)
		return fileModel.uploadPart(request.getPart("file")).blockAwait()
	}

	/**
	 * Test upload, with optional 'token' header.
	 */
	@PostMapping("/testupload")
	fun testUpload(request: HttpServletRequest,
	               @RequestHeader token: String?): String {
		return fileModel.uploadPart(request.getPart("file")).blockAwait()
	}

}