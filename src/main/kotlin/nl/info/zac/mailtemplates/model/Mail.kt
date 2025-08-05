/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.mailtemplates.model

import net.atos.zac.mailtemplates.model.MailTemplateVariables
import nl.info.zac.mailtemplates.model.Mail.TAAK_AANVULLENDE_INFORMATIE
import nl.info.zac.mailtemplates.model.Mail.TAAK_ADVIES_EXTERN
import nl.info.zac.mailtemplates.model.Mail.TAAK_ONTVANGSTBEVESTIGING
import nl.info.zac.mailtemplates.model.Mail.ZAAK_AFGEHANDELD
import nl.info.zac.mailtemplates.model.Mail.ZAAK_ALGEMEEN
import nl.info.zac.mailtemplates.model.Mail.ZAAK_NIET_ONTVANKELIJK
import nl.info.zac.mailtemplates.model.Mail.ZAAK_ONTVANKELIJK

enum class Mail(val mailTemplateVariables: Set<MailTemplateVariables>) {
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
    SIGNALERING_TAAK_VERLOPEN(MailTemplateVariables.TAAK_SIGNALERING_VARIABELEN)
}

fun getLinkableMails() = listOf(
    ZAAK_ALGEMEEN,
    ZAAK_ONTVANKELIJK,
    ZAAK_NIET_ONTVANKELIJK,
    ZAAK_AFGEHANDELD,
    TAAK_AANVULLENDE_INFORMATIE,
    TAAK_ADVIES_EXTERN,
    TAAK_ONTVANGSTBEVESTIGING
)
