package com.arkell.repo.geo

import com.arkell.entity.geo.ObjectLocation
import com.arkell.entity.geo.Place
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor

interface ObjectLocationRepo : JpaRepository<ObjectLocation, String>, JpaSpecificationExecutor<ObjectLocation> {

	fun findByPlace(place: Place): List<ObjectLocation>
	fun findByIsServiceAndPartnerId(status: Boolean, partnerId: String): List<ObjectLocation>

}