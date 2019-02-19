package com.arkell.repo.geo

import com.arkell.entity.geo.City
import com.arkell.entity.geo.Place
import com.arkell.entity.geo.Region
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository

@Repository
interface PlaceRepo : JpaRepository<Place, String>, JpaSpecificationExecutor<Place> {

	fun findByName(name: String): List<Place>
	fun findByParentCity(city: City): List<Place>
	fun findByParentCityParentRegion(region: Region): List<Place>
	fun findByNameIgnoreCaseAndParentCity(name: String, city: City): Place?
	fun findByNameContaining(string: String): List<Place>
	fun findByIsReal(isReal: Boolean): List<Place>

}