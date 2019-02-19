package com.arkell.entity

import com.arkell.entity.geo.City
import com.arkell.entity.geo.Region
import com.arkell.util.IDGenerator
import java.time.LocalDateTime
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.ManyToOne

@Entity
class Banner(var img: String, var text: String?, var link: String = "https://google.com") : VisibleSaveable() {


	constructor() : this("", null)

	var startDate: LocalDateTime = LocalDateTime.now().minusDays(1)
	var endDate: LocalDateTime = LocalDateTime.now()

	var displayName: String? = null
	var title: String? = null

	@ManyToOne
	var city: City? = null
	@ManyToOne
	var region: Region? = null

	@Id
	override var id: String = IDGenerator.longId()

	override var created: Long? = System.currentTimeMillis()
	override var updated: Long? = System.currentTimeMillis()
	override var publishedWeb: Long? = System.currentTimeMillis()
	override var publishedApp: Long? = System.currentTimeMillis()
	override var featuredApp: Boolean? = false
	override var featuredWeb: Boolean? = false
	override var showApp: Boolean? = false
	override var showWeb: Boolean? = false

}