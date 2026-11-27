/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.websocket.event

/**
 * Resource ids of screen events without an object of their own, such as a list. They are part of the OpenAPI spec as
 * the schema `FixedScreenEventResourceId`, so that the frontend subscribes with a generated type.
 */
object FixedScreenEventResourceId {
    const val ZAAKTYPE_CONFIGURATIES = "zaaktype-configuraties"
}
