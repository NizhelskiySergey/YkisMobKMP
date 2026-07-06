package com.ykis.ykismobkmp.domain.services

import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * [MailService] — Сервіс для автоматичної розсилки листів через Firebase Email Extension.
 */
class MailService(private val firestore: FirebaseFirestore?) {

    private val className = "MailService"

    /**
     * [sendEmail] — Надсилає лист конкретному користувачу.
     */
    fun sendEmail(
        to: String,
        subject: String,
        bodyText: String,
        htmlBody: String? = null
    ) {
        if (firestore == null || to.isBlank()) return

        val mailData = mutableMapOf<String, Any>(
            "to" to to,
            "message" to mapOf(
                "subject" to subject,
                "text" to bodyText,
                "html" to (htmlBody ?: bodyText.replace("\n", "<br>"))
            )
        )

        CoroutineScope(Dispatchers.Main).launch {
            try {
                firestore.collection("mail").add(mailData)
                println("[$className.sendEmail]: Лист для $to додано в чергу відправки.")
            } catch (e: Exception) {
                println("[$className.sendEmail_ERROR]: Помилка створення документа листа: ${e.message}")
            }
        }
    }
    
    /**
     * [sendAnnouncementNotification] — Шаблон листа для нових оголошень.
     */
    fun sendAnnouncementNotification(email: String, title: String, message: String) {
        val subject = "📢 ЮКІС: Нове оголошення"
        val body = """
            Вітаємо! 
            
            В додатку ЮКІС опубліковано нове оголошення:
            
            ${title.uppercase()}
            
            $message
            
            Дякуємо, що користуєтесь нашим сервісом!
        """.trimIndent()
        
        sendEmail(email, subject, body)
    }
}
