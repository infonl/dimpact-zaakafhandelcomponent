/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.document.detacheddocument.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import nl.info.zac.database.flyway.FlywayIntegrator
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

@Entity
@Table(schema = FlywayIntegrator.SCHEMA, name = "ontkoppeld_document")
@SequenceGenerator(
    schema = FlywayIntegrator.SCHEMA,
    name = "sq_ontkoppeld_document",
    sequenceName = "sq_ontkoppeld_document",
    allocationSize = 1
)
open class DetachedDocument {
    @Id
    @GeneratedValue(generator = "sq_ontkoppeld_document", strategy = GenerationType.SEQUENCE)
    @Column(name = "id_ontkoppeld_document")
    var id: Long? = null

    @NotNull
    @Column(name = "uuid_document", nullable = false)
    lateinit var documentUUID: UUID

    @NotBlank
    @Column(name = "id_document", nullable = false)
    lateinit var documentID: String

    @NotBlank
    @Column(name = "id_zaak", nullable = false)
    lateinit var zaakID: String

    @NotNull
    @Column(name = "creatiedatum", nullable = false)
    lateinit var creatiedatum: LocalDate

    @NotBlank
    @Column(name = "titel", nullable = false)
    lateinit var titel: String

    @Column(name = "bestandsnaam")
    var bestandsnaam: String? = null

    @NotNull
    @Column(name = "ontkoppeld_op", nullable = false)
    lateinit var ontkoppeldOp: ZonedDateTime

    @NotBlank
    @Column(name = "id_ontkoppeld_door", nullable = false)
    lateinit var ontkoppeldDoor: String

    @Column(name = "reden")
    var reden: String? = null

    companion object {
        /** Naam van property: [.titel]  */
        const val TITEL_PROPERTY_NAME: String = "titel"

        /** Naam van property: [.creatiedatum]  */
        const val CREATIEDATUM_PROPERTY_NAME: String = "creatiedatum"

        /** Naam van property: [.zaakID]  */
        const val ZAAK_ID_PROPERTY_NAME: String = "zaakID"

        /** Naam van property: [.ontkoppeldDoor]  */
        const val ONTKOPPELD_DOOR_PROPERTY_NAME: String = "ontkoppeldDoor"

        /** Naam van property: [.ontkoppeldOp]  */
        const val ONTKOPPELD_OP_PROPERTY_NAME: String = "ontkoppeldOp"

        /** Naam van property: [.reden]  */
        const val REDEN_PROPERTY_NAME: String = "reden"
    }
}
