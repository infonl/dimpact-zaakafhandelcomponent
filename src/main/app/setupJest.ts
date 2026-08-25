/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { HttpTestingController } from "@angular/common/http/testing";
import "@angular/compiler";
import { TestBed } from "@angular/core/testing";
import { QueryClient } from "@tanstack/angular-query-experimental";
import "@testing-library/jest-dom";

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

// jsdom does not implement matchMedia
Object.defineProperty(window, "matchMedia", {
  writable: true,
  value: (query: string) =>
    ({
      matches: false,
      media: query,
      onchange: null,
      addListener: jest.fn(),
      removeListener: jest.fn(),
      addEventListener: jest.fn(),
      removeEventListener: jest.fn(),
      dispatchEvent: jest.fn(),
    }) as unknown as MediaQueryList,
});

// jsdom resolves the full cascade, walking every ancestor and every style rule, on
// each `getComputedStyle` call and caches nothing. Testing Library calls it once per
// element per ancestor while computing accessible names, which dominates the runtime of
// every DOM query. jsdom bumps `_version` on the document for any mutation in the
// attached tree, so it is a synchronous invalidation token for a memo of that resolution.
const jsdomImplementationSymbol = Object.getOwnPropertySymbols(document).find(
  (symbol) => symbol.description === "impl",
);
const documentImplementation = jsdomImplementationSymbol
  ? (
      document as unknown as Record<
        symbol,
        {
          _version: number;
          _nwsapiDontThrow?: {
            match: (selector: string, element: Element) => boolean;
          };
        }
      >
    )[jsdomImplementationSymbol]
  : undefined;

if (typeof documentImplementation?._version === "number") {
  const resolveComputedStyle = window.getComputedStyle.bind(window);

  // Resolving a single computed style matches all rules of the jsdom user-agent
  // stylesheet against the element and, for the inherited `visibility`, against each of
  // its ancestors as well. Memoizing the selector matcher jsdom uses for that collapses
  // the repeated work; the same document version guards it.
  resolveComputedStyle(document.documentElement);
  const selectorMatcher = documentImplementation._nwsapiDontThrow;
  if (selectorMatcher) {
    const matchSelector = selectorMatcher.match.bind(selectorMatcher);
    let matchedVersion = -1;
    let matchesBySelector = new WeakMap<Element, Map<string, boolean>>();

    selectorMatcher.match = (selector: string, element: Element) => {
      if (documentImplementation._version !== matchedVersion) {
        matchedVersion = documentImplementation._version;
        matchesBySelector = new WeakMap();
      }
      let matched = matchesBySelector.get(element);
      if (!matched) {
        matched = new Map();
        matchesBySelector.set(element, matched);
      }
      const memoized = matched.get(selector);
      if (memoized !== undefined) {
        return memoized;
      }
      const result = matchSelector(selector, element);
      matched.set(selector, result);
      return result;
    };
  }

  let memoizedVersion = -1;
  let memoizedStyles = new WeakMap<Element, CSSStyleDeclaration>();

  window.getComputedStyle = (
    element: Element,
    pseudoElement?: string | null,
  ) => {
    if (pseudoElement) {
      return resolveComputedStyle(element, pseudoElement);
    }
    if (documentImplementation._version !== memoizedVersion) {
      memoizedVersion = documentImplementation._version;
      memoizedStyles = new WeakMap();
    }
    const memoized = memoizedStyles.get(element);
    if (memoized) {
      return memoized;
    }
    const computedStyle = resolveComputedStyle(element);
    memoizedStyles.set(element, computedStyle);
    return computedStyle;
  };
}

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

export const MUTATION_TIMEOUT = 50;
export const testQueryClient = new QueryClient({
  defaultOptions: { queries: { retry: false } },
});
export const mockMutationFn = (timeout = MUTATION_TIMEOUT) =>
  new Promise((resolve) => sleep(timeout).then(resolve));

afterEach(() => {
  // Only the specs that provide `provideHttpClientTesting()` have one to verify.
  // Asking for it instantiates the test module, so hand it back reset — this hook
  // runs after the one the Angular preset uses to do that itself.
  try {
    TestBed.inject(HttpTestingController, null, { optional: true })?.verify();
  } finally {
    TestBed.resetTestingModule();
  }
  jest.clearAllMocks();
  testQueryClient.clear();
});

export function sleep(ms: number = 0) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}
