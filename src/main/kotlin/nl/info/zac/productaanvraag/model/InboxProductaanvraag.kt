/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.productaanvraag.model

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
import nl.info.zac.util.AllOpen
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(schema = FlywayIntegrator.SCHEMA, name = "inbox_productaanvraag")
@SequenceGenerator(
    schema = FlywayIntegrator.SCHEMA,
    name = "sq_inbox_productaanvraag",
    sequenceName = "sq_inbox_productaanvraag",
    allocationSize = 1
)
@AllOpen
class InboxProductaanvraag {
    companion object {
        /** Property name of [InboxProductaanvraag.initiatorID] */
        const val INITIATOR = "initiatorID"

        /** Property name of [InboxProductaanvraag.type] */
        const val TYPE = "type"

        /** Property name of [InboxProductaanvraag.ontvangstdatum] */
        const val ONTVANGSTDATUM = "ontvangstdatum"
    }

    @Id
    @GeneratedValue(generator = "sq_inbox_productaanvraag", strategy = GenerationType.SEQUENCE)
    @Column(name = "id_inbox_productaanvraag")
    var id: Long? = null

    @NotNull
    @Column(name = "uuid_productaanvraag_object", nullable = false)
    lateinit var productaanvraagObjectUUID: UUID

    @Column(name = "uuid_aanvraagdocument")
    var aanvraagdocumentUUID: UUID? = null

    @NotNull
    @Column(name = "ontvangstdatum", nullable = false)
    var ontvangstdatum: LocalDate? = null

    @NotBlank
    @Column(name = "productaanvraag_type", nullable = false)
    lateinit var type: String

    @Column(name = "id_initiator")
    var initiatorID: String? = null

    @Column(name = "aantal_bijlagen")
    var aantalBijlagen: Int = 0
}
