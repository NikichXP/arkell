package com.arkell.entity.geo

import com.arkell.entity.Saveable
import com.arkell.util.Def
import com.arkell.util.IDGenerator
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import javax.persistence.*

@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
class Place(var name: String, @ManyToOne var parentCity: City, @Embedded var point: GeoPoint) : Saveable() {

	constructor() : this("", Def.get(), Def.get())

	@Id
	override var id: String = IDGenerator.shortId()

	var type: String? = "metro"
	var isReal: Boolean = true

	var logo: String? = null

	override var created: Long? = System.currentTimeMillis()
	override var updated: Long? = System.currentTimeMillis()
	override var visible: Boolean? = true
}