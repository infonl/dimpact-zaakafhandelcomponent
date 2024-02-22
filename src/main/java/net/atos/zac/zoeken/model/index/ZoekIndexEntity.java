/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.zoeken.model.index;

import static net.atos.zac.util.FlywayIntegrator.SCHEMA;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(schema = SCHEMA, name = "zoek_index")
@SequenceGenerator(
    schema = SCHEMA,
    name = "sq_zoek_index",
    sequenceName = "sq_zoek_index",
    allocationSize = 1)
public class ZoekIndexEntity {

  public static final String OBJECT_ID = "objectId";

  public static final String TYPE = "type";

  public static final String STATUS = "status";

  @Id
  @GeneratedValue(generator = "sq_zoek_index", strategy = GenerationType.SEQUENCE)
  @Column(name = "id_zoek_index")
  private Long id;

  @NotNull @Column(name = "object_id", nullable = false)
  private String objectId;

  @NotNull @Column(name = "type", nullable = false)
  private String type;

  @NotNull @Column(name = "status", nullable = false)
  private String status;

  public ZoekIndexEntity() {
    super();
  }

  public ZoekIndexEntity(final String uuid, final ZoekObjectType type, final IndexStatus status) {
    this.objectId = uuid;
    this.type = type.toString();
    this.status = status.toString();
  }

  public Long getId() {
    return id;
  }

  public void setId(final Long id) {
    this.id = id;
  }

  public String getObjectId() {
    return objectId;
  }

  public void setObjectId(final String objectId) {
    this.objectId = objectId;
  }

  public String getType() {
    return type;
  }

  public void setType(final ZoekObjectType type) {
    this.type = type.toString();
  }

  public IndexStatus getStatus() {
    return IndexStatus.valueOf(status);
  }

  public void setStatus(final IndexStatus status) {
    this.status = status.toString();
  }
}
