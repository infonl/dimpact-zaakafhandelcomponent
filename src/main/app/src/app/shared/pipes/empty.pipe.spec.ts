/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { EmptyPipe } from "./empty.pipe";

describe(EmptyPipe.name, () => {
  const pipe = new EmptyPipe();

  it.each([
    [undefined, undefined, "-"],
    [undefined, "naam", "-"],
    [{ naam: undefined }, "naam", "-"],
    [{ naam: "" }, "naam", "-"],
    [{ naam: "Jaap" }, "naam", "Jaap"],
    ["", undefined, "-"],
    ["", "naam", "-"],
    ["Jaap", undefined, "Jaap"],
    ["Jaap", "naam", "Jaap"],
    [[], undefined, "-"],
    [[], "naam", "-"],
    [["Jaap"], undefined, "Jaap"],
    [["Jaap"], "naam", "-"],
  ])("%p with key %p should return %s", (object, key, exected) => {
    const result = pipe.transform(object, key);

    expect(result).toBe(exected);
  });
});
