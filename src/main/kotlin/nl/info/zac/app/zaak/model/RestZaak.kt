/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.model

import jakarta.json.bind.annotation.JsonbProperty
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import nl.info.client.zgw.zrc.model.generated.VertrouwelijkheidaanduidingEnum
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.zrc.model.generated.Zaak.OMSCHRIJVING_MAX_LENGTH
import nl.info.client.zgw.zrc.model.generated.Zaak.TOELICHTING_MAX_LENGTH
import nl.info.client.zgw.ztc.model.generated.ZaakType
import nl.info.zac.app.identity.model.RestGroup
import nl.info.zac.app.identity.model.RestUser
import nl.info.zac.app.klant.model.klant.IdentificatieType
import nl.info.zac.app.policy.model.RestZaakRechten
import nl.info.zac.search.model.ZaakIndicatie
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.time.LocalDate
import java.util.EnumSet
import java.util.UUID

@NoArgConstructor
@AllOpen
data class RestZaak(
    var archiefActiedatum: LocalDate?,
    var archiefNominatie: String?,
    var behandelaar: RestUser?,
    var besluiten: List<RestDecision>?,
    var bronorganisatie: String?,
    var communicatiekanaal: String?,
    var duurVerlenging: String?,
    var einddatumGepland: LocalDate?,
    var einddatum: LocalDate?,
    var gerelateerdeZaken: List<RestGerelateerdeZaak>?,
    @field:Valid
    var groep: RestGroup?,
    var identificatie: String,
    var indicaties: EnumSet<ZaakIndicatie>,
    var initiatorIdentificatie: String?,
    var kvkNummer: String?,
    var vestigingsNummer: String?,
    var initiatorIdentificatieType: IdentificatieType?,

    @get:JsonbProperty("isOpgeschort")
    var isOpgeschort: Boolean,

    @get:JsonbProperty("isEerderOpgeschort")
    var isEerderOpgeschort: Boolean,

    @get:JsonbProperty("isOpen")
    var isOpen: Boolean,

    @get:JsonbProperty("isHeropend")
    var isHeropend: Boolean,

    @get:JsonbProperty("isHoofdzaak")
    var isHoofdzaak: Boolean,

    @get:JsonbProperty("isDeelzaak")
    var isDeelzaak: Boolean,

    @get:JsonbProperty("isBesluittypeAanwezig")
    var isBesluittypeAanwezig: Boolean,

    @get:JsonbProperty("isInIntakeFase")
    var isInIntakeFase: Boolean,

    /**
     * Indicates whether the case is driven using a BPMN process or not.
     * If not, it is in most cases driven by the ZAC CMMN model.
     */
    @get:JsonbProperty("isProcesGestuurd")
    var isProcesGestuurd: Boolean,

    @get:JsonbProperty("isVerlengd")
    var isVerlengd: Boolean,

    var kenmerken: List<RESTZaakKenmerk>?,

    @field:Size(max = OMSCHRIJVING_MAX_LENGTH)
    var omschrijving: String,

    var publicatiedatum: LocalDate?,
    var rechten: RestZaakRechten,
    var redenOpschorting: String?,
    var redenVerlenging: String?,
    var registratiedatum: LocalDate?,
    var resultaat: RestZaakResultaat?,
    var startdatum: LocalDate?,
    var status: RestZaakStatus?,

    @field:Size(max = TOELICHTING_MAX_LENGTH)
    var toelichting: String?,

    var uiterlijkeEinddatumAfdoening: LocalDate?,
    var uuid: UUID,
    var verantwoordelijkeOrganisatie: String?,
    var vertrouwelijkheidaanduiding: String?,
    var zaakdata: Map<String, Any>?,
    var zaakgeometrie: RestGeometry?,
    var zaaktype: RestZaaktype
)

fun RestZaak.toZaak(
    zaaktype: ZaakType,
    bronOrganisatie: String,
    verantwoordelijkeOrganisatie: String
) = Zaak(
    null,
    this.uuid,
    this.einddatum,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null
).apply {
    this.bronorganisatie = bronOrganisatie
    this.verantwoordelijkeOrganisatie = verantwoordelijkeOrganisatie
    this.startdatum = this@toZaak.startdatum
    this.zaaktype = zaaktype.url
    this.communicatiekanaalNaam = this@toZaak.communicatiekanaal
    this.omschrijving = this@toZaak.omschrijving
    this.toelichting = this@toZaak.toelichting
    this.registratiedatum = LocalDate.now()
    this.vertrouwelijkheidaanduiding = this@toZaak.vertrouwelijkheidaanduiding?.let {
        // convert this enum to uppercase in case the client sends it in lowercase
        VertrouwelijkheidaanduidingEnum.valueOf(it.uppercase())
    }
    this.zaakgeometrie = this@toZaak.zaakgeometrie?.toGeoJSONGeometry()
}

/**
 * Converts a RestZaak to a Zaak object suitable for PATCH requests.
 * This is used when updating an existing case with partial data.
 */
fun RestZaak.toPatchZaak() = Zaak().apply {
    toelichting = this@toPatchZaak.toelichting
    omschrijving = this@toPatchZaak.omschrijving
    startdatum = this@toPatchZaak.startdatum
    einddatumGepland = this@toPatchZaak.einddatumGepland
    uiterlijkeEinddatumAfdoening = this@toPatchZaak.uiterlijkeEinddatumAfdoening
    vertrouwelijkheidaanduiding = this@toPatchZaak.vertrouwelijkheidaanduiding?.let {
        // convert this enum to uppercase in case the client sends it in lowercase
        VertrouwelijkheidaanduidingEnum.valueOf(it.uppercase())
    }
    communicatiekanaalNaam = this@toPatchZaak.communicatiekanaal
    zaakgeometrie = this@toPatchZaak.zaakgeometrie?.toGeoJSONGeometry()
}
