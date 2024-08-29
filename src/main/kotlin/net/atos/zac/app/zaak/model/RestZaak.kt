/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaak.model

import jakarta.json.bind.annotation.JsonbProperty
import jakarta.validation.Valid
import net.atos.zac.app.identity.model.RestGroup
import net.atos.zac.app.identity.model.RestUser
import net.atos.zac.app.klant.model.klant.IdentificatieType
import net.atos.zac.app.policy.model.RESTZaakRechten
import net.atos.zac.zoeken.model.ZaakIndicatie
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import java.time.LocalDate
import java.util.EnumSet
import java.util.UUID

@NoArgConstructor
@AllOpen
data class RestZaak(
    var archiefActiedatum: LocalDate?,
    var archiefNominatie: String?,
    var behandelaar: RestUser?,
    var besluiten: List<RestBesluit>?,
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
    var initiatorIdentificatieType: IdentificatieType?,

    @get:JsonbProperty("isOpgeschort")
    var isOpgeschort: Boolean,

    @get:JsonbProperty("isOpen")
    var isOpen: Boolean,

    @get:JsonbProperty("isHeropend")
    var isHeropend: Boolean,

    @get:JsonbProperty("isHoofdzaak")
    var isHoofdzaak: Boolean,

    @get:JsonbProperty("isDeelzaak")
    var isDeelzaak: Boolean,

    @get:JsonbProperty("isOntvangstbevestigingVerstuurd")
    var isOntvangstbevestigingVerstuurd: Boolean,

    @get:JsonbProperty("isBesluittypeAanwezig")
    var isBesluittypeAanwezig: Boolean,

    @get:JsonbProperty("isInIntakeFase")
    var isInIntakeFase: Boolean,

    @get:JsonbProperty("isProcesGestuurd")
    var isProcesGestuurd: Boolean,

    @get:JsonbProperty("isVerlengd")
    var isVerlengd: Boolean,

    var kenmerken: List<RESTZaakKenmerk>?,
    var omschrijving: String,
    var publicatiedatum: LocalDate?,
    var rechten: RESTZaakRechten,
    var redenOpschorting: String?,
    var redenVerlenging: String?,
    var registratiedatum: LocalDate?,
    var resultaat: RESTZaakResultaat?,
    var startdatum: LocalDate?,
    var status: RestZaakStatus?,
    var toelichting: String?,
    var uiterlijkeEinddatumAfdoening: LocalDate?,
    var uuid: UUID,
    var verantwoordelijkeOrganisatie: String?,
    var vertrouwelijkheidaanduiding: String?,
    var zaakdata: Map<String, Any>?,
    var zaakgeometrie: RestGeometry?,
    var zaaktype: RestZaaktype
)
