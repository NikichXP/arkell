package com.arkell.util

import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.newSingleThreadContext
import java.util.concurrent.atomic.AtomicLong

class ExecutionLimiter(private var silenceTime: Long) {

	private val lastAction = AtomicLong(0L)
	private var action = {}
	private var invoker = launch { }

	@Synchronized
	fun action(newAction: () -> Unit) {

		this.action = newAction

		if (lastAction.get() + silenceTime < System.currentTimeMillis()) {
			lastAction.set(System.currentTimeMillis())
			launch { newAction.invoke() }
			return
		}

		if (invoker.isCompleted) {
			invoker = launch {
				delay(silenceTime - (System.currentTimeMillis() - lastAction.get()))
				lastAction.set(System.currentTimeMillis())
				launch(newSingleThreadContext("exec-limiter")) { action.invoke() }
			}
			action = newAction
		}
	}

}