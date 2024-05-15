/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.mail

import com.mailjet.client.ClientOptions
import com.mailjet.client.MailjetClient

object MailjetService {
    fun createMailjetClient(mailjetApiKey: String, mailjetSecretKey: String): MailjetClient =
        MailjetClient(
            ClientOptions.builder()
                .apiKey(mailjetApiKey)
                .apiSecretKey(mailjetSecretKey)
                .build()
        )
}
