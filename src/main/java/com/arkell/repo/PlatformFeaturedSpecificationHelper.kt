package com.arkell.repo

import com.arkell.entity.VisibleSaveable
import com.arkell.entity.misc.Platform
import org.springframework.data.jpa.domain.Specification
import javax.persistence.criteria.Predicate

object PlatformFeaturedSpecificationHelper {

	fun <T : VisibleSaveable> getFeaturedFilters(platform: Platform, featured: Boolean? = null,
	                                             showHidden: Boolean? = null): Specification<T> {
		return Specification { root, _, cb ->
			return@Specification if (platform == Platform.admin) {
				cb.conjunction()
			} else {
				val platformName = platform.type

				val predicates = mutableListOf<Predicate>()

				if (featured == true) {
					predicates += cb.equal(root.get<Boolean>("featured$platformName"), true)
				}

				if (showHidden != true) {
					predicates += cb.equal(root.get<Boolean>("show$platformName"), true)
					predicates += cb.lt(root["published$platformName"], System.currentTimeMillis())
				}

				if (predicates.isEmpty()) {
					cb.conjunction()
				} else {
					cb.and(*predicates.toTypedArray())
				}
			}
		}
	}

}