/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.informatieobjecten.model

import jakarta.validation.constraints.NotNull
import jakarta.ws.rs.FormParam
import nl.info.client.zgw.drc.model.generated.StatusEnum
import nl.info.zac.app.identity.model.RestUser
import nl.info.zac.app.policy.model.RestDocumentRechten
import nl.info.zac.search.model.DocumentIndicatie
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.EnumSet
import java.util.UUID

/**
 * Representation of an 'enkelvoudig informatieobject' (e.g. a document) in the ZAC REST API.
 */
open class RestEnkelvoudigInformatieobject : RestEnkelvoudigInformatieFileUpload() {
    var uuid: UUID? = null

    @field:FormParam("identificatie")
    var identificatie: String? = null

    @field:NotNull
    @field:FormParam("titel")
    var titel: String? = null

    @field:FormParam("beschrijving")
    var beschrijving: String? = null

    // not always required
    @field:FormParam("creatiedatum")
    var creatiedatum: LocalDate? = null

    @field:FormParam("registratiedatumTijd")
    var registratiedatumTijd: ZonedDateTime? = null

    @field:FormParam("ontvangstdatum")
    var ontvangstdatum: LocalDate? = null

    @field:FormParam("verzenddatum")
    var verzenddatum: LocalDate? = null

    @field:FormParam("bronorganisatie")
    var bronorganisatie: String? = null

    // not always required
    @field:FormParam("vertrouwelijkheidaanduiding")
    var vertrouwelijkheidaanduiding: String? = null

    // not always required
    @field:FormParam("auteur")
    var auteur: String? = null

    @field:FormParam("status")
    var status: StatusEnum? = null

    @field:FormParam("formaat")
    var formaat: String? = null

    @field:FormParam("bestandsomvang")
    var bestandsomvang: Long? = null

    // not always required
    @field:FormParam("taal")
    var taal: String? = null

    @field:FormParam("versie")
    var versie: Int? = null

    @field:NotNull
    @field:FormParam("informatieobjectTypeUUID")
    var informatieobjectTypeUUID: UUID? = null

    @field:FormParam("informatieobjectTypeOmschrijving")
    var informatieobjectTypeOmschrijving: String? = null

    @field:FormParam("link")
    var link: String? = null

    @field:FormParam("ondertekening")
    var ondertekening: RestOndertekening? = null

    @field:FormParam("indicatieGebruiksrecht")
    var indicatieGebruiksrecht: Boolean = false

    @field:FormParam("gelockedDoor")
    var gelockedDoor: RestUser? = null

    @field:FormParam("isBesluitDocument")
    var isBesluitDocument: Boolean = false

    @field:FormParam("rechten")
    var rechten: RestDocumentRechten? = null

    fun getIndicaties(): EnumSet<DocumentIndicatie> {
        val indicaties = EnumSet.noneOf(DocumentIndicatie::class.java)
        if (gelockedDoor != null) {
            indicaties.add(DocumentIndicatie.VERGRENDELD)
        }
        if (ondertekening != null) {
            indicaties.add(DocumentIndicatie.ONDERTEKEND)
        }
        if (indicatieGebruiksrecht) {
            indicaties.add(DocumentIndicatie.GEBRUIKSRECHT)
        }
        if (isBesluitDocument) {
            indicaties.add(DocumentIndicatie.BESLUIT)
        }
        if (verzenddatum != null) {
            indicaties.add(DocumentIndicatie.VERZONDEN)
        }
        return indicaties
    }
}
