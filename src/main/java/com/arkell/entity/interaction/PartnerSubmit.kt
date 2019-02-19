package com.arkell.entity.interaction

import com.arkell.entity.Saveable
import com.arkell.entity.geo.City
import com.arkell.util.IDGenerator
import org.springframework.data.jpa.repository.JpaRepository
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.ManyToOne

@Entity
class PartnerSubmit(var title: String,
                    var legalName: String,
                    var INN: String?,
                    var organisationForm: String,
                    var contactPerson: String?,
                    var phone: String = "",
                    var email: String?,
                    var sellType: String = "Retail",
                    var website: String?,
                    var shopsCount: Int? = null) : Saveable() {

	constructor() : this("", "", null, "", null, "", null,
			"", "", null)

	@Id
	override var id: String = IDGenerator.longId()

	@ManyToOne
	var city: City? = null

	override var created: Long? = System.currentTimeMillis()
	override var updated: Long? = System.currentTimeMillis()
	override var visible: Boolean? = false

}

interface PartnerSubmitRepo : JpaRepository<PartnerSubmit, String>