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
@Table(schema = SCHEMA, name = "zaaktype_cmmn_mailtemplate_parameters")
@SequenceGenerator(schema = SCHEMA, name = "sq_zaaktype_cmmn_mailtemplate_parameters", sequenceName = "sq_zaaktype_cmmn_mailtemplate_parameters", allocationSize = 1)
public class ZaaktypeCmmnMailtemplateParameters implements UserModifiable<ZaaktypeCmmnMailtemplateParameters> {

    @Id
    @GeneratedValue(generator = "sq_zaaktype_cmmn_mailtemplate_parameters", strategy = GenerationType.SEQUENCE)
    @Column(name = "id")
    private Long id;

    @NotNull @ManyToOne
    @JoinColumn(name = "zaaktype_configuration_id", referencedColumnName = "id")
    private ZaaktypeCmmnConfiguration zaaktypeCmmnConfiguration;

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

    public ZaaktypeCmmnConfiguration getZaaktypeCmmnConfiguration() {
        return zaaktypeCmmnConfiguration;
    }

    public void setZaaktypeCmmnConfiguration(final ZaaktypeCmmnConfiguration zaaktypeCmmnConfiguration) {
        this.zaaktypeCmmnConfiguration = zaaktypeCmmnConfiguration;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ZaaktypeCmmnMailtemplateParameters that))
            return false;
        if (mailTemplate == null || that.mailTemplate == null)
            throw new IllegalStateException("mailTemplate is null");
        return Objects.equals(mailTemplate.getId(), that.mailTemplate.getId());
    }

    @Override
    public int hashCode() {
        if (mailTemplate == null)
            throw new IllegalStateException("mailTemplate is null");
        return Objects.hash(mailTemplate.getId());
    }

    @Override
    public boolean isModifiedFrom(ZaaktypeCmmnMailtemplateParameters original) {
        if (mailTemplate == null || original.mailTemplate == null)
            throw new IllegalStateException("mailTemplate is null");
        return Objects.equals(mailTemplate.mail, original.mailTemplate.mail) &&
               !Objects.equals(mailTemplate.getId(), original.mailTemplate.getId());
    }

    @Override
    public void applyChanges(ZaaktypeCmmnMailtemplateParameters changes) {
        mailTemplate = changes.mailTemplate;
    }

    @Override
    public ZaaktypeCmmnMailtemplateParameters resetId() {
        id = null;
        return this;
    }
}
