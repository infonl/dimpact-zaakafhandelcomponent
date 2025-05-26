/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.client.zgw.drc.model

import nl.info.client.zgw.drc.model.generated.BestandsDeel
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObjectCreateLockRequest
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObjectWithLockRequest
import nl.info.client.zgw.drc.model.generated.StatusEnum
import nl.info.client.zgw.drc.model.generated.VertrouwelijkheidaanduidingEnum
import java.net.URI
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Suppress("LongParameterList")
fun createEnkelvoudigInformatieObject(
    uuid: UUID = UUID.randomUUID(),
    url: URI = URI("http://example.com/$uuid"),
    versie: Int = 1234,
    beginRegistratie: OffsetDateTime = OffsetDateTime.now(),
    inhoud: URI = URI("http://example.com/${UUID.randomUUID()}"),
    locked: Boolean = false,
    bestandsdelen: List<BestandsDeel> = emptyList(),
    indicatieGebruiksrecht: Boolean? = null,
    vertrouwelijkheidaanduiding: VertrouwelijkheidaanduidingEnum? = VertrouwelijkheidaanduidingEnum.CONFIDENTIEEL,
    status: StatusEnum = StatusEnum.IN_BEWERKING,
    ontvangstdatum: LocalDate? = null,
    formaat: String = "fakeformaat",
    informatieObjectType: URI = URI("http://example.com/informatieobjecttype/${UUID.randomUUID()}"),
    bestandsomvang: Int = 1234,
) = EnkelvoudigInformatieObject(
    url,
    versie,
    beginRegistratie,
    locked,
    bestandsdelen
).apply {
    this.indicatieGebruiksrecht = indicatieGebruiksrecht
    this.inhoud(inhoud)
    this.vertrouwelijkheidaanduiding = vertrouwelijkheidaanduiding
    this.status = status
    this.ontvangstdatum = ontvangstdatum
    this.formaat = formaat
    this.informatieobjecttype = informatieObjectType
    this.bestandsomvang = bestandsomvang
}

fun createEnkelvoudigInformatieObjectCreateLockRequest(
    url: URI = URI("http://example.com/${UUID.randomUUID()}"),
    bronorganisatie: String = "123456789",
    creatiedatum: LocalDate = LocalDate.now(),
    titel: String = "fakeTitle",
    inhoud: String = "fakeContent"
) = EnkelvoudigInformatieObjectCreateLockRequest().apply {
    this.link = url
    this.bronorganisatie = bronorganisatie
    this.creatiedatum = creatiedatum
    this.titel = titel
    this.inhoud = inhoud
}

fun createEnkelvoudigInformatieObjectWithLockRequest(
    url: URI = URI("http://example.com/${UUID.randomUUID()}"),
    bronorganisatie: String = "123456789",
    creatiedatum: LocalDate = LocalDate.now(),
    titel: String = "fakeTitle",
    inhoud: String = "fakeContent"
) = EnkelvoudigInformatieObjectWithLockRequest().apply {
    this.link = url
    this.bronorganisatie = bronorganisatie
    this.creatiedatum = creatiedatum
    this.titel = titel
    this.inhoud = inhoud
}
