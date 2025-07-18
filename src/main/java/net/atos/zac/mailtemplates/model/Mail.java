/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.mailtemplates.model;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public enum Mail {
    ZAAK_ALGEMEEN(MailTemplateVariables.ZAAK_VOORTGANG_VARIABELEN),
    ZAAK_ONTVANKELIJK(MailTemplateVariables.ZAAK_VOORTGANG_VARIABELEN),
    ZAAK_NIET_ONTVANKELIJK(MailTemplateVariables.ZAAK_VOORTGANG_VARIABELEN),
    ZAAK_AFGEHANDELD(MailTemplateVariables.ZAAK_VOORTGANG_VARIABELEN),
    TAAK_AANVULLENDE_INFORMATIE(MailTemplateVariables.ACTIE_VARIABELEN),
    TAAK_ONTVANGSTBEVESTIGING(MailTemplateVariables.ACTIE_VARIABELEN),
    TAAK_ADVIES_EXTERN(MailTemplateVariables.ACTIE_VARIABELEN),
    SIGNALERING_ZAAK_DOCUMENT_TOEGEVOEGD(MailTemplateVariables.DOCUMENT_SIGNALERING_VARIABELEN),
    SIGNALERING_ZAAK_OP_NAAM(MailTemplateVariables.ZAAK_SIGNALERING_VARIABELEN),
    SIGNALERING_ZAAK_VERLOPEND_STREEFDATUM(MailTemplateVariables.ZAAK_SIGNALERING_VARIABELEN),
    SIGNALERING_ZAAK_VERLOPEND_FATALE_DATUM(MailTemplateVariables.ZAAK_SIGNALERING_VARIABELEN),
    SIGNALERING_TAAK_OP_NAAM(MailTemplateVariables.TAAK_SIGNALERING_VARIABELEN),
    SIGNALERING_TAAK_VERLOPEN(MailTemplateVariables.TAAK_SIGNALERING_VARIABELEN);

    private final Set<MailTemplateVariables> variabelen;

    Mail(final Set<MailTemplateVariables> variabelen) {
        this.variabelen = Collections.unmodifiableSet(variabelen);
    }

    public Set<MailTemplateVariables> getVariabelen() {
        return variabelen;
    }

    public static List<Mail> getKoppelbareMails() {
        return Stream.of(ZAAK_ALGEMEEN, ZAAK_ONTVANKELIJK, ZAAK_NIET_ONTVANKELIJK, ZAAK_AFGEHANDELD,
                TAAK_AANVULLENDE_INFORMATIE, TAAK_ADVIES_EXTERN, TAAK_ONTVANGSTBEVESTIGING).toList();
    }
}
