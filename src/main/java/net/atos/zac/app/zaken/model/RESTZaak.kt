/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaken.model

import net.atos.zac.app.identity.model.RESTGroup
import net.atos.zac.app.identity.model.RESTUser
import net.atos.zac.app.klanten.model.klant.IdentificatieType
import net.atos.zac.app.policy.model.RESTZaakRechten
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import java.time.LocalDate
import java.util.*

@NoArgConstructor
@AllOpen
data class RESTZaak(
    var uuid: UUID,

    var identificatie: String,

    var omschrijving: String,

    var toelichting: String?,

    var zaaktype: RESTZaaktype,

    var status: RESTZaakStatus?,

    var resultaat: RESTZaakResultaat?,

    var besluiten: List<RESTBesluit>?,

    var bronorganisatie: String?,

    var verantwoordelijkeOrganisatie: String?,

    var registratiedatum: LocalDate?,

    var startdatum: LocalDate,

    var einddatumGepland: LocalDate?,

    var einddatum: LocalDate?,

    var uiterlijkeEinddatumAfdoening: LocalDate?,

    var publicatiedatum: LocalDate?,

    var archiefActiedatum: LocalDate?,

    var archiefNominatie: String?,

    var communicatiekanaal: RESTCommunicatiekanaal?,

    var vertrouwelijkheidaanduiding: String,

    var zaakgeometrie: RESTGeometry?,

    var isOpgeschort: Boolean,

    var redenOpschorting: String?,

    var isVerlengd: Boolean,

    var redenVerlenging: String?,

    var duurVerlenging: String?,

    var groep: RESTGroup?,

    var behandelaar: RESTUser?,

    var gerelateerdeZaken: List<RESTGerelateerdeZaak>?,

    var kenmerken: List<RESTZaakKenmerk>?,

    var eigenschappen: List<RESTZaakEigenschap>?,

    var zaakdata: Map<String, Any>?,

    var initiatorIdentificatieType: IdentificatieType?,

    var initiatorIdentificatie: String?,

    var isOpen: Boolean,

    var isHeropend: Boolean,

    var isHoofdzaak: Boolean,

    var isDeelzaak: Boolean,

    var isOntvangstbevestigingVerstuurd: Boolean,

    var isBesluittypeAanwezig: Boolean,

    var isInIntakeFase: Boolean,

    var isProcesGestuurd: Boolean,

    var rechten: RESTZaakRechten?
)
