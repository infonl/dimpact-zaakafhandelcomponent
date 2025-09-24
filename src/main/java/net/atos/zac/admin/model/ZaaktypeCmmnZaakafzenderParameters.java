/*
 * SPDX-FileCopyrightText: 2023 Atos, 2025 INFO.nl
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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(schema = SCHEMA, name = "zaaktype_cmmn_zaakafzender_parameters")
@SequenceGenerator(schema = SCHEMA, name = "sq_zaaktype_cmmn_zaakafzender_parameters", sequenceName = "sq_zaaktype_cmmn_zaakafzender_parameters", allocationSize = 1)
public class ZaaktypeCmmnZaakafzenderParameters implements UserModifiable<ZaaktypeCmmnZaakafzenderParameters> {

    public enum SpecialMail {
        GEMEENTE,
        MEDEWERKER;

        public boolean is(final String name) {
            return this.name().equals(name);
        }
    }

    @Id
    @GeneratedValue(generator = "sq_zaaktype_cmmn_zaakafzender_parameters", strategy = GenerationType.SEQUENCE)
    @Column(name = "id")
    private Long id;

    @NotNull @ManyToOne
    @JoinColumn(name = "zaaktype_configuration_id", referencedColumnName = "id")
    private ZaaktypeCmmnConfiguration zaaktypeCmmnConfiguration;

    @Column(name = "default_mail", nullable = false)
    private boolean defaultMail;

    @NotBlank @Column(name = "mail", nullable = false)
    private String mail;

    @Column(name = "replyto")
    private String replyTo;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public ZaaktypeCmmnConfiguration getZaaktypeCmmnConfiguration() {
        return zaaktypeCmmnConfiguration;
    }

    public void setZaaktypeCmmnConfiguration(final ZaaktypeCmmnConfiguration zaaktypeCmmnConfiguration) {
        this.zaaktypeCmmnConfiguration = zaaktypeCmmnConfiguration;
    }

    public boolean isDefault() {
        return defaultMail;
    }

    public void setDefault(final boolean defaultMail) {
        this.defaultMail = defaultMail;
    }

    public String getMail() {
        return mail;
    }

    public void setMail(final String mail) {
        this.mail = mail;
    }

    public String getReplyTo() {
        return replyTo;
    }

    public void setReplyTo(final String replyTo) {
        this.replyTo = replyTo;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ZaaktypeCmmnZaakafzenderParameters that))
            return false;
        return defaultMail == that.defaultMail && Objects.equals(mail, that.mail) && Objects.equals(replyTo, that.replyTo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(defaultMail, mail, replyTo);
    }

    @Override
    public boolean isModifiedFrom(ZaaktypeCmmnZaakafzenderParameters original) {
        return Objects.equals(mail, original.mail) && (!defaultMail == original.defaultMail ||
                                                       !Objects.equals(replyTo, original.replyTo));
    }

    @Override
    public void applyChanges(ZaaktypeCmmnZaakafzenderParameters changes) {
        this.defaultMail = changes.defaultMail;
        this.replyTo = changes.replyTo;
    }

    @Override
    public ZaaktypeCmmnZaakafzenderParameters resetId() {
        id = null;
        return this;
    }
}
