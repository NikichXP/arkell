package com.arkell.entity.misc

import com.arkell.entity.Saveable
import com.arkell.util.IDGenerator
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import javax.persistence.Cacheable
import javax.persistence.Entity
import javax.persistence.Id

@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
class CardOffer(
		var url: String,
		var img: String,
		var text: String,
		var button: String,
		var isActive: Boolean) : Saveable() {

	@Id
	override var id: String = IDGenerator.shortId()

	constructor() : this("", "", "", "", false)

	override var created: Long? = System.currentTimeMillis()
	override var updated: Long? = System.currentTimeMillis()
	override var visible: Boolean? = true

}