package com.arkell.util

import java.util.*

object IDGenerator {

	fun longId() = UUID.randomUUID().toString().filter { it != '-' }
	fun shortId() = UUID.randomUUID().toString().substring(0..7)

	fun numericalGroups(groupSize: Int = 2, groupCount: Int = 4): String {
		val rand = Random()
		val sb = StringBuilder()
		for (i in 0 until groupCount) {
			for (j in 0 until groupSize) {
				sb.append(rand.int(0..9))
			}
			if (i != groupCount - 1) {
				sb.append('-')
			}
		}
		return sb.toString()
	}

	fun base64Code(length: Int = 20): String {
		val bytes = ByteArray(length)
		Random().nextBytes(bytes)
		return Base64.getEncoder().encodeToString(bytes)
	}

}