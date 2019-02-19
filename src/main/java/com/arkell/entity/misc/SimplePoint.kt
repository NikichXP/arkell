package com.arkell.entity.misc

data class SimplePoint(
		var id: String,
		var cityId: String,
		var lat: Double,
		var lon: Double,
		var marker: String,
		var categoryId: String,
		var partnerName: String) {
}