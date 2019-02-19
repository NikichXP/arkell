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
class City(var name: String, @ManyToOne var parentRegion: Region) : Saveable() {

	constructor() : this("", Def.get())

	@Id
	override var id: String = IDGenerator.shortId()

	@ElementCollection(fetch = FetchType.EAGER)
	var places = mutableSetOf<String>()

	var geoX: Double? = null
	var geoY: Double? = null

	override var created: Long? = System.currentTimeMillis()
	override var updated: Long? = System.currentTimeMillis()
	override var visible: Boolean? = true

}
