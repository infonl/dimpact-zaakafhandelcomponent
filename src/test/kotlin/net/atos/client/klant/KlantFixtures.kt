/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.klant

import net.atos.client.klant.model.DigitaalAdres
import java.net.URI
import java.util.UUID

fun createDigitalAddresses(
    phone: String,
    email: String
) = listOf(
    createDigitalAddress(
        uri = URI("https://example.com/fakeUr"),
        address = phone,
        soortDigitaalAdres = "telefoon"
    ),
    createDigitalAddress(
        uri = URI("https://example.com/fakeUri2"),
        address = email,
        soortDigitaalAdres = "email"
    )
)

fun createDigitalAddress(
    uuid: UUID = UUID.randomUUID(),
    uri: URI = URI("https://example.com/fakeUri"),
    address: String = "fakeAddress",
    soortDigitaalAdres: String = "telefoon"
) = DigitaalAdres(uuid, uri).apply {
    this.soortDigitaalAdres = soortDigitaalAdres
    this.adres = address
}
