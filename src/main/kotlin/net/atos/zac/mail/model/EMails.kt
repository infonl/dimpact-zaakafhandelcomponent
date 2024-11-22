/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.mail.model

import jakarta.json.bind.annotation.JsonbProperty

class EMails(@field:JsonbProperty("Messages") private var eMails: List<EMail>) {
    fun geteMails(): List<EMail> {
        return eMails
    }

    fun seteMails(eMails: List<EMail>) {
        this.eMails = eMails
    }
}
