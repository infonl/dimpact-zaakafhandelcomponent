/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.policy.converter;

import net.atos.zac.app.policy.model.RESTDocumentRechten;
import net.atos.zac.app.policy.model.RESTOverigeRechten;
import net.atos.zac.app.policy.model.RESTTaakRechten;
import net.atos.zac.app.policy.model.RESTWerklijstRechten;
import net.atos.zac.app.policy.model.RESTZaakRechten;
import net.atos.zac.policy.output.DocumentRechten;
import net.atos.zac.policy.output.OverigeRechten;
import net.atos.zac.policy.output.TaakRechten;
import net.atos.zac.policy.output.WerklijstRechten;
import net.atos.zac.policy.output.ZaakRechten;

public class RESTRechtenConverter {

    public RESTDocumentRechten convert(final DocumentRechten documentRechten) {
        final RESTDocumentRechten restDocumentRechten = new RESTDocumentRechten();
        restDocumentRechten.lezen = documentRechten.lezen();
        restDocumentRechten.wijzigen = documentRechten.wijzigen();
        restDocumentRechten.ontgrendelen = documentRechten.ontgrendelen();
        restDocumentRechten.vergrendelen = documentRechten.vergrendelen();
        restDocumentRechten.verwijderen = documentRechten.verwijderen();
        restDocumentRechten.ondertekenen = documentRechten.ondertekenen();
        return restDocumentRechten;
    }

    public RESTTaakRechten convert(final TaakRechten taakRechten) {
        final RESTTaakRechten restTaakRechten = new RESTTaakRechten();
        restTaakRechten.lezen = taakRechten.lezen();
        restTaakRechten.wijzigen = taakRechten.wijzigen();
        restTaakRechten.toekennen = taakRechten.toekennen();
        restTaakRechten.toevoegenDocument = taakRechten.toevoegenDocument();
        return restTaakRechten;
    }

    public RESTZaakRechten convert(final ZaakRechten zaakRechten) {
        final RESTZaakRechten restZaakRechten = new RESTZaakRechten();
        restZaakRechten.lezen = zaakRechten.lezen();
        restZaakRechten.wijzigen = zaakRechten.wijzigen();
        restZaakRechten.toekennen = zaakRechten.toekennen();
        restZaakRechten.behandelen = zaakRechten.behandelen();
        restZaakRechten.afbreken = zaakRechten.afbreken();
        restZaakRechten.heropenen = zaakRechten.heropenen();
        restZaakRechten.wijzigenDoorlooptijd = zaakRechten.wijzigenDoorlooptijd();
        restZaakRechten.bekijkenZaakdata = zaakRechten.bekijkenZaakdata();
        return restZaakRechten;
    }

    public RESTWerklijstRechten convert(final WerklijstRechten werklijstrechten) {
        final RESTWerklijstRechten restWerklijstRechten = new RESTWerklijstRechten();
        restWerklijstRechten.inbox = werklijstrechten.inbox();
        restWerklijstRechten.ontkoppeldeDocumentenVerwijderen = werklijstrechten.ontkoppeldeDocumentenVerwijderen();
        restWerklijstRechten.inboxProductaanvragenVerwijderen = werklijstrechten.inboxProductaanvragenVerwijderen();
        restWerklijstRechten.zakenTaken = werklijstrechten.zakenTaken();
        restWerklijstRechten.zakenTakenVerdelen = werklijstrechten.zakenTakenVerdelen();
        restWerklijstRechten.zakenTakenExporteren = werklijstrechten.zakenTakenExporteren();
        return restWerklijstRechten;
    }

    public RESTOverigeRechten convert(final OverigeRechten overigeRechten) {
        final RESTOverigeRechten restOverigeRechten = new RESTOverigeRechten();
        restOverigeRechten.startenZaak = overigeRechten.startenZaak();
        restOverigeRechten.beheren = overigeRechten.beheren();
        restOverigeRechten.zoeken = overigeRechten.zoeken();
        return restOverigeRechten;
    }
}
