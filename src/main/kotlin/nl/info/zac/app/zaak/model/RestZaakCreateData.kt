/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.model

import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import nl.info.client.zgw.zrc.model.generated.VertrouwelijkheidaanduidingEnum
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.zrc.model.generated.Zaak.OMSCHRIJVING_MAX_LENGTH
import nl.info.client.zgw.zrc.model.generated.Zaak.TOELICHTING_MAX_LENGTH
import nl.info.client.zgw.ztc.model.generated.ZaakType
import nl.info.zac.app.identity.model.RestGroup
import nl.info.zac.app.identity.model.RestUser
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.time.LocalDate

@NoArgConstructor
@AllOpen
data class RestZaakCreateData(
    var behandelaar: RestUser?,
    var bronorganisatie: String?,
    var communicatiekanaal: String?,
    var einddatumGepland: LocalDate?,
    var einddatum: LocalDate?,
    var gerelateerdeZaken: List<RestGerelateerdeZaak>?,
    @field:Valid
    var groep: RestGroup?,
    var initiatorIdentificatie: BetrokkeneIdentificatie?,

    @field:Size(max = OMSCHRIJVING_MAX_LENGTH)
    var omschrijving: String,

    var publicatiedatum: LocalDate?,
    var registratiedatum: LocalDate?,
    var startdatum: LocalDate?,

    @field:Size(max = TOELICHTING_MAX_LENGTH)
    var toelichting: String?,

    var uiterlijkeEinddatumAfdoening: LocalDate?,
    var vertrouwelijkheidaanduiding: String?,
    var zaakgeometrie: RestGeometry?,
    var zaaktype: RestZaaktype
)

fun RestZaakCreateData.toZaak(
    zaaktype: ZaakType,
    bronOrganisatie: String,
    verantwoordelijkeOrganisatie: String
) = Zaak(
    null,
    null,
    this.einddatum,
    null,
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
 * Converts rest zaak create data to a zaak object suitable for PATCH requests.
 * This is used when updating an existing case with partial data.
 */
fun RestZaakCreateData.toPatchZaak() = Zaak().apply {
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
