/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { reconnectDelay } from "./reconnect-delay";

describe(reconnectDelay.name, () => {
  it.each([
    { attempt: 0, longest: 6000 },
    { attempt: 1, longest: 11_000 },
    { attempt: 2, longest: 21_000 },
    { attempt: 3, longest: 41_000 },
    { attempt: 4, longest: 61_000 },
    { attempt: 20, longest: 61_000 },
  ])(
    "waits between 1000 and $longest ms before reconnect attempt $attempt",
    ({ attempt, longest }) => {
      expect(reconnectDelay(attempt, () => 0)).toBe(1000);
      expect(reconnectDelay(attempt, () => 1)).toBe(longest);
    },
  );

  it("spreads the delay randomly within the window", () => {
    expect(reconnectDelay(0, () => 0.5)).toBe(3500);
  });
});
