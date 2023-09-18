/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.websocket.event

import net.atos.zac.event.Opcode

fun createScreenEvent(
    opcode: Opcode = Opcode.CREATED,
    screenEventType: ScreenEventType = ScreenEventType.ZAAK,
    screenEventId: ScreenEventId = createScreenEventId()
) =
    ScreenEvent(
        opcode,
        screenEventType,
        screenEventId
    )

fun createScreenEventId(
    resource: String = "dummyResource",
    detail: String = "dummyDetail"
) =
    ScreenEventId(
        resource,
        detail
    )
