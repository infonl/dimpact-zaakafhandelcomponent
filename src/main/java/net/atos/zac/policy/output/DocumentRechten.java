/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.policy.output;

import jakarta.json.bind.annotation.JsonbCreator;
import jakarta.json.bind.annotation.JsonbProperty;

import net.atos.zac.util.DummyInterface;

public record DocumentRechten(boolean lezen, boolean wijzigen, boolean verwijderen,
                              boolean vergrendelen, boolean ontgrendelen,
                              boolean ondertekenen) implements DummyInterface {

    @JsonbCreator
    public DocumentRechten(
                           @JsonbProperty("lezen") final boolean lezen,
                           @JsonbProperty("wijzigen") final boolean wijzigen,
                           @JsonbProperty("verwijderen") final boolean verwijderen,
                           @JsonbProperty("vergrendelen") final boolean vergrendelen,
                           @JsonbProperty("ontgrendelen") final boolean ontgrendelen,
                           @JsonbProperty("ondertekenen") final boolean ondertekenen) {
        this.lezen = lezen;
        this.verwijderen = verwijderen;
        this.wijzigen = wijzigen;
        this.vergrendelen = vergrendelen;
        this.ontgrendelen = ontgrendelen;
        this.ondertekenen = ondertekenen;
    }
}
