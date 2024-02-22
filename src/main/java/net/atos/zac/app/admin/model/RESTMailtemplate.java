/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.admin.model;

import java.util.Set;

import net.atos.zac.mailtemplates.model.MailTemplateVariabelen;

public class RESTMailtemplate {

    public Long id;

    public String mailTemplateNaam;

    public String onderwerp;

    public String body;

    public String mail;

    public Set<MailTemplateVariabelen> variabelen;

    public boolean defaultMailtemplate;
}
