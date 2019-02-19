package com.arkell.entity.geo

data class GeoRange(var x: ClosedRange<Double>, var y: ClosedRange<Double>) {

	fun test(x: Double, y: Double): Boolean =
			this.x.contains(x) && this.y.contains(y)

}