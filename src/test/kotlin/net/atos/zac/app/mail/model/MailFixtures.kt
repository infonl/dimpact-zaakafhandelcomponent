/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.mail.model

fun createRESTMailGegevens(
    verzender: String = "from@example.com",
    ontvanger: String = "to@example.com",
) = RESTMailGegevens().apply {
    this.verzender = verzender
    this.ontvanger = ontvanger
}
