/*
 * SPDX-FileCopyrightText: 2021 Atos, 2023 Lifely
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


@JsonbTypeAdapter(Point2D.Adapter.class)
public class Point2D {
    private final BigDecimal longitude;
    private final BigDecimal latitude;

    public Point2D(final BigDecimal latitude, final BigDecimal longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Point2D(final double latitude, final double longitude) {
        this.latitude = BigDecimal.valueOf(latitude);
        this.longitude = BigDecimal.valueOf(longitude);
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public static class Adapter implements JsonbAdapter<Point2D, List<BigDecimal>> {

        @Override
        public List<BigDecimal> adaptToJson(final Point2D point2D) {
            final List<BigDecimal> coordinates = new ArrayList<>();
            coordinates.add(point2D.latitude);
            coordinates.add(point2D.longitude);
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

        return new EqualsBuilder().append(longitude, point2D.getLongitude()).append(latitude, point2D.getLatitude()).isEquals();
    }

    @Override
    public final int hashCode() {
        return Objects.hash(longitude, latitude);
    }
}
