package com.arkell.model.internal

import com.google.gson.JsonParser
import org.springframework.stereotype.Service
import java.io.File
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.Session
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

@Service
class MailService {

	private var mailAddress: String = "sdv@defa.ru"
	private var mailPassword: String = "sdv345"
	private var mailServer: String = "smtp.mail.ru"
	private var port = 25
	private var tls = true

	init {
		val json = JsonParser().parse(File(System.getProperty("user.dir") + "/conf.json").readText())
		json.asJsonObject.getAsJsonObject("mail").apply {
			mailAddress = get("address").asString
			mailPassword = get("pass").asString
			mailServer = get("server").asString
			port = get("port").asInt
			tls = try {
				get("tls").asBoolean
			} catch (e: Exception) {
				true
			}
		}
	}

	fun sendMail(subject: String, body: String, vararg to: String) {
		val props = System.getProperties()
		props["mail.smtp.starttls.enable"] = "true"
		props["mail.smtp.host"] = mailServer
		props["mail.smtp.user"] = mailAddress
		props["mail.smtp.password"] = mailPassword
		props["mail.smtp.port"] = port
		props["mail.smtp.auth"] = "true"

		val session = Session.getDefaultInstance(props)
		val message = MimeMessage(session)

		for (address in to) {
			try {
				message.setFrom(InternetAddress(mailAddress))
				message.addRecipient(Message.RecipientType.TO, InternetAddress(address))
				message.subject = subject
				message.setContent(body.replace("\n", "<br>"), "text/html; charset=utf-8")
				val transport = session.getTransport("smtp")
				transport.connect(mailServer, mailAddress, mailPassword)
				transport.sendMessage(message, message.allRecipients)
				transport.close()
			} catch (me: MessagingException) {
				me.printStackTrace()
			}
		}
	}

}