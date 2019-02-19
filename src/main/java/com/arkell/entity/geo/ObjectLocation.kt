package com.arkell.entity.geo

import com.arkell.entity.Category
import com.arkell.entity.Partner
import com.arkell.entity.Saveable
import com.arkell.util.IDGenerator
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.util.stream.Stream
import javax.persistence.*

@Entity(name = "objectlocation")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
class ObjectLocation() : Saveable() {

	constructor(place: Place, point: GeoPoint = place.point) : this() {
		this.place = place
		this.point = point
	}

	constructor(place: Place) : this(place, place.point)

	constructor(place: Place, x: Double, y: Double) : this(place, GeoPoint(x, y))

	@Id
	override var id: String = IDGenerator.longId()

	@ManyToOne
	var place: Place = Place()
		set(value) {
			field = value
			updateIds()
		}

	@ManyToOne
	var mall: Mall? = null

	@ManyToMany
	var places = mutableListOf<Place>()
		set(value) {
			field = mutableListOf()
			addPlaces(value)
		}

	@Embedded
	var point: GeoPoint = place.point

	// following fields are proxy and required for easy-weighted injections
	var placeId: String? = null
	var cityId: String? = null
	var regionId: String? = null
	var isReal: Boolean? = null

	var isService: Boolean? = false

	@ManyToOne
	var category: Category? = null
	var marker: String? = null

	fun addPlaces(list: Iterable<Place>) {
		list.forEach {
			addPlace(it)
		}
	}

	fun addPlace(place: Place) {
		if (places.none { it.id == place.id }) {
			if (places.size == 0) {
				places.add(place)
				this.place = place // auto update ids
			} else {
				if (!canAdd(place)) {
					throw IllegalArgumentException("Place ${place.id} can't be added to location due to limitations (parentCity.id)")
				}
				places.add(place)
			}
		}
	}

	fun canAdd(place: Place): Boolean {
		return place.parentCity.id == cityId
	}

	/**
	 * Update IDs to faster indexing in DB
	 */
	fun updateIds() {
		isReal = place.isReal
		placeId = place.id
		cityId = place.parentCity.id
		regionId = place.parentCity.parentRegion.id
	}

	var streetName: String? = null
	var streetType: String? = null
	var building: String? = null
	var buildingSection: String? = null
	var territory: String? = null
	var postCode: String? = null
	var district: String? = null

	var comment: String? = null

	var addressString: String? = null
		get() {
			return field ?: Stream.of(streetType, streetName, building, buildingSection, territory)
				.map { it ?: "" }.reduce { s1, s2 -> "$s1 $s2" }.orElse("Нет адреса.")
		}

	var contactInfo: String? = null
	var workHours: String? = "8:00 - 22:00, Каждый день"

	var partnerId: String? = null

	override var created: Long? = System.currentTimeMillis()
	override var updated: Long? = System.currentTimeMillis()
	override var visible: Boolean? = true

	fun setPartner(partner: Partner) {
		this.partnerId = partner.id
		this.category = partner.mainCategory
		this.marker = this.category?.marker
	}

}