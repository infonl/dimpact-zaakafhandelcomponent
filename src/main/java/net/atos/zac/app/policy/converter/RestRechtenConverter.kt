/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.policy.converter

import net.atos.zac.app.policy.model.RestDocumentRechten
import net.atos.zac.app.policy.model.RestOverigeRechten
import net.atos.zac.app.policy.model.RestTaakRechten
import net.atos.zac.app.policy.model.RestWerklijstRechten
import net.atos.zac.app.policy.model.RestZaakRechten
import nl.info.zac.policy.output.DocumentRechten
import nl.info.zac.policy.output.OverigeRechten
import nl.info.zac.policy.output.TaakRechten
import nl.info.zac.policy.output.WerklijstRechten
import nl.info.zac.policy.output.ZaakRechten

fun DocumentRechten.toRestDocumentRechten() = RestDocumentRechten(
    lezen = this.lezen,
    wijzigen = this.wijzigen,
    ontgrendelen = this.ontgrendelen,
    vergrendelen = this.vergrendelen,
    verwijderen = this.verwijderen,
    ondertekenen = this.ondertekenen,
    toevoegenNieuweVersie = this.toevoegenNieuweVersie
)

fun TaakRechten.toRestTaakRechten() = RestTaakRechten(
    lezen = this.lezen,
    wijzigen = this.wijzigen,
    toekennen = this.toekennen,
    toevoegenDocument = this.toevoegenDocument
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

fun WerklijstRechten.toRestWerklijstRechten() = RestWerklijstRechten(
    inbox = this.inbox,
    ontkoppeldeDocumentenVerwijderen = this.ontkoppeldeDocumentenVerwijderen,
    inboxProductaanvragenVerwijderen = this.inboxProductaanvragenVerwijderen,
    zakenTaken = this.zakenTaken,
    zakenTakenVerdelen = this.zakenTakenVerdelen,
    zakenTakenExporteren = this.zakenTakenExporteren
)

fun OverigeRechten.toRestOverigeRechten() = RestOverigeRechten(
    startenZaak = this.startenZaak,
    beheren = this.beheren,
    zoeken = this.zoeken
)
