/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.zgw.zrc.model;

import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.json.bind.annotation.JsonbTypeDeserializer;
import jakarta.json.bind.annotation.JsonbTypeSerializer;

import net.atos.client.zgw.zrc.util.GeometryJsonbDeserializer;
import nl.info.client.zgw.zrc.jsonb.GeometryJsonbSerializer;

@JsonbTypeDeserializer(GeometryJsonbDeserializer.class)
@JsonbTypeSerializer(GeometryJsonbSerializer.class)
public abstract class Geometry {
    public static final String GEOMETRY_TYPE_NAAM = "type";

    private final GeometryType type;

    /**
     * If set to true indicates that the geometry should be deleted.
     * Used in {@link GeometryJsonbSerializer}.
     * Note that the @JsonbNillable annotation cannot be used here because
     * that is a static annotation, and we need to set this value dynamically.
     */
    @JsonbTransient
    private Boolean markGeometryForDeletion = false;

    protected Geometry(final GeometryType type) {
        this.type = type;
    }

    public Geometry() {
        type = null;
    }

    public GeometryType getType() {
        return type;
    }

    public boolean getMarkGeometryForDeletion() {
        return markGeometryForDeletion;
    }

    public void setMarkGeometryForDeletion() {
        markGeometryForDeletion = true;
    }

    @Override
    public abstract String toString();
}
