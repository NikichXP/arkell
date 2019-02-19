package com.arkell.util.objects

class ObjectObserver<T>(value: T) {

	var value: T = value
		set(newValue) {
			if (hasDeepChange(field)) {
				onChange.invoke(field)
			}
			field = newValue
			action.invoke(value)
		}
		get() {
			onAccess?.invoke(field)
			return field
		}

	var hasDeepChange: (T) -> Boolean = {
		((it?.hashCode() ?: 0) != prevHash).also { prevHash = it.hashCode() }
	}
	var action: (T) -> Unit = {}
	var onChange: (T) -> Unit = {}
	var onAccess: ((T) -> Unit)? = null

	private var prevHash = value?.hashCode() ?: 0
	private var silentGet = false

	constructor(action: () -> T) : this(action.invoke())
	constructor(value: T, onChangeAction: (T) -> Unit) : this(value) {
		this.onChange = onChangeAction
	}

	fun getSilent(): T {
		silentGet = true
		return value.also { silentGet = false }
	}
}