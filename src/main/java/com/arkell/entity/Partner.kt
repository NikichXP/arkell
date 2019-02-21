package com.arkell.entity

import com.arkell.entity.geo.City
import com.arkell.util.IDGenerator
import javax.persistence.*

@Entity(name = "partner")
class Partner(var title: String,
              var legalName: String,
              var INN: String?,
              var organisationForm: String?,
              var phone: String?,
              var email: String?,
              var sellType: String = "Retail",
              var website: String?,
              var shopsCount: Int? = 1) : VisibleSaveable() {

	constructor() : this("", "", "", "", "", "", "", "", 0)

	@Id
	override var id: String = IDGenerator.longId()

	@ElementCollection
	var locations: MutableList<String> = mutableListOf()

	@OneToMany(cascade = [(CascadeType.ALL)])
	var contactPersons: MutableList<ContactPerson> = mutableListOf()

	@OneToMany(cascade = [CascadeType.ALL])
	var promocodes: MutableList<Promocode> = mutableListOf()

	@ManyToMany
	var categories: MutableList<Category> = mutableListOf()
	@ManyToOne
	var mainCategory: Category? = null

	@ManyToOne
	var city: City? = null
	@ManyToMany
	var cities = mutableListOf<City>()

	@Column(columnDefinition = "TEXT")
	var about: String? = null

	var isGlobal: Boolean? = false

	var linkName: String? = null

	var logo: String = "logo.jpg"
	var status = Status.PENDING
	var priority = 1

	var resident: Boolean? = true
	var url: String? = null
	var urlEnabled: Boolean? = null
		set(value) {
			field = value ?: false
		}

	var promocode: String? = null

	//service
	var hasLocations: Boolean? = false
		set(value) {
			field = value ?: true
		}

	override var created: Long? = System.currentTimeMillis()
	override var updated: Long? = System.currentTimeMillis()
	override var publishedWeb: Long? = System.currentTimeMillis()
	override var publishedApp: Long? = System.currentTimeMillis()
	override var featuredApp: Boolean? = false
	override var featuredWeb: Boolean? = false
	override var showApp: Boolean? = false
	override var showWeb: Boolean? = false

	enum class Status {
		BLOCKED, PENDING, APPROVED
	}

	@Entity
	data class ContactPerson(var name: String, var phone: String, var mail: String, var position: String) {
		@Id
		var id: String = IDGenerator.shortId()

		constructor() : this("", "", "", "")
	}
}
