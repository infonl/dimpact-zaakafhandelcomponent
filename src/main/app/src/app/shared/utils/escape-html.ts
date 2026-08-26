/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

const REPLACEMENTS: Record<string, string> = {
  "&": "&amp;", // keeps an already-escaped entity literal
  "<": "&lt;", // opens a tag
  ">": "&gt;", // completes one
  '"': "&quot;", // ends a double-quoted attribute
  "'": "&#39;", // ends a single-quoted one; &apos; is not HTML4
};

export function escapeHtml(value: string) {
  return value.replace(
    /[&<>"']/g,
    (character) => REPLACEMENTS[character] ?? character,
  );
}
