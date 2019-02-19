package com.arkell.util

import com.arkell.entity.Category
import com.arkell.entity.Partner
import com.arkell.entity.geo.City
import com.arkell.entity.geo.GeoPoint
import com.arkell.entity.geo.Place
import com.arkell.entity.geo.Region

/**
 * Default value provider
 */
object Def {

	@Suppress("IMPLICIT_CAST_TO_ANY")
	inline fun <reified T> get(): T {
		return when (T::class.java) {
			City::class.java -> City()
			Region::class.java -> Region()
			Place::class.java -> Place()
			GeoPoint::class.java -> GeoPoint()
			Category::class.java -> Category()
			Partner::class.java -> Partner()
			String::class.java -> ""
			else -> TODO()
		} as T
	}

}