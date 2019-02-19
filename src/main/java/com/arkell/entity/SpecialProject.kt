package com.arkell.entity

import com.arkell.util.IDGenerator
import javax.persistence.*

@Entity
class SpecialProject : VisibleSaveable() {

	@Id
	override var id: String = IDGenerator.longId()

	var title: String = ""
	@Column(columnDefinition = "TEXT")
	var description: String = ""

	var image: String? = "default.jpg"
	var imageApp: String? = "default.jpg"

	var startDate: Long = System.currentTimeMillis()
	var endDate: Long = System.currentTimeMillis()

	@ManyToOne
	var category: Category? = null
	@ElementCollection
	var partnerList = mutableListOf<String>()
	@ElementCollection
	var offerList = mutableListOf<String>()

	var bannerWeb: String? = null
	var bannerApp: String? = null
	var featured: Boolean? = false
	var priority: Int? = 20
	var url: String? = null
	var urlEnabled: Boolean? = null
		set(value) {
			field = value ?: false
		}

	override var visible: Boolean? = true
	override var created: Long? = System.currentTimeMillis()
	override var updated: Long? = System.currentTimeMillis()
	override var publishedWeb: Long? = System.currentTimeMillis()
	override var publishedApp: Long? = System.currentTimeMillis()
	override var featuredApp: Boolean? = false
	override var featuredWeb: Boolean? = false
	override var showApp: Boolean? = false
	override var showWeb: Boolean? = false

}