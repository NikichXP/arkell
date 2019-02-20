package com.arkell.entity

import com.arkell.entity.geo.City
import com.arkell.entity.geo.Region
import com.arkell.util.IDGenerator
import javax.persistence.*

@Entity
data class News(var title: String,
                @Column(columnDefinition = "TEXT")
                var description: String?,
                var beginDate: Long,
                var endDate: Long) : VisibleSaveable() {

	constructor() : this("", "", 0, 0)

	@Id
	override var id: String = IDGenerator.shortId()

	@ElementCollection
	var offers: MutableList<String> = mutableListOf()
	@ElementCollection
	var partners: MutableList<String> = mutableListOf()

	var imageUrl: String? = null
	var imageApp: String? = null

	@Column(columnDefinition = "TEXT")
	var terms: String? = null
	var workTerms: String? = null
	var voucher: String? = null
	var previewText: String? = null
	var banner: String? = null
	var bannerApp: String? = null
	var priority: Int? = 20
	var url: String? = null
	var urlEnabled: Boolean? = null
		set(value) {
			field = value ?: false
		}

	@ManyToMany
	var cities = mutableListOf<City>()
	@ManyToMany
	var regions = mutableListOf<Region>()

	var onlyClient: Boolean? = false
	var featured: Boolean? = false

	override var created: Long? = System.currentTimeMillis()
	override var updated: Long? = System.currentTimeMillis()
	override var publishedWeb: Long? = System.currentTimeMillis()
	override var publishedApp: Long? = System.currentTimeMillis()
	override var featuredApp: Boolean? = false
	override var featuredWeb: Boolean? = false
	override var showApp: Boolean? = false
	override var showWeb: Boolean? = false


	// legacy

	var regionId: String? = null
	var cityId: String? = null

}
