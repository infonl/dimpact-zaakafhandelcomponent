/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.websocket.event

data class ScreenEventId(val resource: String, val detail: String?) {
    /**
     * Screen event IDs are considered equal if their 'resource' fields are equal,
     * so the 'detail' field is explicitly excluded.
     */
    override fun equals(other: Any?) = this === other || (other is ScreenEventId && resource == other.resource)

    override fun hashCode() = resource.hashCode()

    /**
     * This string format is used in the ZAC WebSocket code and should not be changed.
     */
    override fun toString() = detail?.let { "$resource;$it" } ?: resource
}
