/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.zgw.zrc.model;

import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.builder.EqualsBuilder;


public class Polygon extends Geometry {
    private List<List<Point2D>> coordinates;

    public Polygon() {
        super(GeometryType.POLYGON);
    }

    public Polygon(final List<List<Point2D>> coordinates) {
        super(GeometryType.POLYGON);
        this.coordinates = coordinates;
    }

    public List<List<Point2D>> getCoordinates() {
        return coordinates;
    }

    @Override
    public String toString() {
        return "Polygon{coordinates=" + coordinates + "}";
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Polygon polygon = (Polygon) o;
        return new EqualsBuilder().append(super.getType(), polygon.getType()).append(coordinates, polygon.coordinates).isEquals();
    }

    @Override
    public int hashCode() {
        return Objects.hash(coordinates);
    }
}
