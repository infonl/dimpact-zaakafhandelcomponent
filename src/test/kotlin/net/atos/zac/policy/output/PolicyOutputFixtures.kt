/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.policy.output

fun createDocumentRechtenAllAllow() = DocumentRechten(
    true, true, true, true, true, true, true,
    true, true, true,
)

@Suppress("LongParameterList")
fun createDocumentRechtenAllDeny(
    lezen: Boolean = false,
    wijzigen: Boolean = false,
    verwijderen: Boolean = false,
    vergrendelen: Boolean = false,
    ontgrendelen: Boolean = false,
    ondertekenen: Boolean = false,
    toevoegenNieuweVersie: Boolean = false,
    verplaatsen: Boolean = false,
    ontkoppelen: Boolean = false,
    downloaden: Boolean = false,
) = DocumentRechten(
    lezen, wijzigen, verwijderen, vergrendelen, ontgrendelen, ondertekenen,
    toevoegenNieuweVersie, verplaatsen, ontkoppelen, downloaden
)

fun createTaakRechtenAllDeny(
    lezen: Boolean = false,
    wijzigen: Boolean = false,
    toekennen: Boolean = false,
    creeerenDocument: Boolean = false,
    toevoegenDocument: Boolean = false,
) = TaakRechten(lezen, wijzigen, toekennen, creeerenDocument, toevoegenDocument)

fun createZaakRechtenAllAllow() = ZaakRechten(
    true, true, true, true, true, true, true,
    true, true, true, true, true, true,
    true, true, true, true, true,
    true, true, true, true,
    true, true, true, true, true
)

@Suppress("LongParameterList")
fun createZaakRechtenAllDeny(
    lezen: Boolean = false,
    wijzigen: Boolean = false,
    toekennen: Boolean = false,
    behandelen: Boolean = false,
    afbreken: Boolean = false,
    heropenen: Boolean = false,
    wijzigenZaakdata: Boolean = false,
    wijzigenDoorlooptijd: Boolean = false,
    verlengen: Boolean = false,
    opschorten: Boolean = false,
    hervatten: Boolean = false,
    creeerenDocument: Boolean = false,
    toevoegenDocument: Boolean = false,
    koppelen: Boolean = false,
    koppelenGerelateerd: Boolean = false,
    versturenEmail: Boolean = false,
    versturenOntvangstbevestiging: Boolean = false,
    toevoegenInitiatorPersoon: Boolean = false,
    toevoegenInitiatorBedrijf: Boolean = false,
    verwijderenInitiator: Boolean = false,
    toevoegenBetrokkenePersoon: Boolean = false,
    toevoegenBetrokkeneBedrijf: Boolean = false,
    verwijderenBetrokkene: Boolean = false,
    toevoegenBagObject: Boolean = false,
    startenTaak: Boolean = false,
    vastleggenBesluit: Boolean = false,
    verlengenDoorlooptijd: Boolean = false,
) = ZaakRechten(
    lezen, wijzigen, toekennen, behandelen, afbreken, heropenen, wijzigenZaakdata, wijzigenDoorlooptijd,
    verlengen, opschorten, hervatten, creeerenDocument, toevoegenDocument, koppelen, koppelenGerelateerd,
    versturenEmail, versturenOntvangstbevestiging, toevoegenInitiatorPersoon, toevoegenInitiatorBedrijf,
    verwijderenInitiator, toevoegenBetrokkenePersoon, toevoegenBetrokkeneBedrijf, verwijderenBetrokkene,
    toevoegenBagObject, startenTaak, vastleggenBesluit, verlengenDoorlooptijd
)

fun createWerklijstRechtenAllDeny(
    inbox: Boolean = false,
    ontkoppeldeDocumentenVerwijderen: Boolean = false,
    inboxProductaanvragenVerwijderen: Boolean = false,
    zakenTaken: Boolean = false,
    zakenTakenVerdelen: Boolean = false,
) = WerklijstRechten(
    inbox,
    ontkoppeldeDocumentenVerwijderen,
    inboxProductaanvragenVerwijderen,
    zakenTaken,
    zakenTakenVerdelen
)

fun createOverigeRechtenAllDeny(
    startenZaak: Boolean = false,
    beheren: Boolean = false,
    zoeken: Boolean = false
) = OverigeRechten(
    startenZaak,
    beheren,
    zoeken
)
