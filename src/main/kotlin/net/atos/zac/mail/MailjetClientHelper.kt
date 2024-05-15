package net.atos.zac.mail

import com.mailjet.client.ClientOptions
import com.mailjet.client.MailjetClient

object MailjetClientHelper {
    fun createMailjetClient(mailjetApiKey: String?, mailjetSecretKey: String?): MailjetClient {
        return MailjetClient(
            ClientOptions.builder()
                .apiKey(mailjetApiKey)
                .apiSecretKey(mailjetSecretKey)
                .build()
        )
    }
}
