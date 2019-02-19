package com.arkell.util

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort

class OffsetPageRequest(page: Int, pageSize: Int, sort: Sort = Sort.unsorted()) : PageRequest(page, pageSize, sort) {

	private var offset: Long = 0

	fun withOffset(offset: Long): OffsetPageRequest = apply {
		this.offset = offset
	}

	override fun getOffset(): Long {
		return offset
	}


}
