/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { isCausedByCurrentUser } from "./is-caused-by-current-user";
import { ObjectType } from "./model/object-type";
import { Opcode } from "./model/opcode";
import { ScreenEvent } from "./model/screen-event";
import { ScreenEventId } from "./model/screen-event-id";

function screenEvent(actorUsername?: string) {
  return new ScreenEvent(
    Opcode.UPDATED,
    ObjectType.ZAAK,
    new ScreenEventId("zaak-uuid"),
    undefined,
    actorUsername,
  );
}

describe(isCausedByCurrentUser.name, () => {
  it("recognises an event caused by the current user", () => {
    expect(
      isCausedByCurrentUser(screenEvent("fakeUsername1"), "fakeUsername1"),
    ).toBe(true);
  });

  it("does not recognise an event caused by somebody else", () => {
    expect(
      isCausedByCurrentUser(screenEvent("fakeUsername2"), "fakeUsername1"),
    ).toBe(false);
  });

  it("does not recognise an event without an actor, so a change of unknown origin is announced", () => {
    expect(isCausedByCurrentUser(screenEvent(), "fakeUsername1")).toBe(false);
  });

  it("does not recognise any event while the current user is unknown", () => {
    expect(isCausedByCurrentUser(screenEvent("fakeUsername1"), undefined)).toBe(
      false,
    );
  });
});
