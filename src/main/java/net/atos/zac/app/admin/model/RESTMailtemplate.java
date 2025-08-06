/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.admin.model;

import java.util.Set;

import nl.info.zac.mailtemplates.model.MailTemplateVariables;

public class RESTMailtemplate {

    // When storing a new mail template, the ID currently needs to be provided by client.
    // We should refactor this. The backend (database) should be in control of generating IDs, not the client.
    public Long id;

    public String mailTemplateNaam;

    public String onderwerp;

    public String body;

    // We should use the Mail enum here in future instead of a string
    public String mail;

    public Set<MailTemplateVariables> variabelen;

    public boolean defaultMailtemplate;
}
