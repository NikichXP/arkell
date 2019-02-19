package com.arkell.repo

import com.arkell.util.OffsetPageRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.CriteriaQuery
import javax.persistence.criteria.Predicate
import javax.persistence.criteria.Root

/**
 * Helps create JPA specs faster and easier
 */
class SpecificationHelper<T> {

	val specs = mutableListOf<Specification<T>>()
	var sort: Sort = Sort.by(Sort.Direction.DESC, "created")
	var page: Int? = 0
	var pageSize = 20
	var offset: Int? = null

	/**
	 * Filters is value for each key != null
	 */
	fun with(vararg args: Pair<String, Any?>) = apply {
		for (pair in args) {
			pair.second ?: continue
			specs += Specification { root, _, cb ->
				cb.equal(root.get<Any>(pair.first), pair.second)
			}
		}
	}

	fun withNot(args: Pair<String, Any?>) = apply {
		where { root, _, cb -> cb.notEqual(root.get<Any>(args.first), args.second) }
	}

	fun eqNotNull(arg: String, value: Any?) = apply {
		if (value != null) {
			this.with(arg to value)
		}
	}

	fun textIgnoreCase(arg: String, value: String?) = apply {
		value ?: return@apply
		where(Specification { root, cq, cb ->
			cb.like(cb.lower(root.get<String>(arg)), "%${value.toLowerCase()}%")
		})
	}

	fun where(spec: Specification<T>) = apply { specs += spec }

	inline fun where(crossinline spec: (Root<T>, CriteriaQuery<*>, CriteriaBuilder) -> Predicate) = apply {
		specs += Specification { root, query, criteriaBuilder -> spec(root, query, criteriaBuilder) }
	}

	fun page(page: Int, pageSize: Int = 20) = apply {
		this.page = page
		this.pageSize = pageSize
	}

	fun offset(offset: Int, pageSize: Int = 20) = apply {
		this.page = null
		this.pageSize = pageSize
		this.offset = offset
	}

	fun sort(direction: Sort.Direction, vararg param: String) = apply {
		sort = Sort.by(direction, *param)
	}

	fun result(repository: JpaSpecificationExecutor<T>): Page<T> {
		if (specs.isEmpty()) {
			specs += Specification { _, _, criteriaBuilder -> criteriaBuilder.conjunction() }
		}

		return if (page != null) repository.findAll(specs.reduce { s1, s2 -> s1.and(s2) },
				PageRequest.of(page!!, pageSize, sort))
		else repository.findAll(specs.reduce { s1, s2 -> s1.and(s2) },
				OffsetPageRequest(offset!!, pageSize, sort))
	}

	fun resultUnPaged(repository: JpaSpecificationExecutor<T>): List<T> {
		if (specs.isEmpty()) {
			specs += Specification { _, _, criteriaBuilder -> criteriaBuilder.conjunction() }
		}

		return repository.findAll(specs.reduce { s1, s2 -> s1.and(s2) })
	}

}