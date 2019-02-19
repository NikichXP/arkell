package com.arkell.model

import com.arkell.entity.Saveable
import com.arkell.entity.exception.ElementNotFoundException
import com.arkell.util.Locks
import com.arkell.util.objects.ObjectFromMapUpdater
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
abstract class UpdateAction<T : Saveable> {

	abstract val repository: CrudRepository<T, String>

	@Transactional
	fun getById(id: String): T = repository.findById(id).orElseGet { notFound(id) }

	fun edit(id: String, update: T.() -> Unit): T = update(id) { update(it) }

	fun save(entity: T): T = repository.save(entity)

	fun update(id: String, action: (T) -> Unit): T {
		var t: T? = null
		Locks.withBlock(id) {
			t = getById(id)
			action(t!!)
			repository.save(t!!)
		}
		return t ?: notFound()
	}

	@Transactional
	fun getByIds(ids: Iterable<String>): List<T> {

		return ids.map {
			return@map try {
				getById(it)
			} catch (e: Exception) {
				null
			}
		}.filterNotNull()
	}

	fun autoEdit(id: String, params: Map<String, String>, vararg exclude: String, action: T.() -> Unit): T =
			autoUpdate(id, params, *exclude) { action(it) }

	fun autoUpdate(id: String, params: Map<String, String>, vararg exclude: String, action: (T) -> Unit): T {
		var t: T? = null
		Locks.withBlock(id) {
			t = getById(id)
			ObjectFromMapUpdater(t!!, params)
				.exclude(*exclude)
				.modify()
			action(t!!)
			t?.updated = System.currentTimeMillis()
			repository.save(t!!)
		}
		return t ?: notFound()
	}

	fun notFound(reason: String = ""): T {
		throw ElementNotFoundException("Given element ($reason) not found")
	}

	fun deleteById(id: String): Boolean {
		repository.deleteById(id)
		return true
	}


}