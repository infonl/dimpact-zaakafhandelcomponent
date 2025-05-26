/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.policy.model

import nl.info.zac.policy.output.ZaakRechten

data class RestZaakRechten(
    val lezen: Boolean,
    val wijzigen: Boolean,
    val toekennen: Boolean,
    val behandelen: Boolean,
    val afbreken: Boolean,
    val heropenen: Boolean,
    val bekijkenZaakdata: Boolean,
    val wijzigenDoorlooptijd: Boolean,
    val toevoegenBagObject: Boolean,
    val toevoegenBetrokkeneBedrijf: Boolean,
    val toevoegenBetrokkenePersoon: Boolean,
    val toevoegenInitiatorBedrijf: Boolean,
    val toevoegenInitiatorPersoon: Boolean,
    val versturenOntvangstbevestiging: Boolean,
    val verwijderenBetrokkene: Boolean,
    val verwijderenInitiator: Boolean,
    val creeerenDocument: Boolean,
    val versturenEmail: Boolean,
    val wijzigenLocatie: Boolean
)

fun ZaakRechten.toRestZaakRechten() = RestZaakRechten(
    lezen = this.lezen,
    wijzigen = this.wijzigen,
    toekennen = this.toekennen,
    behandelen = this.behandelen,
    afbreken = this.afbreken,
    heropenen = this.heropenen,
    wijzigenDoorlooptijd = this.wijzigenDoorlooptijd,
    bekijkenZaakdata = this.bekijkenZaakdata,
    versturenOntvangstbevestiging = this.versturenOntvangstbevestiging,
    toevoegenBagObject = this.toevoegenBagObject,
    toevoegenBetrokkeneBedrijf = this.toevoegenBetrokkeneBedrijf,
    toevoegenBetrokkenePersoon = this.toevoegenBetrokkenePersoon,
    toevoegenInitiatorBedrijf = this.toevoegenInitiatorBedrijf,
    toevoegenInitiatorPersoon = this.toevoegenInitiatorPersoon,
    verwijderenBetrokkene = this.verwijderenBetrokkene,
    verwijderenInitiator = this.verwijderenInitiator,
    creeerenDocument = this.creeerenDocument,
    versturenEmail = this.versturenEmail,
    wijzigenLocatie = this.wijzigenLocatie
)
