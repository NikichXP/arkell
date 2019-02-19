package com.arkell.model.internal

import com.arkell.entity.ConfigPair
import com.arkell.repo.ConfigPairRepo
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ConfigService(
		private val repository: ConfigPairRepo) {

	init {
		ConfigPair.configServiceInstance = this
		//		repository.save(ConfigPair("great-job").apply { params["x"] = "y" })
	}

	fun getConfig(name: String): ConfigPair = try {
		repository.findById(name).orElseGet {
			repository.save(ConfigPair(name))
		}
	} catch (e: Exception) {
		repository.save(ConfigPair(name))
	}

	@Transactional
	fun addParam(name: String, param: String, value: String) = getConfig(name).apply {
		this.params[param] = value
		save()
	}

	@Transactional
	fun getParam(name: String, param: String, defaultValue: String): String =
			getConfig(name).params[param] ?: defaultValue.apply { addParam(name, param, this) }

	@Transactional
	fun save(config: ConfigPair): ConfigPair = config.also { repository.save(it) }


}