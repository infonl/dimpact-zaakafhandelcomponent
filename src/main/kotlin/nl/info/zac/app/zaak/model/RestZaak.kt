/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.model

import jakarta.json.bind.annotation.JsonbProperty
import nl.info.zac.app.identity.model.RestGroup
import nl.info.zac.app.identity.model.RestUser
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
    var startdatumBewaartermijn: LocalDate?,
    var archiefNominatie: String?,
    var behandelaar: RestUser?,
    var besluiten: List<RestDecision>?,
    var bronorganisatie: String?,
    var communicatiekanaal: String?,
    var duurVerlenging: String?,
    var einddatumGepland: LocalDate?,
    var einddatum: LocalDate?,
    var gerelateerdeZaken: List<RestGerelateerdeZaak>?,
    var groep: RestGroup?,
    var identificatie: String,
    var indicaties: EnumSet<ZaakIndicatie>,
    var initiatorIdentificatie: BetrokkeneIdentificatie?,

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

    @get:JsonbProperty("heeftOntvangstbevestigingVerstuurd")
    var heeftOntvangstbevestigingVerstuurd: Boolean,

    /**
     * Indicates whether the case is driven using a BPMN process or not.
     * If not, it is in most cases driven by the ZAC CMMN model.
     */
    @get:JsonbProperty("isProcesGestuurd")
    var isProcesGestuurd: Boolean,

    @get:JsonbProperty("isVerlengd")
    var isVerlengd: Boolean,

    var kenmerken: List<RESTZaakKenmerk>?,
    var omschrijving: String,
    var publicatiedatum: LocalDate?,
    var rechten: RestZaakRechten,
    var redenOpschorting: String?,
    var redenVerlenging: String?,
    var registratiedatum: LocalDate?,
    var resultaat: RestZaakResultaat?,
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
