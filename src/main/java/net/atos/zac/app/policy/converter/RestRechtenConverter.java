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
        restDocumentRechten.lezen = documentRechten.getLezen();
        restDocumentRechten.wijzigen = documentRechten.getWijzigen();
        restDocumentRechten.ontgrendelen = documentRechten.getOntgrendelen();
        restDocumentRechten.vergrendelen = documentRechten.getVergrendelen();
        restDocumentRechten.verwijderen = documentRechten.getVerwijderen();
        restDocumentRechten.ondertekenen = documentRechten.getOndertekenen();
        restDocumentRechten.toevoegenNieuweVersie = documentRechten.getToevoegenNieuweVersie();
        return restDocumentRechten;
    }

    public static RestTaakRechten convert(final TaakRechten taakRechten) {
        final RestTaakRechten restTaakRechten = new RestTaakRechten();
        restTaakRechten.lezen = taakRechten.getLezen();
        restTaakRechten.wijzigen = taakRechten.getWijzigen();
        restTaakRechten.toekennen = taakRechten.getToekennen();
        restTaakRechten.toevoegenDocument = taakRechten.getToevoegenDocument();
        return restTaakRechten;
    }

    public static RestZaakRechten convert(final ZaakRechten zaakRechten) {
        final RestZaakRechten restZaakRechten = new RestZaakRechten();
        restZaakRechten.lezen = zaakRechten.getLezen();
        restZaakRechten.wijzigen = zaakRechten.getWijzigen();
        restZaakRechten.toekennen = zaakRechten.getToekennen();
        restZaakRechten.behandelen = zaakRechten.getBehandelen();
        restZaakRechten.afbreken = zaakRechten.getAfbreken();
        restZaakRechten.heropenen = zaakRechten.getHeropenen();
        restZaakRechten.wijzigenDoorlooptijd = zaakRechten.getWijzigenDoorlooptijd();
        restZaakRechten.bekijkenZaakdata = zaakRechten.getBekijkenZaakdata();
        restZaakRechten.versturenOntvangstbevestiging = zaakRechten.getVersturenOntvangstbevestiging();
        restZaakRechten.toevoegenBagObject = zaakRechten.getToevoegenBagObject();
        restZaakRechten.toevoegenBetrokkeneBedrijf = zaakRechten.getToevoegenBetrokkeneBedrijf();
        restZaakRechten.toevoegenBetrokkenePersoon = zaakRechten.getToevoegenBetrokkenePersoon();
        restZaakRechten.toevoegenInitiatorBedrijf = zaakRechten.getToevoegenInitiatorBedrijf();
        restZaakRechten.toevoegenInitiatorPersoon = zaakRechten.getToevoegenInitiatorPersoon();
        restZaakRechten.verwijderenBetrokkene = zaakRechten.getVerwijderenBetrokkene();
        restZaakRechten.verwijderenInitiator = zaakRechten.getVerwijderenInitiator();
        restZaakRechten.creeerenDocument = zaakRechten.getCreeerenDocument();
        restZaakRechten.versturenEmail = zaakRechten.getVersturenEmail();
        restZaakRechten.wijzigenLocatie = zaakRechten.getWijzigenLocatie();
        return restZaakRechten;
    }

    public static RestWerklijstRechten convert(final WerklijstRechten werklijstrechten) {
        final RestWerklijstRechten restWerklijstRechten = new RestWerklijstRechten();
        restWerklijstRechten.inbox = werklijstrechten.getInbox();
        restWerklijstRechten.ontkoppeldeDocumentenVerwijderen = werklijstrechten.getOntkoppeldeDocumentenVerwijderen();
        restWerklijstRechten.inboxProductaanvragenVerwijderen = werklijstrechten.getInboxProductaanvragenVerwijderen();
        restWerklijstRechten.zakenTaken = werklijstrechten.getZakenTaken();
        restWerklijstRechten.zakenTakenVerdelen = werklijstrechten.getZakenTakenVerdelen();
        restWerklijstRechten.zakenTakenExporteren = werklijstrechten.getZakenTakenExporteren();
        return restWerklijstRechten;
    }

    public static RestOverigeRechten convert(final OverigeRechten overigeRechten) {
        final RestOverigeRechten restOverigeRechten = new RestOverigeRechten();
        restOverigeRechten.startenZaak = overigeRechten.getStartenZaak();
        restOverigeRechten.beheren = overigeRechten.getBeheren();
        restOverigeRechten.zoeken = overigeRechten.getZoeken();
        return restOverigeRechten;
    }
}
