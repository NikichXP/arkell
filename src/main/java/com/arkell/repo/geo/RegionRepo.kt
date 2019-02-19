package com.arkell.repo.geo

import com.arkell.entity.geo.Region
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface RegionRepo : JpaRepository<Region, String> {

	fun findByName(name: String): Region?
	fun findByVisible(visible: Boolean): List<Region>

}