package com.arkell.model.file

import com.arkell.util.blockAwait
import com.google.gson.JsonParser
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.asCoroutineDispatcher
import kotlinx.coroutines.experimental.async
import org.apache.tomcat.util.http.fileupload.IOUtils
import org.jboss.logging.Logger
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.stereotype.Service
import org.springframework.util.FileCopyUtils
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.*
import java.util.concurrent.Executors
import javax.imageio.ImageIO
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.Part
import kotlin.streams.toList

@Service
class FileModel(
		environment: ConfigurableEnvironment) {

	val fileSystem: FileSystemProvider

	init {

		val json = JsonParser().parse(File(System.getProperty("user.dir") + "/conf.json").readText()).asJsonObject

		val filestore = json.get("filestore").asString

		fileSystem = if (filestore == "aws:s3") {
			json.getAsJsonObject("s3").run {
				S3FileSystemProvider(
						accessKey = get("accessKey").asString,
						secretKey = get("secretKey").asString,
						bucketName = get("bucket").asString
				)
			}
		} else {
			LocalFileSystemProvider(filestore)
		}
	}

	//	private val bucketName = "arkell"
	//	private val amazonServer = "https://s3-eu-west-1.amazonaws.com/"

	private val context = Executors.newFixedThreadPool(10).asCoroutineDispatcher()

	private val log = Logger.getLogger(this::class.java)

	private val tempDir = System.getProperty("user.dir") + "/src/main/resources/files/temp/"

	init {
		val parent = File(tempDir)
		if (parent.exists()) {
			parent.delete()
		}
		parent.mkdirs()
		//		try {
		//			s3Client = AmazonS3Client(BasicAWSCredentials(
		//					"AKIAISIYOCTXF5XTYYYA",
		//					"9DbBH2M67JtXDroqLCBEfby1RtHFMG0Mh7ihcgvl"))
		//		} catch (e: Exception) {
		//			Logger.getLogger(this::class.java).warn("S3 did not started!")
		//		}
	}

	fun getFileStream(fileName: String): InputStream {
		return fileSystem.getInStream(fileName)
	}

	fun resize(file: String, size: Int) {
		if (fileSystem.exists("resized/$size$file")) {
			return // getFileStream("resized/$size$file")
		}

		//		var s3Object: S3Object? = null
		log.info("creating resized version of $file ($size)")
		val oldFile = File(tempDir + file)

		try {
			oldFile.createNewFile()
			val stream = fileSystem.getInStream(file)
			try {

				//				s3Object = s3Client.getObject(GetObjectRequest(bucketName, file))
				FileCopyUtils.copy(
						stream,
						FileOutputStream(oldFile)
				)
			} finally {
				stream.close()
			}

			val newFile = resizeImage(oldFile, size).blockAwait()

			try {
				fileSystem.putObject("resized/$size$file", newFile.inputStream())

				//				s3Client.putObject(PutObjectRequest(bucketName, "resized/$size$file", newFile))
				oldFile.delete()
				println("done resized version of $file ($size)")
				//				return "$amazonServer$bucketName/resized/$size$file"
			} finally {
				newFile.delete()
			}
		} finally {
			oldFile.delete()
			System.gc()
			//			if (s3Object != null) {
			//				s3Object.close()
			//			}
		}
	}

	//	fun sendResize(file: String, size: Int, request: HttpServletRequest, response: HttpServletResponse) {
	//
	//		if (s3Client.doesObjectExist(bucketName, "resized/$size$file")) {
	//			response.sendRedirect("$amazonServer$bucketName/resized/$size$file")
	//			return
	//		}
	//
	//		var s3Object: S3Object? = null
	//		log.info("creating resized version of $file ($size)")
	//		val oldFile = File(tempDir + file)
	//
	//		try {
	//			oldFile.createNewFile()
	//
	//			try {
	//				s3Object = s3Client.getObject(GetObjectRequest(bucketName, file))
	//				FileCopyUtils.copy(
	//						s3Object!!.objectContent,
	//						FileOutputStream(oldFile)
	//				)
	//			} finally {
	//				s3Object!!.close()
	//			}
	//
	//			val newFile = resizeImage(oldFile, size).blockAwait()
	//
	//			try {
	//				fileSystem.putObject("resized/$size$file", newFile.inputStream())
	////				s3Client.putObject(PutObjectRequest(bucketName, "resized/$size$file", newFile))
	//
	//				val sc = request.session.servletContext
	//				response.contentType = sc.getMimeType(file)
	//				response.addHeader("Access-Control-Allow-Origin", "*")
	//				response.setContentLength(newFile.length().toInt())
	//				response.setHeader("Content-Disposition", "attachment; filename=\"$file\"")
	//				try {
	//					FileCopyUtils.copy(FileInputStream(newFile), response.outputStream)
	//				} catch (e: IOException) {
	//					e.printStackTrace()
	//				}
	//			} finally {
	//				newFile.delete()
	//			}
	//
	//			oldFile.delete()
	//			println("done resized version of $file ($size)")
	//
	//
	//		} catch (e: Exception) {
	//			e.printStackTrace()
	//		} finally {
	//			oldFile.delete()
	//			System.gc()
	//			if (s3Object != null) {
	//				s3Object.close()
	//			}
	//		}
	//	}

	fun resizeImage(oldFile: File, size: Int): Deferred<File> = async {
		val newFile = File(tempDir + "resized/" + size + oldFile.name)
		try {
			newFile.createNewFile()
			val originalImage = ImageIO.read(oldFile)
			val h: Int
			val w: Int
			if (Math.max(originalImage.height, originalImage.width) <= size) {
				oldFile.copyTo(newFile)
				return@async newFile
			}
			if (originalImage.height > originalImage.width) {
				w = size
				h = (originalImage.height * (w * 1.0 / originalImage.width)).toInt()
			} else {
				h = size
				w = (originalImage.width * (h * 1.0 / originalImage.height)).toInt()
			}
			val image = originalImage.getScaledInstance(w, h, Image.SCALE_AREA_AVERAGING)
			val changedImage = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
			val g2d = changedImage.createGraphics()
			g2d.drawImage(image, 0, 0, null)
			g2d.dispose()
			ImageIO.write(changedImage, "jpg", newFile)
			return@async newFile
		} catch (e: Exception) {
			newFile.delete()
			return@async newFile
		}
	}

	fun upload(request: HttpServletRequest, vararg names: String): List<String> {
		return names.toList().parallelStream()
			.map { uploadPart(request.getPart(it)) }
			.map { it.blockAwait() }
			.toList()
	}

	fun uploadLocalFile(file: File, futureId: String = UUID.randomUUID().toString()): String {
		try {
			fileSystem.putObject(futureId, file.inputStream())
		} catch (e: Exception) {
		}
		return futureId
	}

	fun uploadPart(part: Part): Deferred<String> {
		val fileExt = part.submittedFileName.substringAfterLast('.')
		val fileName = UUID.randomUUID().toString() + '.' + fileExt
		return async(context) {
			fileSystem.putObject(fileName, part.inputStream)
			return@async fileName
		}
	}

	fun uploadToTempStorage(part: Part): File {
		val file = File(System.getProperty("user.dir") + "/temp/" + part.submittedFileName)
		file.parentFile.mkdir()
		file.createNewFile()
		IOUtils.copy(part.inputStream, file.outputStream())
		return file
	}

}

