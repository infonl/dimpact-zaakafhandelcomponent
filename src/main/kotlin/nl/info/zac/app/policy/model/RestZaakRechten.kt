/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.policy.model

import nl.info.zac.policy.output.ZaakRechten

data class RestZaakRechten(
    val canLezen: Boolean,
    val canWijzigen: Boolean,
    val canToekennen: Boolean,
    val canBehandelen: Boolean,
    val canAfbreken: Boolean,
    val canHeropenen: Boolean,
    val canBekijkenZaakdata: Boolean,
    val canWijzigenDoorlooptijd: Boolean,
    val canToevoegenBagObject: Boolean,
    val canToevoegenBetrokkeneBedrijf: Boolean,
    val canToevoegenBetrokkenePersoon: Boolean,
    val canToevoegenInitiatorBedrijf: Boolean,
    val canToevoegenInitiatorPersoon: Boolean,
    val canVersturenOntvangstbevestiging: Boolean,
    val canVerwijderenBetrokkene: Boolean,
    val canVerwijderenInitiator: Boolean,
    val canCreerenDocument: Boolean,
    val canVersturenEmail: Boolean,
    val canWijzigenLocatie: Boolean,
    val canBrondatumZetten: Boolean
)

fun ZaakRechten.toRestZaakRechten() = RestZaakRechten(
    canLezen = this.canLezen,
    canWijzigen = this.canWijzigen,
    canToekennen = this.canToekennen,
    canBehandelen = this.canBehandelen,
    canAfbreken = this.canAfbreken,
    canHeropenen = this.canHeropenen,
    canWijzigenDoorlooptijd = this.canWijzigenDoorlooptijd,
    canBekijkenZaakdata = this.canBekijkenZaakdata,
    canVersturenOntvangstbevestiging = this.canVersturenOntvangstbevestiging,
    canToevoegenBagObject = this.canToevoegenBagObject,
    canToevoegenBetrokkeneBedrijf = this.canToevoegenBetrokkeneBedrijf,
    canToevoegenBetrokkenePersoon = this.canToevoegenBetrokkenePersoon,
    canToevoegenInitiatorBedrijf = this.canToevoegenInitiatorBedrijf,
    canToevoegenInitiatorPersoon = this.canToevoegenInitiatorPersoon,
    canVerwijderenBetrokkene = this.canVerwijderenBetrokkene,
    canVerwijderenInitiator = this.canVerwijderenInitiator,
    canCreerenDocument = this.canCreerenDocument,
    canVersturenEmail = this.canVersturenEmail,
    canWijzigenLocatie = this.canWijzigenLocatie,
    canBrondatumZetten = this.canBrondatumZetten
)
