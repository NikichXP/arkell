package com.arkell.entity

import com.arkell.model.internal.ConfigService
import com.arkell.util.IDGenerator
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.Id

@Entity
data class ConfigPair(@Id var id: String,
                      @ElementCollection(fetch = FetchType.EAGER)
                      var params: MutableMap<String, String> = mutableMapOf()) {

	constructor() : this(id = IDGenerator.longId(), params = mutableMapOf())

	fun save() {
		configServiceInstance.save(this)
	}

	companion object {
		lateinit var configServiceInstance: ConfigService
	}
}