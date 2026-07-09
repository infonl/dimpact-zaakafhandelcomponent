/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

/**
 * Create a partial object for testing purposes.
 * Replacement for @total-typescript/shoehorn's fromPartial function.
 */
export function fromPartial<T>(partial: Partial<T>): T {
  return partial as T;
}

/**
 * Type assertion helper for tests.
 * Replacement for ts-expect's expectType function.
 */
export function expectType<T>(_value: T): void {
  // This is a compile-time only check
}
