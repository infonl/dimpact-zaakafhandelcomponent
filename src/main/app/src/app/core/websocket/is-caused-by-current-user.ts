/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ScreenEvent } from "./model/screen-event";

/**
 * Tells whether the change behind `event` was made by the current user, so that a screen can refresh
 * on its own change without announcing it as somebody else's.
 *
 * Falls back to `false` whenever either side is unknown, so an unattributed event is still announced.
 */
export function isCausedByCurrentUser(
  event: ScreenEvent,
  currentUserId: string | undefined,
) {
  return Boolean(currentUserId) && event.actorUserId === currentUserId;
}
