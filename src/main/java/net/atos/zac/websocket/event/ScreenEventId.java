/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.websocket.event;

import org.jspecify.annotations.NonNull;

public record ScreenEventId(String resource, String detail) {
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
