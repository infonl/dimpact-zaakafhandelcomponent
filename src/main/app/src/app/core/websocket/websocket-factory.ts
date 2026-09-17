/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { InjectionToken } from "@angular/core";
import { webSocket } from "rxjs/webSocket";

/**
 * `rxjs/webSocket` is a plain function import, not an Angular service, so
 * `jest.mock("rxjs/webSocket")` is the only way to replace it in a spec — and that
 * mock applies to the whole module, including its types, which forces an `as jest.Mock`
 * cast everywhere it's used. Wrapping it behind an injection token instead lets a spec
 * substitute a fake connection through a plain TestBed provider, like any other dependency.
 */
export const WEBSOCKET_FACTORY = new InjectionToken<typeof webSocket>(
  "rxjs/webSocket.webSocket",
  {
    providedIn: "root",
    factory: () => webSocket,
  },
);
