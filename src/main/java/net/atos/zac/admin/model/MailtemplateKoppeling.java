/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.admin.model;

import static net.atos.zac.util.FlywayIntegrator.SCHEMA;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import nl.info.zac.mailtemplates.model.MailTemplate;


@Entity
@Table(schema = SCHEMA, name = "mail_template_koppelingen")
@SequenceGenerator(schema = SCHEMA, name = "sq_mail_template_koppelingen", sequenceName = "sq_mail_template_koppelingen", allocationSize = 1)
public class MailtemplateKoppeling {

    @Id
    @GeneratedValue(generator = "sq_mail_template_koppelingen", strategy = GenerationType.SEQUENCE)
    @Column(name = "id_mail_template_koppelingen")
    private Long id;

    @NotNull @ManyToOne
    @JoinColumn(name = "id_zaakafhandelparameters", referencedColumnName = "id_zaakafhandelparameters")
    private ZaakafhandelParameters zaakafhandelParameters;

    @NotNull @ManyToOne
    @JoinColumn(name = "id_mail_template", referencedColumnName = "id_mail_template")
    private MailTemplate mailTemplate;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public MailTemplate getMailTemplate() {
        return mailTemplate;
    }

    public void setMailTemplate(final MailTemplate mailTemplate) {
        this.mailTemplate = mailTemplate;
    }

    public ZaakafhandelParameters getZaakafhandelParameters() {
        return zaakafhandelParameters;
    }

    public void setZaakafhandelParameters(final ZaakafhandelParameters zaakafhandelParameters) {
        this.zaakafhandelParameters = zaakafhandelParameters;
    }
}
