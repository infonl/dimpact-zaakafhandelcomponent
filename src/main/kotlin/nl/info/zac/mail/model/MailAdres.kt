/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.mail.model

import jakarta.json.bind.annotation.JsonbProperty
import jakarta.mail.Address
import jakarta.mail.internet.InternetAddress
import net.atos.zac.util.ValidationUtil

data class MailAdres(
    @field:JsonbProperty("Email") val email: String,
    @field:JsonbProperty("Name") val name: String?
) {
    init {
        require(ValidationUtil.isValidEmail(email)) { "Email '$email' is not valid" }
    }

    fun toAddress(): Address = InternetAddress(email, name)

    override fun toString() = "$email ($name)"
}
