package com.arkell.repo.auth

import com.arkell.entity.auth.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository

interface RefreshTokenRepo : JpaRepository<RefreshToken, String> {

}