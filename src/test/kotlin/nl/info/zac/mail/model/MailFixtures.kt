/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.mail.model

fun createMailAdres(
    email: String = "fake@example.com",
    name: String = "fakeName"
) = MailAdres(
    email,
    name
)
