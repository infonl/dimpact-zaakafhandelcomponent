/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.policy.converter;

import net.atos.zac.app.policy.model.RestDocumentRechten;
import net.atos.zac.app.policy.model.RestOverigeRechten;
import net.atos.zac.app.policy.model.RestTaakRechten;
import net.atos.zac.app.policy.model.RestWerklijstRechten;
import net.atos.zac.app.policy.model.RestZaakRechten;
import net.atos.zac.policy.output.DocumentRechten;
import net.atos.zac.policy.output.OverigeRechten;
import net.atos.zac.policy.output.TaakRechten;
import net.atos.zac.policy.output.WerklijstRechten;
import net.atos.zac.policy.output.ZaakRechten;

public class RestRechtenConverter {

    public static RestDocumentRechten convert(final DocumentRechten documentRechten) {
        final RestDocumentRechten restDocumentRechten = new RestDocumentRechten();
        restDocumentRechten.lezen = documentRechten.lezen();
        restDocumentRechten.wijzigen = documentRechten.wijzigen();
        restDocumentRechten.ontgrendelen = documentRechten.ontgrendelen();
        restDocumentRechten.vergrendelen = documentRechten.vergrendelen();
        restDocumentRechten.verwijderen = documentRechten.verwijderen();
        restDocumentRechten.ondertekenen = documentRechten.ondertekenen();
        restDocumentRechten.toevoegenNieuweVersie = documentRechten.toevoegenNieuweVersie();
        return restDocumentRechten;
    }

    public static RestTaakRechten convert(final TaakRechten taakRechten) {
        final RestTaakRechten restTaakRechten = new RestTaakRechten();
        restTaakRechten.lezen = taakRechten.lezen();
        restTaakRechten.wijzigen = taakRechten.wijzigen();
        restTaakRechten.toekennen = taakRechten.toekennen();
        restTaakRechten.toevoegenDocument = taakRechten.toevoegenDocument();
        return restTaakRechten;
    }

    public static RestZaakRechten convert(final ZaakRechten zaakRechten) {
        final RestZaakRechten restZaakRechten = new RestZaakRechten();
        restZaakRechten.lezen = zaakRechten.lezen();
        restZaakRechten.wijzigen = zaakRechten.wijzigen();
        restZaakRechten.toekennen = zaakRechten.toekennen();
        restZaakRechten.behandelen = zaakRechten.behandelen();
        restZaakRechten.afbreken = zaakRechten.afbreken();
        restZaakRechten.heropenen = zaakRechten.heropenen();
        restZaakRechten.wijzigenDoorlooptijd = zaakRechten.wijzigenDoorlooptijd();
        restZaakRechten.bekijkenZaakdata = zaakRechten.bekijkenZaakdata();
        restZaakRechten.versturenOntvangstbevestiging = zaakRechten.versturenOntvangstbevestiging();
        restZaakRechten.toevoegenBagObject = zaakRechten.toevoegenBagObject();
        restZaakRechten.toevoegenBetrokkeneBedrijf = zaakRechten.toevoegenBetrokkeneBedrijf();
        restZaakRechten.toevoegenBetrokkenePersoon = zaakRechten.toevoegenBetrokkenePersoon();
        restZaakRechten.toevoegenInitiatorBedrijf = zaakRechten.toevoegenInitiatorBedrijf();
        restZaakRechten.toevoegenInitiatorPersoon = zaakRechten.toevoegenInitiatorPersoon();
        restZaakRechten.verwijderenBetrokkene = zaakRechten.verwijderenBetrokkene();
        restZaakRechten.verwijderenInitiator = zaakRechten.verwijderenInitiator();
        restZaakRechten.creeerenDocument = zaakRechten.creeerenDocument();
        restZaakRechten.versturenEmail = zaakRechten.versturenEmail();
        return restZaakRechten;
    }

    public static RestWerklijstRechten convert(final WerklijstRechten werklijstrechten) {
        final RestWerklijstRechten restWerklijstRechten = new RestWerklijstRechten();
        restWerklijstRechten.inbox = werklijstrechten.inbox();
        restWerklijstRechten.ontkoppeldeDocumentenVerwijderen = werklijstrechten.ontkoppeldeDocumentenVerwijderen();
        restWerklijstRechten.inboxProductaanvragenVerwijderen = werklijstrechten.inboxProductaanvragenVerwijderen();
        restWerklijstRechten.zakenTaken = werklijstrechten.zakenTaken();
        restWerklijstRechten.zakenTakenVerdelen = werklijstrechten.zakenTakenVerdelen();
        restWerklijstRechten.zakenTakenExporteren = werklijstrechten.zakenTakenExporteren();
        return restWerklijstRechten;
    }

    public static RestOverigeRechten convert(final OverigeRechten overigeRechten) {
        final RestOverigeRechten restOverigeRechten = new RestOverigeRechten();
        restOverigeRechten.startenZaak = overigeRechten.startenZaak();
        restOverigeRechten.beheren = overigeRechten.beheren();
        restOverigeRechten.zoeken = overigeRechten.zoeken();
        return restOverigeRechten;
    }
}
