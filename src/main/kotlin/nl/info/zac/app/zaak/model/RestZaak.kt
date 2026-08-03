/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.model

import jakarta.json.bind.annotation.JsonbProperty
import nl.info.zac.app.identity.model.RestGroup
import nl.info.zac.app.identity.model.RestUser
import nl.info.zac.app.klant.model.contactdetails.ContactDetails
import nl.info.zac.app.policy.model.RestZaakRechten
import nl.info.zac.app.zaak.model.besluit.RestBesluit
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
    var besluiten: List<RestBesluit>?,
    var bpmnProcessDefinition: RestZaakBpmnProcessDefinition?,
    var bronorganisatie: String?,
    var communicatiekanaal: String?,
    var duurVerlenging: String?,
    var eerdereOpschorting: Boolean,
    var einddatum: LocalDate?,
    var einddatumGepland: LocalDate?,
    var gerelateerdeZaken: List<RestGerelateerdeZaak>?,
    var groep: RestGroup?,

    @get:JsonbProperty("heeftOntvangstbevestigingVerstuurd")
    var heeftOntvangstbevestigingVerstuurd: Boolean,

    var identificatie: String,
    var indicaties: EnumSet<ZaakIndicatie>,
    var initiatorIdentificatie: BetrokkeneIdentificatie?,

    @get:JsonbProperty("isBesluittypeAanwezig")
    var isBesluittypeAanwezig: Boolean,

    @get:JsonbProperty("isDeelzaak")
    var isDeelzaak: Boolean,

    @get:JsonbProperty("isHeropend")
    var isHeropend: Boolean,

    @get:JsonbProperty("isHoofdzaak")
    var isHoofdzaak: Boolean,

    @get:JsonbProperty("isInIntakeFase")
    var isInIntakeFase: Boolean,

    @get:JsonbProperty("isOpen")
    var isOpen: Boolean,

    @get:JsonbProperty("isOpgeschort")
    var isOpgeschort: Boolean,

    /**
     * Indicates whether the case is driven using a BPMN process or not.
     * If not, it is in most cases driven by the ZAC CMMN model.
     */
    @get:JsonbProperty("isProcesGestuurd")
    var isProcesGestuurd: Boolean,

    @get:JsonbProperty("isVerlengd")
    var isVerlengd: Boolean,

    var kenmerken: List<RestZaakKenmerk>?,
    var omschrijving: String,
    var publicatiedatum: LocalDate?,
    var rechten: RestZaakRechten,
    var redenOpschorting: String?,
    var redenVerlenging: String?,
    var registratiedatum: LocalDate?,
    var resultaat: RestZaakResultaat?,
    var startdatum: LocalDate?,
    var startdatumBewaartermijn: LocalDate?,
    var status: RestZaakStatus?,
    var toelichting: String?,
    var uiterlijkeEinddatumAfdoening: LocalDate?,
    var uuid: UUID,
    var verantwoordelijkeOrganisatie: String?,
    var vertrouwelijkheidaanduiding: String?,
    var zaakdata: Map<String, Any>?,
    var zaakgeometrie: RestGeometry?,
    var zaakSpecificContactDetails: ContactDetails?,
    var zaaktype: RestZaaktype
)
