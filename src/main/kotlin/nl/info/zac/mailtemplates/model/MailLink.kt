/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.mailtemplates.model

import org.apache.commons.lang3.StringUtils
import org.apache.commons.text.StringEscapeUtils
import java.net.URI

class MailLink(identificatie: String, url: URI, prefix: String?, suffix: String?) {
    val identificatie: String = StringEscapeUtils.escapeHtml4(identificatie)

    val url: String = StringEscapeUtils.escapeHtml4(url.toString())

    val prefix: String = StringEscapeUtils.escapeHtml4(
        prefix ?: StringUtils.EMPTY
    ).plus(if (prefix != null) " " else "")

    val suffix: String = StringEscapeUtils.escapeHtml4(
        suffix ?: StringUtils.EMPTY
    ).let { if (suffix != null) " $it" else it }

    // Make sure that what is returned is FULLY encoded HTML (no injection vulnerabilities)
    fun toHtml() = "Klik om naar $prefix<a href=\"$url\" title=\"de zaakafhandelcomponent...\">$identificatie</a>$suffix te gaan."
}
