/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.mailtemplates.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import net.atos.zac.util.FlywayIntegrator
import nl.info.zac.util.AllOpen

@Entity
@Table(schema = FlywayIntegrator.SCHEMA, name = "mail_template")
@SequenceGenerator(
    schema = FlywayIntegrator.SCHEMA,
    name = "sq_mail_template",
    sequenceName = "sq_mail_template",
    allocationSize = 1
)
@AllOpen
class MailTemplate {
    companion object {
        /**
         * Naam van property: [mail]
         */
        const val MAIL = "mail"

        /**
         * Naam van property: [isDefaultMailtemplate]
         */
        const val DEFAULT_MAILTEMPLATE = "isDefaultMailtemplate"
    }

    @Id
    @GeneratedValue(generator = "sq_mail_template", strategy = GenerationType.SEQUENCE)
    @Column(name = "id_mail_template")
    var id: Long = 0

    @Column(name = "mail_template_naam", nullable = false)
    @NotBlank
    lateinit var mailTemplateNaam: String

    @Column(name = "onderwerp", nullable = false)
    @NotBlank
    lateinit var onderwerp: String

    @Column(name = "body", nullable = false)
    @NotBlank
    lateinit var body: String

    @Column(name = "mail_template_enum", nullable = false)
    @Enumerated(EnumType.STRING)
    lateinit var mail: Mail

    @Column(name = "default_mailtemplate", nullable = false)
    var isDefaultMailtemplate: Boolean = false
}
