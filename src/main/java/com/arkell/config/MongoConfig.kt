import org.springframework.boot.autoconfigure.orm.jpa.JpaBaseConfiguration

//package com.arkell.config
//
//import com.mongodb.MongoClient
//import com.mongodb.MongoClientURI
//import org.springframework.context.ApplicationContext
//import org.springframework.context.annotation.Configuration
//import org.springframework.data.mongodb.config.AbstractMongoConfiguration
//import org.springframework.data.mongodb.core.MongoTemplate
//
//
//@Configuration
//class MongoConfig(
//		applicationContext: ApplicationContext) : AbstractMongoConfiguration() {
//
//	private val client: MongoClient = MongoClient(MongoClientURI(
//			"mongodb://root:qwerty123@ds145881.mlab.com:45881/arkell"))
//
//	private val dbName = "arkell"
//
//	override fun mongoClient(): MongoClient = client
//	override fun getDatabaseName(): String = dbName
//
//	fun getMongoOperations() = MongoTemplate(mongoClient(), databaseName)
//}