/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.util

import jakarta.ws.rs.ApplicationPath
import jakarta.ws.rs.core.Application
import nl.info.zac.websocket.SubscriptionType.SubscriptionMessage
import nl.info.zac.websocket.event.ScreenEvent
import org.eclipse.microprofile.openapi.annotations.Components
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition
import org.eclipse.microprofile.openapi.annotations.info.Info
import org.eclipse.microprofile.openapi.annotations.media.Schema

@ApplicationPath("rest")
@AllOpen
@OpenAPIDefinition(
    info = Info(title = "Zaakafhandelcomponent backend API", version = "1.0"),
    // The websocket messages are not part of any REST operation, so they are added to the OpenAPI spec explicitly
    // to generate the frontend types for the websocket contract from it.
    components = Components(
        schemas = [
            Schema(name = "ScreenEvent", implementation = ScreenEvent::class),
            Schema(name = "SubscriptionMessage", implementation = SubscriptionMessage::class)
        ]
    )
)
class JaxRSActivator : Application()
