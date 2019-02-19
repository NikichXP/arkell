package com.arkell.entity.misc

import java.time.DayOfWeek
import javax.persistence.ElementCollection
import javax.persistence.FetchType

class WorkingHours() {

	constructor(times: Map<Int, String>) : this() {
		times.forEach { k, v ->
			this.time[DayOfWeek.of(k)] = v
		}
	}

	@ElementCollection(fetch = FetchType.EAGER)
	var time = mutableMapOf<DayOfWeek, String>()

	fun set(day: Int, time: String) = this.apply {
		this.time[DayOfWeek.of(day)] = time
	}

	fun remove(day: Int) = this.apply {
		this.time.remove(java.time.DayOfWeek.of(day))
	}

	fun range(days: IntRange, time: String) = this.apply {
		days.iterator().forEach { set(it, time) }
	}
}