package com.arkell.util.objects

object Excludes {

	val default = arrayOf("id", "created", "updated")

	val partner = default + arrayOf("featured")

	var user = default + arrayOf("visible", "newsSubscribed", "mail")

}