package com.arkell.entity

import com.arkell.util.IDGenerator
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.Id

@Entity
class Catalog : Saveable() {

	@Id
	override var id: String = IDGenerator.longId()

	lateinit var title: String

	@ElementCollection
	var data = mutableListOf<String>()

	override var created: Long? = System.currentTimeMillis()
	override var updated: Long? = System.currentTimeMillis()
	override var visible: Boolean? = true
}