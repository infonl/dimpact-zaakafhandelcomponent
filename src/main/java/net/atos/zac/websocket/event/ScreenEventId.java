/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.websocket.event;

import java.util.Objects;

import org.jspecify.annotations.NonNull;

public record ScreenEventId(String resource, String detail) {
    /**
     * Override the default equals to explicitly exclude the 'detail' field,
     * because screen event IDs are considered equal if their 'resource' fields are equal.
     *
     * @param object the object to compare with
     * @return true if equal, false otherwise
     */
    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (!(object instanceof ScreenEventId other))
            return false;
        return resource.equals(other.resource);
    }

    /**
     * Override the default equals to explicitly exclude the 'detail' field,
     * because screen event IDs are considered equal if their 'resource' fields are equal.
     *
     * @return hash code based on the 'resource' field
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(resource);
    }

    /**
     * String representation of the ScreenEventId.
     * This string format is used in the ZAC WebSocket code and should not be changed.
     *
     * @return string representation
     */
    @Override
    public @NonNull String toString() {
        return detail != null ? String.format("%s;%s", resource, detail) : resource;
    }
}
