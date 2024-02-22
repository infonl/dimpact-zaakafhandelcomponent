/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.zgw.zrc.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jakarta.json.bind.adapter.JsonbAdapter;
import jakarta.json.bind.annotation.JsonbTypeAdapter;

import org.apache.commons.lang3.builder.EqualsBuilder;

/**
 *
 */
@JsonbTypeAdapter(Point2D.Adapter.class)
public class Point2D {

    private final BigDecimal x;

    private final BigDecimal y;

    public Point2D(final BigDecimal x, final BigDecimal y) {
        this.x = x;
        this.y = y;
    }

    public Point2D(final double x, final double y) {
        this.x = BigDecimal.valueOf(x);
        this.y = BigDecimal.valueOf(y);
    }

    public BigDecimal getX() {
        return x;
    }

    public BigDecimal getY() {
        return y;
    }

    public static class Adapter implements JsonbAdapter<Point2D, List<BigDecimal>> {

        @Override
        public List<BigDecimal> adaptToJson(final Point2D point2D) {
            final List<BigDecimal> coordinates = new ArrayList<>();
            coordinates.add(point2D.x);
            coordinates.add(point2D.y);
            return coordinates;
        }

        @Override
        public Point2D adaptFromJson(final List<BigDecimal> coordinates) {
            return new Point2D(coordinates.get(0), coordinates.get(1));
        }
    }

    @Override
    public final boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Point2D point2D = (Point2D) o;

        return new EqualsBuilder().append(x, point2D.getX()).append(y, point2D.getY()).isEquals();
    }

    @Override
    public final int hashCode() {
        return Objects.hash(x, y);
    }
}
