/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.enkelvoudiginformatieobject.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

import static net.atos.zac.util.FlywayIntegrator.SCHEMA;

@Entity
@Table(schema = SCHEMA, name = "enkelvoudiginformatieobject_lock")
@SequenceGenerator(schema = SCHEMA, name = "sq_enkelvoudiginformatieobject_lock", sequenceName =
        "sq_enkelvoudiginformatieobject_lock", allocationSize = 1)
public class EnkelvoudigInformatieObjectLock {

    @Id
    @GeneratedValue(generator = "sq_enkelvoudiginformatieobject_lock", strategy = GenerationType.SEQUENCE)
    @Column(name = "id_enkelvoudiginformatieobject_lock")
    private Long id;

    @NotNull
    @Column(name = "uuid_enkelvoudiginformatieobject", nullable = false)
    private UUID enkelvoudiginformatieobjectUUID;

    @NotBlank
    @Column(name = "id_user", nullable = false)
    private String userId;

    @NotBlank
    @Column(name = "lock", nullable = false)
    private String lock;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public UUID getEnkelvoudiginformatieobjectUUID() {
        return enkelvoudiginformatieobjectUUID;
    }

    public void setEnkelvoudiginformatieobjectUUID(final UUID enkelvoudiginformatieobjectUUID) {
        this.enkelvoudiginformatieobjectUUID = enkelvoudiginformatieobjectUUID;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(final String userId) {
        this.userId = userId;
    }

    public String getLock() {
        return lock;
    }

    public void setLock(final String lock) {
        this.lock = lock;
    }
}
