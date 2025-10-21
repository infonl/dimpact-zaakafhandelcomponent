/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.klant

import nl.info.client.klant.model.BetrokkeneForeignKey
import nl.info.client.klant.model.CategorieRelatie
import nl.info.client.klant.model.CategorieRelatieForeignKey
import nl.info.client.klant.model.DigitaalAdres
import nl.info.client.klant.model.DigitaalAdresForeignKey
import nl.info.client.klant.model.ExpandBetrokkene
import nl.info.client.klant.model.ExpandPartij
import nl.info.client.klant.model.ExpandPartijAllOfExpand
import nl.info.client.klant.model.PaginatedExpandPartijList
import nl.info.client.klant.model.PartijForeignKey
import nl.info.client.klant.model.PartijIdentificator
import nl.info.client.klant.model.PartijIdentificatorForeignkey
import nl.info.client.klant.model.PartijIdentificatorGroepType
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

@Suppress("LongParameterList")
fun createExpandPartij(
    uuid: UUID = UUID.randomUUID(),
    uri: URI = URI("https://example.com/fakeUri"),
    betrokkenen: List<BetrokkeneForeignKey> = emptyList(),
    categorieRelaties: List<CategorieRelatieForeignKey> = emptyList(),
    vertegenwoordigden: List<PartijForeignKey> = emptyList(),
    nummer: String = "1234",
    partijIdentificatoren: List<PartijIdentificator> = listOf(createPartijIdentificator()),
    digitaleAdressen: List<DigitaalAdresForeignKey> = listOf(createDigitaalAdresForeignKey()),
    expand: ExpandPartijAllOfExpand = createExpandPartijAllOfExpand()
) = ExpandPartij(
    uuid,
    uri,
    betrokkenen,
    categorieRelaties,
    vertegenwoordigden
).apply {
    this.nummer = nummer
    this.partijIdentificatoren = partijIdentificatoren
    this.digitaleAdressen = digitaleAdressen
    this.expand = expand
}

fun createExpandPartijAllOfExpand(
    betrokkenen: List<ExpandBetrokkene>? = null,
    categorieRelaties: List<CategorieRelatie>? = null,
    digitaleAdressen: List<DigitaalAdres> = listOf(createDigitalAddress())
) = ExpandPartijAllOfExpand(betrokkenen, categorieRelaties).apply {
    this.digitaleAdressen = digitaleAdressen
}

fun createPaginatedExpandPartijList(
    expandPartijen: List<ExpandPartij> = listOf(createExpandPartij())
) = PaginatedExpandPartijList().apply {
    this.results = expandPartijen
    this.count = expandPartijen.size
}

fun createPartijIdentificator(
    uuid: UUID = UUID.randomUUID(),
    uri: URI = URI("https://example.com/fakeUri"),
    partijIdentificator: PartijIdentificatorGroepType? = createPartijIdentificatorGroepType(),
    subIdentificatorVan: PartijIdentificatorForeignkey? = null
) = PartijIdentificator(uri).apply {
    this.uuid = uuid
    this.partijIdentificator = partijIdentificator
    this.subIdentificatorVan = subIdentificatorVan
}

fun createPartijIdentificatorForeignkey(
    uri: URI = URI("https://example.com/fakeUri"),
    uuid: UUID = UUID.randomUUID()
) = PartijIdentificatorForeignkey(uri).apply {
    this.uuid = uuid
}

fun createPartijIdentificatorGroepType(
    codeObjecttype: String = "fakeCodeObjecttype",
    codeRegister: String = "fakeCodeRegister",
    codeSoortObjectId: String = "fakeCodeSoortObjectId",
    objectId: String = "fakeObjectId"
) = PartijIdentificatorGroepType().apply {
    this.codeObjecttype = codeObjecttype
    this.codeRegister = codeRegister
    this.codeSoortObjectId = codeSoortObjectId
    this.objectId = objectId
}
