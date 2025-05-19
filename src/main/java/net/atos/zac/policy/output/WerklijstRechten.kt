/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.policy.output;

import jakarta.json.bind.annotation.JsonbCreator;
import jakarta.json.bind.annotation.JsonbProperty;

import net.atos.zac.util.SerializableByYasson;

public record WerklijstRechten(
                               boolean inbox,
                               boolean ontkoppeldeDocumentenVerwijderen,
                               boolean inboxProductaanvragenVerwijderen,
                               boolean zakenTaken,
                               boolean zakenTakenVerdelen,
                               boolean zakenTakenExporteren
) implements SerializableByYasson {

    @JsonbCreator
    public WerklijstRechten(
            @JsonbProperty("inbox") final boolean inbox,
            @JsonbProperty("ontkoppelde_documenten_verwijderen") final boolean ontkoppeldeDocumentenVerwijderen,
            @JsonbProperty("inbox_productaanvragen_verwijderen") final boolean inboxProductaanvragenVerwijderen,
            @JsonbProperty("zaken_taken") final boolean zakenTaken,
            @JsonbProperty("zaken_taken_verdelen") final boolean zakenTakenVerdelen,
            @JsonbProperty("zaken_taken_exporteren") final boolean zakenTakenExporteren
    ) {
        this.inbox = inbox;
        this.ontkoppeldeDocumentenVerwijderen = ontkoppeldeDocumentenVerwijderen;
        this.inboxProductaanvragenVerwijderen = inboxProductaanvragenVerwijderen;
        this.zakenTaken = zakenTaken;
        this.zakenTakenVerdelen = zakenTakenVerdelen;
        this.zakenTakenExporteren = zakenTakenExporteren;
    }


}
