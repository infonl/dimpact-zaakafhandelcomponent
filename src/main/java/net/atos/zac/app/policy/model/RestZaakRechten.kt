/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.policy.model

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
