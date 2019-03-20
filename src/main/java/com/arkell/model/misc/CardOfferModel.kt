package com.arkell.model.misc

import com.arkell.entity.misc.CardOffer
import com.arkell.model.UpdateAction
import com.arkell.repo.misc.CardOfferRepo
import com.arkell.util.objects.Excludes
import com.arkell.util.objects.ObjectFromMapUpdater
import org.springframework.stereotype.Service

@Service
class CardOfferModel(
		override val repository: CardOfferRepo) : UpdateAction<CardOffer>() {

	fun addCardOffer(data: Map<String, String>): CardOffer = save(
			ObjectFromMapUpdater(CardOffer()).data(data).modify().apply {
				var i = 0

				while (repository.findById("card-offer-$i").isPresent) {
					i++
				}

				id = "card-offer-$i"
			})

	fun edit(id: String, data: Map<String, String>) = autoEdit(id, data, *Excludes.default) {}
	fun list(): List<CardOffer> {
		return repository.findAll()
	}


}