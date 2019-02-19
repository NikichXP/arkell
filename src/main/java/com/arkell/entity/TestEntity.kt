package com.arkell.entity

import org.springframework.data.jpa.repository.JpaRepository
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id

@Entity
class TestEntity() {

	@Id
	var id: String = UUID.randomUUID().toString()

	@Column(columnDefinition = "TEXT")
	var longText: String = (1..1000).map { it.toString() }.reduce { a, b -> "$a,$b" }

	constructor(i: Int, j: Int = i + 1000) : this() {
		id = i.toString()
		longText = (i..j).map { it.toString() }.reduce { a, b -> "$a,$b" }
	}
}

interface TestRepo : JpaRepository<TestEntity, String>