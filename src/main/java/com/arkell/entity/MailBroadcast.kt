package com.arkell.entity

import com.arkell.entity.geo.Region
import com.arkell.util.IDGenerator
import javax.persistence.*



@Entity
class MailBroadcast(
		var title: String,
		var topic: String,
		var banner: String,
		@Column(columnDefinition = "TEXT")
		var content: String,
		var date: Long?) : Saveable() {

	constructor() : this("", "", "", "", System.currentTimeMillis())

	@Id
	override var id: String = IDGenerator.longId()

	@ManyToOne
	var region: Region? = null
	@ManyToOne
	var category: Category? = null

	@Column(columnDefinition = "TEXT")
	var legalText = "(c) Измени этот текст до выхода на прод."
	var info: String? = null

	var gender: UserEntity.Gender? = null
	var greeting: String? = null

	@ElementCollection
	var newsList = mutableListOf<String>()
	@ElementCollection
	var offerList = mutableListOf<String>()
	@ElementCollection
	var projectList = mutableListOf<String>()

	override var created: Long? = System.currentTimeMillis()
	override var updated: Long? = System.currentTimeMillis()
	override var visible: Boolean? = true

}