/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.informatieobjecten.model

import nl.info.client.zgw.drc.model.generated.StatusEnum
import nl.info.client.zgw.ztc.model.generated.VertrouwelijkheidaanduidingEnum
import java.time.LocalDate
import java.util.UUID

fun createRestDocumentVerzendGegevens(
    zaakUuid: UUID = UUID.randomUUID(),
    verzenddatum: LocalDate = LocalDate.now(),
    informatieobjecten: List<UUID> = listOf(UUID.randomUUID()),
    toelichting: String = "fakeToelichting",
) = RestDocumentVerzendGegevens(
    zaakUuid = zaakUuid,
    verzenddatum = verzenddatum,
    informatieobjecten = informatieobjecten,
    toelichting = toelichting
)

@Suppress("LongParameterList")
fun createRestEnkelvoudigInformatieobject(
    uuid: UUID = UUID.randomUUID(),
    status: StatusEnum = StatusEnum.IN_BEWERKING,
    vertrouwelijkheidaanduiding: String? = null,
    creatieDatum: LocalDate? = null,
    auteur: String? = null,
    taal: String? = null,
    informatieobjectTypeUUID: UUID = UUID.randomUUID(),
    file: ByteArray = "fakeFile".toByteArray(),
    bestandsNaam: String = "fakeFilename",
    formaat: String = "fakeType",
    indicatieGebruiksrecht: Boolean? = null
) = RestEnkelvoudigInformatieobject(
    uuid = uuid,
    status = status,
    vertrouwelijkheidaanduiding = vertrouwelijkheidaanduiding,
    creatiedatum = creatieDatum,
    auteur = auteur,
    taal = taal,
    informatieobjectTypeUUID = informatieobjectTypeUUID,
    formaat = formaat,
    indicatieGebruiksrecht = indicatieGebruiksrecht ?: false
).also {
    it.file = file
    it.bestandsnaam = bestandsNaam
}

fun createRestFileUpload(
    file: ByteArray = "fakeFile".toByteArray(),
    fileSize: Long = 123L,
    filename: String = "fakeFilename",
    type: String = "fakeType"
) = RestFileUpload(
    file = file,
    fileSize = fileSize,
    filename = filename,
    type = type
)

fun createRestInformatieobjecttype(
    uuid: UUID = UUID.randomUUID(),
    omschrijving: String = "fakeOmschrijving",
    vertrouwelijkheidaanduiding: String = VertrouwelijkheidaanduidingEnum.OPENBAAR.name,
    concept: Boolean = false
) = RestInformatieobjecttype(
    uuid = uuid,
    omschrijving = omschrijving,
    vertrouwelijkheidaanduiding = vertrouwelijkheidaanduiding,
    concept = concept
)

@Suppress("LongParameterList")
fun createRestEnkelvoudigInformatieObjectVersieGegevens(
    uuid: UUID = UUID.randomUUID(),
    zaakUuid: UUID = UUID.randomUUID(),
    bestandsnaam: String = "fakeFile.txt",
    file: ByteArray = "fakeFile".toByteArray(),
    formaat: String = "fakeType",
    informatieobjectTypeUUID: UUID = UUID.randomUUID(),
    vertrouwelijkheidaanduiding: String = VertrouwelijkheidaanduidingEnum.OPENBAAR.name
) = RestEnkelvoudigInformatieObjectVersieGegevens(
    uuid = uuid,
    zaakUuid = zaakUuid,
    formaat = formaat,
    informatieobjectTypeUUID = informatieobjectTypeUUID,
    vertrouwelijkheidaanduiding = vertrouwelijkheidaanduiding
).also {
    it.bestandsnaam = bestandsnaam
    it.file = file
}

fun createRestInformatieobjectZoekParameters(
    informatieobjectUUIDs: List<UUID>? = listOf(UUID.randomUUID(), UUID.randomUUID()),
    zaakUuid: UUID = UUID.randomUUID(),
    besluittypeUuid: UUID = UUID.randomUUID(),
    gekoppeldeZaakDocumenten: Boolean = false
) = RestInformatieobjectZoekParameters(
    informatieobjectUUIDs = informatieobjectUUIDs,
    zaakUUID = zaakUuid,
    besluittypeUUID = besluittypeUuid,
    gekoppeldeZaakDocumenten = gekoppeldeZaakDocumenten
)
