/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.policy.output;

import jakarta.json.bind.annotation.JsonbCreator;
import jakarta.json.bind.annotation.JsonbProperty;

import net.atos.zac.util.DummyInterface;

public record WerklijstRechten(
                               boolean inbox,
                               boolean ontkoppeldeDocumentenVerwijderen,
                               boolean inboxProductaanvragenVerwijderen,
                               boolean zakenTaken,
                               boolean zakenTakenVerdelen
) implements DummyInterface {

    @JsonbCreator
    public WerklijstRechten(
            @JsonbProperty("inbox") final boolean inbox,
            @JsonbProperty("ontkoppelde_documenten_verwijderen") final boolean ontkoppeldeDocumentenVerwijderen,
            @JsonbProperty("inbox_productaanvragen_verwijderen") final boolean inboxProductaanvragenVerwijderen,
            @JsonbProperty("zaken_taken") final boolean zakenTaken,
            @JsonbProperty("zaken_taken_verdelen") final boolean zakenTakenVerdelen
    ) {
        this.inbox = inbox;
        this.ontkoppeldeDocumentenVerwijderen = ontkoppeldeDocumentenVerwijderen;
        this.inboxProductaanvragenVerwijderen = inboxProductaanvragenVerwijderen;
        this.zakenTaken = zakenTaken;
        this.zakenTakenVerdelen = zakenTakenVerdelen;
    }


}
