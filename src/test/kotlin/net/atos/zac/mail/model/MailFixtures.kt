/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.mail.model

fun createMailAdres(
    email: String = "dummy@example.com",
    name: String = "dummyName"
) = MailAdres(
    email,
    name
)
