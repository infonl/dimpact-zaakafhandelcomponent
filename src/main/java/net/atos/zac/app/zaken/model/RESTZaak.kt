/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaken.model

import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import net.atos.zac.app.identity.model.RESTGroup
import net.atos.zac.app.identity.model.RESTUser
import net.atos.zac.app.klanten.model.klant.IdentificatieType
import net.atos.zac.app.policy.model.RESTZaakRechten
import java.time.LocalDate
import java.util.*
import javax.annotation.Nullable

data class RESTZaak(
    val uuid: UUID,

    val identificatie: String,

    @NotNull
    val omschrijving: String,

    val toelichting: String?,

    @NotNull @Valid
    val zaaktype: RESTZaaktype,

    val status: RESTZaakStatus?,

    val resultaat: RESTZaakResultaat?,

    val besluiten: List<RESTBesluit>?,

    val bronorganisatie: String?,

    val verantwoordelijkeOrganisatie: String?,

    val registratiedatum: LocalDate?,

    @NotNull
    val startdatum: LocalDate,

    val einddatumGepland: LocalDate?,

    val einddatum: LocalDate?,

    val uiterlijkeEinddatumAfdoening: LocalDate?,

    val publicatiedatum: LocalDate?,

    val archiefActiedatum: LocalDate?,

    val archiefNominatie: String?,

    val communicatiekanaal: RESTCommunicatiekanaal?,

    @NotNull
    val vertrouwelijkheidaanduiding: String,

    val zaakgeometrie: RESTGeometry?,

    val isOpgeschort: Boolean,

    val redenOpschorting: String?,

    val isVerlengd: Boolean,

    val redenVerlenging: String?,

    val duurVerlenging: String?,

    @Nullable
    @Valid
    val groep: RESTGroup?,

    val behandelaar: RESTUser?,

    val gerelateerdeZaken: List<RESTGerelateerdeZaak>?,

    val kenmerken: List<RESTZaakKenmerk>?,

    val eigenschappen: List<RESTZaakEigenschap>?,

    val zaakdata: Map<String, Any>?,

    val initiatorIdentificatieType: IdentificatieType?,

    val initiatorIdentificatie: String?,

    val isOpen: Boolean,

    val isHeropend: Boolean,

    val isHoofdzaak: Boolean,

    val isDeelzaak: Boolean,

    val isOntvangstbevestigingVerstuurd: Boolean,

    val isBesluittypeAanwezig: Boolean,

    val isInIntakeFase: Boolean,

    val isProcesGestuurd: Boolean,

    val rechten: RESTZaakRechten?
)
