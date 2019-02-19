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
class Region(var name: String) : Saveable() {

	constructor() : this("NoName")

	@Id
	override var id: String = IDGenerator.shortId()

	@ElementCollection(fetch = FetchType.EAGER)
	var cities = mutableSetOf<String>()

	override var created: Long? = System.currentTimeMillis()
	override var updated: Long? = System.currentTimeMillis()
	override var visible: Boolean? = true
}