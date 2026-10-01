/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

export const MIN_DELAY_MS = 1000;
export const FIRST_SPREAD_MS = 5000;
export const MAX_SPREAD_MS = 60_000;

export const reconnectDelay = (attempt: number, random = Math.random) =>
  MIN_DELAY_MS +
  random() * Math.min(MAX_SPREAD_MS, FIRST_SPREAD_MS * 2 ** attempt);
