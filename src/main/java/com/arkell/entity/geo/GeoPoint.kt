package com.arkell.entity.geo

import com.arkell.util.IDGenerator
import javax.persistence.Entity
import javax.persistence.Id

@Entity
class GeoPoint(var x: Double, var y: Double) {

	constructor() : this(0.0, 0.0)

	@Id
	var id: String = IDGenerator.longId()

	operator fun rangeTo(other: GeoPoint): GeoRange {
		return GeoRange(x = this.x..other.x, y = this.y..other.y)
	}


}

