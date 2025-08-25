/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import "whatwg-fetch";

const cryptoPolyfill = {
  randomUUID: () => {
    return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, (c) => {
      const r = (Math.random() * 16) | 0;
      const v = c === "x" ? r : (r & 0x3) | 0x8;
      return v.toString(16);
    }) as `${string}-${string}-${string}-${string}-${string}`;
  },
  subtle: {
    encrypt: jest.fn(),
    decrypt: jest.fn(),
    sign: jest.fn(),
    verify: jest.fn(),
    digest: jest.fn(),
    generateKey: jest.fn(),
    deriveKey: jest.fn(),
    deriveBits: jest.fn(),
    importKey: jest.fn(),
    exportKey: jest.fn(),
    wrapKey: jest.fn(),
    unwrapKey: jest.fn(),
  } as SubtleCrypto,
  getRandomValues: jest.fn(),
} as Crypto;

Object.defineProperty(globalThis, "crypto", {
  value: cryptoPolyfill,
  writable: false,
  configurable: false,
});

console.log = jest.fn();
console.warn = jest.fn();
console.error = jest.fn();
console.info = jest.fn();
// console.debug = jest.fn(); // We do want to see debug logs

afterEach(() => {
  jest.clearAllMocks();
});
