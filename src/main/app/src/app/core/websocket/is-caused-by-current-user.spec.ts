/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { isCausedByCurrentUser } from "./is-caused-by-current-user";
import { ObjectType } from "./model/object-type";
import { Opcode } from "./model/opcode";
import { ScreenEvent } from "./model/screen-event";
import { ScreenEventId } from "./model/screen-event-id";

function screenEvent(actorUserId?: string) {
  return new ScreenEvent(
    Opcode.UPDATED,
    ObjectType.ZAAK,
    new ScreenEventId("zaak-uuid"),
    undefined,
    actorUserId,
  );
}

describe(isCausedByCurrentUser.name, () => {
  it("recognises an event caused by the current user", () => {
    expect(isCausedByCurrentUser(screenEvent("user-1"), "user-1")).toBe(true);
  });

  it("does not recognise an event caused by somebody else", () => {
    expect(isCausedByCurrentUser(screenEvent("user-2"), "user-1")).toBe(false);
  });

  it("does not recognise an event without an actor, so a change of unknown origin is announced", () => {
    expect(isCausedByCurrentUser(screenEvent(), "user-1")).toBe(false);
  });

  it("does not recognise any event while the current user is unknown", () => {
    expect(isCausedByCurrentUser(screenEvent("user-1"), undefined)).toBe(false);
  });
});
