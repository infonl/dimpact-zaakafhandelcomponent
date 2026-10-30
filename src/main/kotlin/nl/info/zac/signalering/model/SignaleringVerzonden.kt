/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.signalering.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import nl.info.zac.database.flyway.FlywayIntegrator.Companion.SCHEMA
import nl.info.zac.util.AllOpen
import java.time.ZonedDateTime

@Entity
@Table(schema = SCHEMA, name = "signalering_verzonden")
@SequenceGenerator(
    schema = SCHEMA,
    name = "sq_signalering_verzonden",
    sequenceName = "sq_signalering_verzonden",
    allocationSize = 1
)
@AllOpen
class SignaleringVerzonden {
    @Id
    @GeneratedValue(generator = "sq_signalering_verzonden", strategy = GenerationType.SEQUENCE)
    @Column(name = "id_signalering_verzonden")
    var id: Long? = null

    @NotNull
    @ManyToOne
    @JoinColumn(name = "signaleringtype_enum", referencedColumnName = "signaleringtype_enum", nullable = false)
    lateinit var type: SignaleringType

    @NotNull
    @Column(name = "targettype_enum", nullable = false)
    @Enumerated(EnumType.STRING)
    lateinit var targettype: SignaleringTarget

    @NotBlank
    @Column(name = "target", nullable = false)
    lateinit var target: String

    @NotBlank
    @Column(name = "subject", nullable = false)
    lateinit var subject: String

    @Column(name = "detail")
    var detail: String? = null

    @NotNull
    @Column(name = "tijdstip", nullable = false)
    lateinit var tijdstip: ZonedDateTime

    val subjecttype: SignaleringSubject
        get() = type.subjecttype
}
