package com.arkell.util

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@Service
class OrdinalIDGetter(
		val jdbcTemplate: JdbcTemplate) {

	private val ids = ConcurrentHashMap<String, AtomicLong>()

	fun getByTable(name: String): Long {
		return ids.getOrPut(name, { AtomicLong(loadLastByTable(name) + 1) }).incrementAndGet()
	}

	fun loadLastByTable(name: String): Long =
			jdbcTemplate.queryForList("select id from $name").map {
				try {
					it["id"]?.toString()?.toLong()
				} catch (e: Exception) {
					null
				}
			}.filterNotNull().max() ?: 0

}