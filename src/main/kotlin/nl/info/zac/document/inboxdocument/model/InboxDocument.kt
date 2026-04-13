/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.document.inboxdocument.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import nl.info.zac.database.flyway.FlywayIntegrator.Companion.SCHEMA
import nl.info.zac.util.AllOpen
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(schema = SCHEMA, name = "inbox_document")
@SequenceGenerator(schema = SCHEMA, name = "sq_inbox_document", sequenceName = "sq_inbox_document", allocationSize = 1)
@AllOpen
class InboxDocument {
    companion object {
        const val ENKELVOUDIGINFORMATIEOBJECT_ID_PROPERTY_NAME = "enkelvoudiginformatieobjectID"
        const val ENKELVOUDIGINFORMATIEOBJECT_UUID_PROPERTY_NAME = "enkelvoudiginformatieobjectUUID"
        const val TITEL_PROPERTY_NAME = "titel"
        const val CREATIE_DATUM_PROPERTY_NAME = "creatiedatum"
    }

    @Id
    @GeneratedValue(generator = "sq_inbox_document", strategy = GenerationType.SEQUENCE)
    @Column(name = "id_inbox_document")
    var id: Long? = null

    @NotNull
    @Column(name = "uuid_enkelvoudiginformatieobject", nullable = false)
    lateinit var enkelvoudiginformatieobjectUUID: UUID

    @NotBlank
    @Column(name = "id_enkelvoudiginformatieobject", nullable = false)
    lateinit var enkelvoudiginformatieobjectID: String

    @NotNull
    @Column(name = "creatiedatum", nullable = false)
    lateinit var creatiedatum: LocalDate

    @NotBlank
    @Column(name = "titel", nullable = false)
    lateinit var titel: String

    @Column(name = "bestandsnaam")
    var bestandsnaam: String? = null
}
