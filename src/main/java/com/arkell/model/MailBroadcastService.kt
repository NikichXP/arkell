package com.arkell.model

import com.arkell.entity.Category
import com.arkell.entity.MailBroadcast
import com.arkell.entity.UserEntity
import com.arkell.entity.geo.Region
import com.arkell.model.internal.MailService
import com.arkell.repo.MailSubscriptionRepo
import kotlinx.coroutines.experimental.launch
import org.springframework.stereotype.Service

@Service
class MailBroadcastService(
		val mailSubscriptionRepo: MailSubscriptionRepo,
		val mailService: MailService) {

	fun broadcast(broadcast: MailBroadcast) {
		val mails = mailSubscriptionRepo.findAll { root, _, cb ->
			var result = cb.conjunction()

			if (broadcast.category != null) {
				result = cb.and(result, cb.or(
						cb.equal(root.get<Category>("category"), broadcast.category),
						cb.isNull(root.get<Category>("category"))
				))
			}

			broadcast.regions.forEach {
				result = cb.and(result, cb.or(
						cb.equal(root.get<Region>("region"), it),
						cb.isNull(root.get<Region>("region"))
				))
			}

			if (broadcast.gender != null) {
				result = cb.and(result, cb.equal(root.get<UserEntity.Gender>("gender"), broadcast.gender))
			}

			return@findAll result
		}.map { it.mail }

		mails.forEach {
			launch { mailService.sendMail(subject = broadcast.topic, to = *arrayOf(it), body = broadcast.content) }
		}
	}

}