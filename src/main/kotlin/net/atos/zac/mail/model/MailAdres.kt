/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.mail.model

import jakarta.json.bind.annotation.JsonbProperty

class MailAdres @JvmOverloads constructor(
    @field:JsonbProperty("Email") var email: String, @field:JsonbProperty(
        "Name"
    ) var name: String? = null
)
