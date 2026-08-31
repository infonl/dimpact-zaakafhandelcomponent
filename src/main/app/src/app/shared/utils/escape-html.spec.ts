/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { escapeHtml } from "./escape-html";

describe(escapeHtml.name, () => {
  it.each([
    ["&", "&amp;"],
    ["<", "&lt;"],
    [">", "&gt;"],
    ['"', "&quot;"],
    ["'", "&#39;"],
  ])("should escape %s", (character, entity) => {
    expect(escapeHtml(character)).toBe(entity);
  });

  it("should escape every occurrence, not only the first", () => {
    expect(escapeHtml("<b><i>")).toBe("&lt;b&gt;&lt;i&gt;");
  });

  it("should leave text holding none of them untouched", () => {
    expect(escapeHtml("ZAAK-2026-0000000835")).toBe("ZAAK-2026-0000000835");
  });

  it("should escape the ampersand it introduces itself only once", () => {
    expect(escapeHtml("&lt;")).toBe("&amp;lt;");
  });

  it("should render a script tag harmless", () => {
    const escaped = escapeHtml('<img src=x onerror="alert(1)">');

    expect(escaped).not.toContain("<");
    expect(escaped).not.toContain('"');
    expect(escaped).toBe("&lt;img src=x onerror=&quot;alert(1)&quot;&gt;");
  });
});
