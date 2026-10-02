/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

const I18N_KEY = /^[a-z0-9]+(-[a-z0-9]+)*(\.[a-z0-9]+(-[a-z0-9]+)*)*$/;

export function isI18nKey(key: string) {
  return I18N_KEY.test(key);
}

export function toI18nKey(key: string) {
  return key.split(".").map(toKebabCaseSegment).join(".");
}

function toKebabCaseSegment(segment: string) {
  return segment
    .replace(/([a-z0-9])([A-Z])/g, "$1-$2")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-|-$/g, "");
}
