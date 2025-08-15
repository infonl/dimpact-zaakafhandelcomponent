/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.admin.model;

import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import nl.info.zac.mailtemplates.model.Mail;
import nl.info.zac.mailtemplates.model.MailTemplateVariables;

public class RESTMailtemplate {

    // ID is optional for POST requests (will be ignored) and required for responses
    public Long id;

    @NotBlank(message = "Mail template name is required")
    public String mailTemplateNaam;

    @NotBlank(message = "Subject is required")
    public String onderwerp;

    @NotBlank(message = "Body is required")
    public String body;

    @NotNull(message = "Mail type is required")
    public Mail mail;

    public Set<MailTemplateVariables> variabelen;

    public boolean defaultMailtemplate;
}
