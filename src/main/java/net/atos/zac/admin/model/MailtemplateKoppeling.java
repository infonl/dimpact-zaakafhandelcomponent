/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.admin.model;

import static nl.info.zac.database.flyway.FlywayIntegrator.SCHEMA;

import java.util.Objects;

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
public class MailtemplateKoppeling implements ZaakafhandelparametersComponent<MailtemplateKoppeling> {

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

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MailtemplateKoppeling that))
            return false;
        return Objects.equals(mailTemplate.getId(), that.mailTemplate.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(mailTemplate.getId());
    }

    @Override
    public boolean isChanged(MailtemplateKoppeling original) {
        return !this.equals(original);
    }

    @Override
    public void modify(MailtemplateKoppeling changes) {
        if (!(changes instanceof MailtemplateKoppeling that))
            throw new IllegalArgumentException("Invalid data type for element modification");
        mailTemplate = that.mailTemplate;
    }

    @Override
    public MailtemplateKoppeling clearId() {
        id = null;
        return this;
    }
}
