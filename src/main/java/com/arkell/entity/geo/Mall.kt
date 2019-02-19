package com.arkell.entity.geo

import com.arkell.entity.Saveable
import com.arkell.util.IDGenerator
import javax.persistence.*

@Entity
class Mall(
		var title: String,
		@Embedded
		var point: GeoPoint) : Saveable() {

	constructor() : this("", GeoPoint(0.0, 0.0))

	@Id
	override var id: String = IDGenerator.longId()

	@ManyToMany
	var places = mutableListOf<Place>()

	var cityId: String? = places.firstOrNull()?.parentCity?.id

	var streetName: String? = null
	var streetType: String? = null
	var building: String? = null
	var buildingSection: String? = null
	var territory: String? = null
	var district: String? = null

	override var created: Long? = System.currentTimeMillis()
	override var updated: Long? = System.currentTimeMillis()
	override var visible: Boolean? = true
}