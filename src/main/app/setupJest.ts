/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import "whatwg-fetch";

Object.defineProperty(HTMLElement.prototype, "animate", {
  value: jest.fn(),
  writable: true,
});

Object.defineProperty(window, "addEventListener", {
  value: jest.fn(),
});
Object.defineProperty(window, "removeEventListener", {
  value: jest.fn(),
});
Object.defineProperty(HTMLElement.prototype, "addEventListener", {
  value: jest.fn(),
});
