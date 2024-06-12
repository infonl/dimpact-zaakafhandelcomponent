/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.enkelvoudiginformatieobject.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import net.atos.zac.util.FlywayIntegrator
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import java.util.UUID

@Entity
@Table(schema = FlywayIntegrator.SCHEMA, name = "enkelvoudiginformatieobject_lock")
@SequenceGenerator(
    schema = FlywayIntegrator.SCHEMA,
    name = "sq_enkelvoudiginformatieobject_lock",
    sequenceName = "sq_enkelvoudiginformatieobject_lock",
    allocationSize = 1
)
@AllOpen
@NoArgConstructor
class EnkelvoudigInformatieObjectLock {
    @Id
    @GeneratedValue(generator = "sq_enkelvoudiginformatieobject_lock", strategy = GenerationType.SEQUENCE)
    @Column(name = "id_enkelvoudiginformatieobject_lock")
    var id: Long = 0

    @Column(name = "uuid_enkelvoudiginformatieobject", nullable = false)
    lateinit var enkelvoudiginformatieobjectUUID: UUID

    @Column(name = "id_user", nullable = false)
    lateinit var userId: @NotBlank String

    @Column(name = "lock", nullable = false)
    lateinit var lock: @NotBlank String
}
