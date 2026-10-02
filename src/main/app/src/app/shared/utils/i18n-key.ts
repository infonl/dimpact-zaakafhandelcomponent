/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

const CAMEL_CASE_SEGMENT = /^[a-z][a-z0-9]*([A-Z][a-z0-9]*)+$/;

export function isCamelCaseSegment(segment: string) {
  return CAMEL_CASE_SEGMENT.test(segment);
}

export function toI18nKey(key: string) {
  const lastDot = key.lastIndexOf(".");
  const prefix = key.slice(0, lastDot + 1);
  const lastSegment = key.slice(lastDot + 1);
  if (!isCamelCaseSegment(lastSegment)) return key;

  return (
    prefix +
    lastSegment.replace(/[A-Z]/g, (letter) => `-${letter.toLowerCase()}`)
  );
}
