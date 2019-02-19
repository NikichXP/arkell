package com.arkell.entity

import com.arkell.util.IDGenerator
import com.arkell.util.randomOrNull
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import javax.persistence.Cacheable
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.Id

@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
class Category(var name: String,
               var icon: String) : Saveable() {

	/** for JPA */
	constructor() : this("", "")

	@Id
	override var id: String = IDGenerator.longId()

	var title: String? = null
	var marker: String = "group-122.png" // 121 - 139
	@ElementCollection
	var defaultImg: MutableList<String> = mutableListOf("default.jpg")
	@ElementCollection
	var imagesApp: MutableList<String> = mutableListOf("default.jpg")
	@ElementCollection
	var imagesWeb: MutableList<String> = mutableListOf("default.jpg")

	var randomImage: String?
		set(value) {}
		get() = imagesWeb.randomOrNull() ?: defaultImg.randomOrNull()


	override var created: Long? = System.currentTimeMillis()
	override var updated: Long? = System.currentTimeMillis()
	override var visible: Boolean? = true

}