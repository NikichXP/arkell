package com.arkell.model

import com.arkell.model.internal.ConfigService
import com.arkell.repo.CatalogRepo
import com.google.gson.Gson
import kotlinx.coroutines.experimental.launch
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

@Service
class ConfigModel(
		val configService: ConfigService,
		private val catalogRepo: CatalogRepo) {

	val gson = Gson()
	private final val config = ConfigParams.CONFIG.value

	var feedbackMail: String = ""
		set(value) {
			configService.addParam(config, ConfigParams.MAIL.value, value)
			field = value
		}

	var cardUrl: String = ""
		set(value) {
			configService.addParam(config, ConfigParams.CARD_URL.value, value)
			field = value
		}

	var about: MutableMap<String, String> = configService.getConfig(ConfigParams.ABOUT.value).params
		set(value) {
			field = value
			saveAbout()
		}

	var markerList: MutableList<String> = mutableListOf()
		set(value) {
			field = value
			saveMarkers()
		}

	var feedbackThemes: MutableList<String> = mutableListOf()
		set(value) {
			field = value
			saveFeedbackThemes()
		}

	@PostConstruct
	fun postConstruct() = launch {
		feedbackMail = configService.getParam(config, ConfigParams.MAIL.value, "mailto@admins.com")
		cardUrl = configService.getParam(config, ConfigParams.CARD_URL.value, "https://foo.bar/hehe")
		markerList = gson.fromJson(configService.getParam(config, ConfigParams.MARKER.value, "[\"logo.jpg\"]"),
				List::class.java).map { it.toString() }.toMutableList()
		feedbackThemes = toList(configService.getParam(config, ConfigParams.FEEDBACK.value,
				"""["Жалоба", "Предложение"]"""))
			.toMutableList()

		if (about.isEmpty()) {
			about = mutableMapOf(
					"text" to "Lorem Ipsum",
					"about" to "We are the champions"
			)
		}
	}

	fun addMarker(marker: String) = markerList.also {
		it.add(marker)
		saveMarkers()
	}

	fun removeMarker(marker: String) = markerList.also {
		it.remove(marker)
		saveMarkers()
	}

	fun saveMarkers() = markerList.also { configService.addParam(config, ConfigParams.MARKER.value, gson.toJson(it)) }

	fun saveFeedbackThemes() = feedbackThemes.also { configService.addParam(config, ConfigParams.FEEDBACK.value, gson.toJson(it)) }

	fun addAbout(key: String, value: String) = about.also {
		it[key] = value
		saveAbout()
	}

	fun removeAbout(key: String) = about.also {
		it.remove(key)
		saveAbout()
	}

	fun saveAbout(): Map<String, *> =
			configService.getConfig(ConfigParams.ABOUT.value).also {
				it.params = about
				configService.save(it)
			}.params

	private fun toList(string: String): List<String> {
		return gson.fromJson(string, List::class.java).map { it.toString() }.toMutableList()
	}

	private enum class ConfigParams(val value: String) {
		MAIL("mail"),
		ABOUT("about"),
		CONFIG("config"),
		MARKER("marker"),
		FEEDBACK("feedback"),
		CARD_URL("card-url"),
		ABOUT_TEXT("about-text")
	}
}