/*
 * SPDX-FileCopyrightText: 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.policy.output

import nl.info.zac.policy.output.DocumentRechten
import nl.info.zac.policy.output.OverigeRechten
import nl.info.zac.policy.output.TaakRechten
import nl.info.zac.policy.output.WerklijstRechten
import nl.info.zac.policy.output.ZaakRechten

@Suppress("LongParameterList")
fun createDocumentRechten(
    lezen: Boolean = true,
    wijzigen: Boolean = true,
    verwijderen: Boolean = true,
    vergrendelen: Boolean = true,
    ontgrendelen: Boolean = true,
    ondertekenen: Boolean = true,
    toevoegenNieuweVersie: Boolean = true,
    verplaatsen: Boolean = true,
    ontkoppelen: Boolean = true,
    downloaden: Boolean = true
) = DocumentRechten(
    lezen, wijzigen, verwijderen, vergrendelen, ontgrendelen, ondertekenen,
    toevoegenNieuweVersie, verplaatsen, ontkoppelen, downloaden
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
    downloaden: Boolean = false
) = createDocumentRechten(
    lezen, wijzigen, verwijderen, vergrendelen, ontgrendelen, ondertekenen,
    toevoegenNieuweVersie, verplaatsen, ontkoppelen, downloaden
)

fun createTaakRechten(
    lezen: Boolean = true,
    wijzigen: Boolean = true,
    toekennen: Boolean = true,
    creeerenDocument: Boolean = true,
    toevoegenDocument: Boolean = true
) = TaakRechten(lezen, wijzigen, toekennen, creeerenDocument, toevoegenDocument)

fun createTaakRechtenAllDeny(
    lezen: Boolean = false,
    wijzigen: Boolean = false,
    toekennen: Boolean = false,
    creeerenDocument: Boolean = false,
    toevoegenDocument: Boolean = false
) = createTaakRechten(lezen, wijzigen, toekennen, creeerenDocument, toevoegenDocument)

@Suppress("LongParameterList")
fun createZaakRechten(
    lezen: Boolean = true,
    wijzigen: Boolean = true,
    toekennen: Boolean = true,
    behandelen: Boolean = true,
    afbreken: Boolean = true,
    heropenen: Boolean = true,
    bekijkenZaakdata: Boolean = true,
    wijzigenDoorlooptijd: Boolean = true,
    verlengen: Boolean = true,
    opschorten: Boolean = true,
    hervatten: Boolean = true,
    creeerenDocument: Boolean = true,
    toevoegenDocument: Boolean = true,
    koppelen: Boolean = true,
    versturenEmail: Boolean = true,
    versturenOntvangstbevestiging: Boolean = true,
    toevoegenInitiatorPersoon: Boolean = true,
    toevoegenInitiatorBedrijf: Boolean = true,
    verwijderenInitiator: Boolean = true,
    toevoegenBetrokkenePersoon: Boolean = true,
    toevoegenBetrokkeneBedrijf: Boolean = true,
    verwijderenBetrokkene: Boolean = true,
    toevoegenBagObject: Boolean = true,
    startenTaak: Boolean = true,
    vastleggenBesluit: Boolean = true,
    verlengenDoorlooptijd: Boolean = true,
    wijzigenLocatie: Boolean = true
) = ZaakRechten(
    lezen, wijzigen, toekennen, behandelen, afbreken, heropenen, bekijkenZaakdata, wijzigenDoorlooptijd,
    verlengen, opschorten, hervatten, creeerenDocument, toevoegenDocument, koppelen, versturenEmail,
    versturenOntvangstbevestiging, toevoegenInitiatorPersoon, toevoegenInitiatorBedrijf, verwijderenInitiator,
    toevoegenBetrokkenePersoon, toevoegenBetrokkeneBedrijf, verwijderenBetrokkene, toevoegenBagObject, startenTaak,
    vastleggenBesluit, verlengenDoorlooptijd, wijzigenLocatie
)

@Suppress("LongParameterList")
fun createZaakRechtenAllDeny(
    lezen: Boolean = false,
    wijzigen: Boolean = false,
    toekennen: Boolean = false,
    behandelen: Boolean = false,
    afbreken: Boolean = false,
    heropenen: Boolean = false,
    bekijkenZaakdata: Boolean = false,
    wijzigenDoorlooptijd: Boolean = false,
    verlengen: Boolean = false,
    opschorten: Boolean = false,
    hervatten: Boolean = false,
    creeerenDocument: Boolean = false,
    toevoegenDocument: Boolean = false,
    koppelen: Boolean = false,
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
    verlengenDoorlooptijd: Boolean = false
) = createZaakRechten(
    lezen,
    wijzigen,
    toekennen,
    behandelen,
    afbreken,
    heropenen,
    bekijkenZaakdata,
    wijzigenDoorlooptijd,
    verlengen,
    opschorten,
    hervatten,
    creeerenDocument,
    toevoegenDocument,
    koppelen,
    versturenEmail,
    versturenOntvangstbevestiging,
    toevoegenInitiatorPersoon,
    toevoegenInitiatorBedrijf,
    verwijderenInitiator,
    toevoegenBetrokkenePersoon,
    toevoegenBetrokkeneBedrijf,
    verwijderenBetrokkene,
    toevoegenBagObject,
    startenTaak,
    vastleggenBesluit,
    verlengenDoorlooptijd
)

@Suppress("LongParameterList")
fun createWerklijstRechten(
    inbox: Boolean = true,
    ontkoppeldeDocumentenVerwijderen: Boolean = true,
    inboxProductaanvragenVerwijderen: Boolean = true,
    zakenTaken: Boolean = true,
    zakenTakenVerdelen: Boolean = true,
    zakenTakenExporteren: Boolean = true
) = WerklijstRechten(
    inbox,
    ontkoppeldeDocumentenVerwijderen,
    inboxProductaanvragenVerwijderen,
    zakenTaken,
    zakenTakenVerdelen,
    zakenTakenExporteren
)

fun createWerklijstRechtenAllDeny(
    inbox: Boolean = false,
    ontkoppeldeDocumentenVerwijderen: Boolean = false,
    inboxProductaanvragenVerwijderen: Boolean = false,
    zakenTaken: Boolean = false,
    zakenTakenVerdelen: Boolean = false
) = createWerklijstRechten(
    inbox,
    ontkoppeldeDocumentenVerwijderen,
    inboxProductaanvragenVerwijderen,
    zakenTaken,
    zakenTakenVerdelen
)

fun createOverigeRechten(
    startenZaak: Boolean = true,
    beheren: Boolean = true,
    zoeken: Boolean = true
) = OverigeRechten(
    startenZaak,
    beheren,
    zoeken
)

fun createOverigeRechtenAllDeny(
    startenZaak: Boolean = false,
    beheren: Boolean = false,
    zoeken: Boolean = false
) = createOverigeRechten(startenZaak, beheren, zoeken)
