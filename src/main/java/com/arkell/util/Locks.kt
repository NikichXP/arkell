package com.arkell.util

import kotlinx.coroutines.experimental.runBlocking
import kotlinx.coroutines.experimental.sync.Mutex
import kotlinx.coroutines.experimental.sync.withLock
import java.util.concurrent.ConcurrentHashMap

object Locks {

	val locks = ConcurrentHashMap<String, Mutex>()

	suspend inline fun withLock(lockId: String, function: () -> Unit) {
		locks.getOrPut(lockId, { Mutex() })
			.withLock {
				function.invoke()
			}
	}

	inline fun withBlock(lockId: String, crossinline function: () -> Unit) {
		runBlocking { withLock(lockId, function) }
	}

	//	val locks = ConcurrentHashMap<String, Semaphore>()
	//
	//	fun withBlock(lockId: String, function: () -> Unit) {
	//		val lock = locks.getOrPut(lockId, { Semaphore(1) })
	//
	//		Logger.getLogger(this::class.java).info("Locking: $lockId")
	//
	//		lock.acquire()
	//
	//		try {
	//			function()
	//		} finally {
	//			lock.release()
	//			Logger.getLogger(this::class.java).info("Unlock: $lockId")
	//		}
	//	}
}