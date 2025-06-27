/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.admin.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import net.atos.zac.util.FlywayIntegrator;

@Entity
@Table(schema = FlywayIntegrator.SCHEMA, name = "automatic_email_confirmation")
@SequenceGenerator(
        schema = FlywayIntegrator.SCHEMA, name = "sq_automatic_email_confirmation", sequenceName = "sq_automatic_email_confirmation", allocationSize = 1
)
public class AutomaticEmailConfirmation {
    @Id
    @GeneratedValue(generator = "sq_automatic_email_confirmation", strategy = GenerationType.SEQUENCE)
    @Column(name = "id_automatic_email_confirmation")
    private Long id;

    @OneToOne
    @JoinColumn(name = "id_zaakafhandelparameters", referencedColumnName = "id_zaakafhandelparameters")
    @NotNull
    private ZaakafhandelParameters zaakafhandelParameters;

    @Column(name = "enabled")
    private boolean enabled = false;

    @Column(name = "template_name")
    private String templateName;

    @Column(name = "email_sender")
    private String emailSender;

    @Column(name = "email_reply")
    private String emailReply;

    public AutomaticEmailConfirmation() {
        // Default constructor
    }

    public AutomaticEmailConfirmation(
            ZaakafhandelParameters zaakafhandelParameters,
            boolean enabled,
            String templateName,
            String emailSender,
            String emailReply
    ) {
        this.zaakafhandelParameters = zaakafhandelParameters;
        this.enabled = enabled;
        this.templateName = templateName;
        this.emailSender = emailSender;
        this.emailReply = emailReply;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ZaakafhandelParameters getZaakafhandelParameters() {
        return zaakafhandelParameters;
    }

    public void setZaakafhandelParameters(ZaakafhandelParameters zaakafhandelParameters) {
        this.zaakafhandelParameters = zaakafhandelParameters;
    }

    public boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getEmailSender() {
        return emailSender;
    }

    public void setEmailSender(String emailSender) {
        this.emailSender = emailSender;
    }

    public String getEmailReply() {
        return emailReply;
    }

    public void setEmailReply(String emailReply) {
        this.emailReply = emailReply;
    }
}
