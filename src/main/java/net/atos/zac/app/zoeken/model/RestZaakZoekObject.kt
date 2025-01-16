/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zoeken.model

import net.atos.zac.app.policy.model.RestZaakRechten
import net.atos.zac.zoeken.model.ZaakIndicatie
import nl.info.zac.util.NoArgConstructor
import java.time.LocalDate
import java.util.EnumSet

@NoArgConstructor
data class RestZaakZoekObject(
    var omschrijving: String? = null,
    var toelichting: String? = null,
    var registratiedatum: LocalDate? = null,
    var startdatum: LocalDate? = null,
    var einddatumGepland: LocalDate? = null,
    var einddatum: LocalDate? = null,
    var archiefActiedatum: LocalDate? = null,
    var uiterlijkeEinddatumAfdoening: LocalDate? = null,
    var publicatiedatum: LocalDate? = null,
    var communicatiekanaal: String? = null,
    var vertrouwelijkheidaanduiding: String? = null,
    var archiefNominatie: String? = null,
    var afgehandeld: Boolean = false,
    var groepId: String? = null,
    var groepNaam: String? = null,
    var behandelaarNaam: String? = null,
    var behandelaarGebruikersnaam: String? = null,
    var initiatorIdentificatie: String? = null,
    var locatie: String? = null,
    var indicatieVerlenging: Boolean = false,
    var indicatieOpschorting: Boolean = false,
    var indicatieHeropend: Boolean = false,
    var indicatieDeelzaak: Boolean = false,
    var indicatieHoofdzaak: Boolean = false,
    var duurVerlenging: String? = null,
    var redenVerlenging: String? = null,
    var redenOpschorting: String? = null,
    var zaaktypeUuid: String? = null,
    var zaaktypeOmschrijving: String? = null,
    var resultaattypeOmschrijving: String? = null,
    var resultaatToelichting: String? = null,
    var statustypeOmschrijving: String? = null,
    var statusToelichting: String? = null,
    var aantalOpenstaandeTaken: Long = 0,
    var indicaties: EnumSet<ZaakIndicatie>? = null,
    var rechten: RestZaakRechten? = null,
    var betrokkenen: Map<String, MutableList<String>>? = null
) : AbstractRestZoekObject()
