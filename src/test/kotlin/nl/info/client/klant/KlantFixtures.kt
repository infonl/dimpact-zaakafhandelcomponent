/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.klant

import nl.info.client.klant.model.DigitaalAdres
import nl.info.client.klant.model.DigitaalAdresForeignKey
import nl.info.client.klant.model.ExpandBetrokkene
import nl.info.client.klant.model.SoortDigitaalAdresEnum
import java.net.URI
import java.util.UUID

fun createDigitalAddresses(
    phone: String = "0612345678",
    email: String = "test@example.com"
) = listOf(
    createDigitalAddress(
        uri = URI("https://example.com/fakeUr"),
        address = phone,
        soortDigitaalAdres = SoortDigitaalAdresEnum.TELEFOONNUMMER
    ),
    createDigitalAddress(
        uri = URI("https://example.com/fakeUri2"),
        address = email,
        soortDigitaalAdres = SoortDigitaalAdresEnum.EMAIL
    )
)

fun createDigitalAddress(
    uuid: UUID = UUID.randomUUID(),
    uri: URI = URI("https://example.com/fakeUri"),
    address: String = "dummyAddress",
    soortDigitaalAdres: SoortDigitaalAdresEnum = SoortDigitaalAdresEnum.TELEFOONNUMMER
) = DigitaalAdres(uuid, uri).apply {
    this.soortDigitaalAdres = soortDigitaalAdres
    this.adres = address
}

fun createDigitaalAdresForeignKey(
    uuid: UUID = UUID.randomUUID(),
    uri: URI = URI("https://example.com/fakeUri")
) = DigitaalAdresForeignKey(uri).apply {
    this.uuid = uuid
}

fun createExpandBetrokkene(
    uuid: UUID = UUID.randomUUID(),
    uri: URI = URI("https://example.com/fakeUri"),
    digitalAddresses: List<DigitaalAdresForeignKey> = listOf(createDigitaalAdresForeignKey()),
    fullName: String = "fakeFullName"
) = ExpandBetrokkene(uuid, uri, digitalAddresses, fullName)
