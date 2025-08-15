/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.admin.converter;

import static nl.info.zac.util.HtmlUtilsKt.stripHtmlParagraphTags;

import net.atos.zac.app.admin.model.RESTMailtemplate;
import nl.info.zac.mailtemplates.model.Mail;
import nl.info.zac.mailtemplates.model.MailTemplate;

public final class RESTMailtemplateConverter {

    public static RESTMailtemplate convert(final MailTemplate mailTemplate) {
        final RESTMailtemplate restMailtemplate = new RESTMailtemplate();
        restMailtemplate.id = mailTemplate.getId();
        restMailtemplate.mailTemplateNaam = mailTemplate.getMailTemplateNaam();
        restMailtemplate.mail = mailTemplate.getMail().name();
        restMailtemplate.variabelen = mailTemplate.getMail().getMailTemplateVariables();
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

    /**
     * Converts RESTMailtemplate to MailTemplate for create operations.
     * Explicitly does NOT set ID on domain model to allow database auto-generation.
     *
     * @param restMailtemplate the REST model to convert
     * @return MailTemplate domain model without ID set
     */
    public static MailTemplate convertForCreate(final RESTMailtemplate restMailtemplate) {
        final MailTemplate mailTemplate = new MailTemplate();
        // Explicitly do NOT set ID - let database generate it
        mailTemplate.setMail(Mail.valueOf(restMailtemplate.mail));
        mailTemplate.setMailTemplateNaam(restMailtemplate.mailTemplateNaam);
        mailTemplate.setOnderwerp(stripHtmlParagraphTags(restMailtemplate.onderwerp));
        mailTemplate.setBody(restMailtemplate.body);
        mailTemplate.setDefaultMailtemplate(restMailtemplate.defaultMailtemplate);
        return mailTemplate;
    }

    /**
     * Converts RESTMailtemplate to MailTemplate for update operations.
     * Handles field mapping without ID concerns - ID will be set by the service layer.
     *
     * @param restMailtemplate the REST model to convert
     * @return MailTemplate domain model with fields mapped (ID not set)
     */
    public static MailTemplate convertForUpdate(final RESTMailtemplate restMailtemplate) {
        final MailTemplate mailTemplate = new MailTemplate();
        // ID will be set by the service layer from the path parameter
        mailTemplate.setMail(Mail.valueOf(restMailtemplate.mail));
        mailTemplate.setMailTemplateNaam(restMailtemplate.mailTemplateNaam);
        mailTemplate.setOnderwerp(stripHtmlParagraphTags(restMailtemplate.onderwerp));
        mailTemplate.setBody(restMailtemplate.body);
        mailTemplate.setDefaultMailtemplate(restMailtemplate.defaultMailtemplate);
        return mailTemplate;
    }
}
