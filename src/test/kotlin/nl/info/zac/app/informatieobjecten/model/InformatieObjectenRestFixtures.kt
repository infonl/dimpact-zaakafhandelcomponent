/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.informatieobjecten.model

import net.atos.zac.app.informatieobjecten.model.RESTFileUpload
import net.atos.zac.app.informatieobjecten.model.RESTInformatieobjectZoekParameters
import net.atos.zac.app.informatieobjecten.model.RestEnkelvoudigInformatieObjectVersieGegevens
import net.atos.zac.app.informatieobjecten.model.RestEnkelvoudigInformatieobject
import net.atos.zac.app.informatieobjecten.model.RestInformatieobjecttype
import nl.info.client.zgw.drc.model.generated.StatusEnum
import nl.info.client.zgw.ztc.model.generated.VertrouwelijkheidaanduidingEnum
import java.time.LocalDate
import java.util.UUID

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
) = RestEnkelvoudigInformatieobject().apply {
    this.uuid = uuid
    this.status = status
    this.vertrouwelijkheidaanduiding = vertrouwelijkheidaanduiding
    this.creatiedatum = creatieDatum
    this.auteur = auteur
    this.taal = taal
    this.informatieobjectTypeUUID = informatieobjectTypeUUID
    this.file = file
    this.bestandsnaam = bestandsNaam
    this.formaat = formaat
    this.indicatieGebruiksrecht = indicatieGebruiksrecht ?: false
}

fun createRESTFileUpload(
    file: ByteArray = "fakeFile".toByteArray(),
    fileSize: Long = 123L,
    filename: String = "fakeFilename",
    type: String = "fakeType"
) = RESTFileUpload().apply {
    this.file = file
    this.filename = filename
    this.fileSize = fileSize
    this.type = type
}

fun createRestInformatieobjecttype(
    uuid: UUID = UUID.randomUUID(),
    omschrijving: String = "fakeOmschrijving",
    vertrouwelijkheidaanduiding: String = VertrouwelijkheidaanduidingEnum.OPENBAAR.name,
    concept: Boolean = false
) = RestInformatieobjecttype().apply {
    this.uuid = uuid
    this.omschrijving = omschrijving
    this.vertrouwelijkheidaanduiding = vertrouwelijkheidaanduiding
    this.concept = concept
}

@Suppress("LongParameterList")
fun createRestEnkelvoudigInformatieObjectVersieGegevens(
    uuid: UUID = UUID.randomUUID(),
    zaakUuid: UUID = UUID.randomUUID(),
    bestandsnaam: String = "fakeFile.txt",
    file: ByteArray = "fakeFile".toByteArray(),
    formaat: String = "fakeType",
    informatieobjectTypeUUID: UUID = UUID.randomUUID(),
    vertrouwelijkheidaanduiding: String = VertrouwelijkheidaanduidingEnum.OPENBAAR.name
) = RestEnkelvoudigInformatieObjectVersieGegevens().apply {
    this.uuid = uuid
    this.zaakUuid = zaakUuid
    this.bestandsnaam = bestandsnaam
    this.formaat = formaat
    this.file = file
    this.informatieobjectTypeUUID = informatieobjectTypeUUID
    this.vertrouwelijkheidaanduiding = vertrouwelijkheidaanduiding
}

fun createRESTInformatieobjectZoekParameters(
    informatieobjectUUIDs: List<UUID>? = listOf(UUID.randomUUID(), UUID.randomUUID()),
    zaakUuid: UUID = UUID.randomUUID(),
    besluittypeUuid: UUID = UUID.randomUUID(),
    gekoppeldeZaakDocumenten: Boolean = false
) = RESTInformatieobjectZoekParameters().apply {
    this.informatieobjectUUIDs = informatieobjectUUIDs
    this.zaakUUID = zaakUuid
    this.besluittypeUUID = besluittypeUuid
    this.gekoppeldeZaakDocumenten = gekoppeldeZaakDocumenten
}
