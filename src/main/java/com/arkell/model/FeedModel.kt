package com.arkell.model

import com.arkell.api.OfferAPI
import com.arkell.entity.misc.Platform
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FeedModel(
		private val newsModel: NewsModel,
		private val offerModel: OfferAPI) {

	@Transactional
	fun getNewsFeed(token: String, platform: Platform = Platform.app): Map<String, Any> {
		return mapOf(
				"news" to newsModel.listBy(actual = true, pageSize = 8, page = 0, platform = platform).content,
				"offers" to offerModel.recommended(token, platform, 0, 5, featured = true).content
		)
	}

}