/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.admin.converter;

import static nl.info.zac.util.HtmlUtilsKt.stripHtmlParagraphTags;

import net.atos.zac.app.admin.model.RESTMailtemplate;
import net.atos.zac.mailtemplates.model.Mail;
import nl.info.zac.mailtemplates.model.MailTemplate;

public final class RESTMailtemplateConverter {

    public static RESTMailtemplate convert(final MailTemplate mailTemplate) {
        final RESTMailtemplate restMailtemplate = new RESTMailtemplate();
        restMailtemplate.id = mailTemplate.getId();
        restMailtemplate.mailTemplateNaam = mailTemplate.getMailTemplateNaam();
        restMailtemplate.mail = mailTemplate.getMail().name();
        restMailtemplate.variabelen = mailTemplate.getMail().getVariabelen();
        restMailtemplate.onderwerp = mailTemplate.getOnderwerp();
        restMailtemplate.body = mailTemplate.getBody();
        restMailtemplate.defaultMailtemplate = mailTemplate.isDefaultMailtemplate();
        return restMailtemplate;
    }

    public static MailTemplate convert(final RESTMailtemplate restMailtemplate) {
        final MailTemplate mailTemplate = new MailTemplate();
        mailTemplate.setId(restMailtemplate.id);
        mailTemplate.setMail(Mail.valueOf(restMailtemplate.mail));
        mailTemplate.setMailTemplateNaam(restMailtemplate.mailTemplateNaam);
        mailTemplate.setOnderwerp(stripHtmlParagraphTags(restMailtemplate.onderwerp));
        mailTemplate.setBody(restMailtemplate.body);
        mailTemplate.setDefaultMailtemplate(restMailtemplate.defaultMailtemplate);
        return mailTemplate;
    }
}
