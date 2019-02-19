package com.arkell.repo

import com.arkell.entity.ConfigPair
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Transactional
@Repository
interface ConfigPairRepo : JpaRepository<ConfigPair, String> {
}