package com.arkell.repo.misc

import com.arkell.entity.misc.CardOffer
import org.springframework.data.jpa.repository.JpaRepository

interface CardOfferRepo : JpaRepository<CardOffer, String> {
}