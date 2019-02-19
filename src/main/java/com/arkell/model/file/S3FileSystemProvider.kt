package com.arkell.model.file

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.ListObjectsV2Request
import com.amazonaws.services.s3.model.ObjectMetadata
import java.io.InputStream

class S3FileSystemProvider(accessKey: String, secretKey: String, val bucketName: String) : FileSystemProvider {

	var maxKeyListSize = 10_000

	val s3Client = AmazonS3ClientBuilder
		.standard()
		.withCredentials(
				AWSStaticCredentialsProvider(
						BasicAWSCredentials(accessKey, secretKey)))
		.withRegion(Regions.EU_WEST_1)
		.build()

	override fun list(prefix: String): List<String> {
		return s3Client
			.listObjectsV2(
					ListObjectsV2Request()
						.withPrefix(prefix)
						.withMaxKeys(maxKeyListSize))
			.objectSummaries
			.map { it.key }
	}

	override fun exists(name: String): Boolean {
		return s3Client.doesObjectExist(bucketName, name)
	}

	override fun getInStream(name: String): InputStream {
		return s3Client.getObject(bucketName, name).objectContent
	}

	override fun putObject(name: String, stream: InputStream) {
		s3Client.putObject(bucketName, name, stream, ObjectMetadata().also {
			it.contentType = when (name.substringAfterLast('.').toLowerCase()) {
				"jpg", "jpeg" -> "image/jpeg"
				"png" -> "image/png"
				"gif" -> "image/gif"
				else -> "application/octet-stream"
			}
		})
	}

	override fun delete(name: String): Boolean {
		s3Client.deleteObject(bucketName, name)
		return true
	}
}