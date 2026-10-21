/*
 * SPDX-FileCopyrightText: 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.websocket.event

import nl.info.zac.event.Opcode

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
    resource: String = "fakeResource",
    detail: String = "fakeDetail"
) =
    ScreenEventId(
        resource,
        detail
    )
