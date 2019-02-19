package com.arkell.entity

import com.arkell.util.IDGenerator
import javax.persistence.Id
import javax.persistence.Inheritance
import javax.persistence.InheritanceType

@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
abstract class Saveable {

	@Id
	open var id: String = IDGenerator.longId()

	abstract var created: Long?
	abstract var updated: Long?
	abstract var visible: Boolean?

}

@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
abstract class VisibleSaveable : Saveable() {

	override var visible: Boolean? = showApp ?: false && showWeb ?: false

	abstract var publishedWeb: Long?
	abstract var publishedApp: Long?
	abstract var featuredApp: Boolean?
	abstract var featuredWeb: Boolean?
	abstract var showApp: Boolean?
	abstract var showWeb: Boolean?

}