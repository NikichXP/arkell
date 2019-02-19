package com.arkell.repo.geo

import com.arkell.entity.geo.City
import com.arkell.entity.geo.Region
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository

@Repository
interface CityRepo : JpaRepository<City, String>, JpaSpecificationExecutor<City> {

	fun findByParentRegion(region: Region): List<City>

	fun findByName(name: String): List<City>
	fun findByNameAndParentRegion(name: String, region: Region): City?

}