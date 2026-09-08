/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { EnvironmentInjector, createEnvironmentInjector } from "@angular/core";
import { TestBed } from "@angular/core/testing";
import { TranslateService } from "@ngx-translate/core";
import { QueryClient } from "@tanstack/angular-query-experimental";
import { Subject, of } from "rxjs";
import { webSocket } from "rxjs/webSocket";
import { IdentityService } from "../../identity/identity.service";
import { UtilService } from "../service/util.service";
import { ObjectType } from "./model/object-type";
import { Opcode } from "./model/opcode";
import { SubscriptionMessage } from "./model/subscription-message";
import { SubscriptionType } from "./model/subscription-type";
import { WebsocketService } from "./websocket.service";

jest.mock("rxjs/webSocket", () => ({
  webSocket: jest.fn(),
}));

const flushMicrotasks = () => Promise.resolve();

describe(WebsocketService.name, () => {
  let service: WebsocketService;
  let sockets: Subject<unknown>[];

  beforeEach(() => {
    jest.useFakeTimers();
    sockets = [];
    (webSocket as jest.Mock).mockImplementation(
      (config: { openObserver?: { next: () => void } }) => {
        const socket = new Subject<unknown>();
        // Unlike a plain Subject, the real WebSocketSubject.next() sends a message to the server and
        // does not deliver it back to this connection's own subscribers, so the spy must not call through.
        jest.spyOn(socket, "next").mockImplementation(() => {});
        sockets.push(socket);
        // Deferred to a microtask because the real connection opens asynchronously too: `open()` only
        // assigns `this.connection$` after this mock returns, and resubscribing reads that field.
        queueMicrotask(() => config.openObserver?.next());
        return socket;
      },
    );

    TestBed.configureTestingModule({
      providers: [
        {
          provide: TranslateService,
          useValue: { get: jest.fn().mockReturnValue(of("")) },
        },
        { provide: UtilService, useValue: { openSnackbar: jest.fn() } },
        { provide: QueryClient, useValue: { getQueryData: jest.fn() } },
        {
          provide: IdentityService,
          useValue: {
            readLoggedInUser: jest.fn().mockReturnValue({ queryKey: [] }),
          },
        },
      ],
    });

    service = TestBed.inject(WebsocketService);
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  it("opens a single connection on construction", () => {
    expect(sockets.length).toBe(1);
  });

  it("replays an active subscription on the new connection after the connection closes cleanly", async () => {
    const listener = service.addListener(
      Opcode.UPDATED,
      ObjectType.ZAAK,
      "zaak-1",
      jest.fn(),
    );

    // A clean close (e.g. an idle-timeout proxy) completes the connection instead of erroring it.
    sockets[0].complete();
    jest.advanceTimersByTime(3000);
    await flushMicrotasks();

    expect(sockets.length).toBe(2);
    expect(sockets[1].next).toHaveBeenCalledWith(
      new SubscriptionMessage(SubscriptionType.CREATE, listener.event),
    );
  });

  it("replays an active subscription on the new connection after the connection errors", async () => {
    const listener = service.addListener(
      Opcode.UPDATED,
      ObjectType.ZAAK,
      "zaak-1",
      jest.fn(),
    );

    sockets[0].error(new Error("connection reset"));
    jest.advanceTimersByTime(3000);
    await flushMicrotasks();

    expect(sockets.length).toBe(2);
    expect(sockets[1].next).toHaveBeenCalledWith(
      new SubscriptionMessage(SubscriptionType.CREATE, listener.event),
    );
  });

  it("does not replay a subscription that was removed before the reconnect", async () => {
    const listener = service.addListener(
      Opcode.UPDATED,
      ObjectType.ZAAK,
      "zaak-1",
      jest.fn(),
    );
    service.removeListener(listener);

    sockets[0].complete();
    jest.advanceTimersByTime(3000);
    await flushMicrotasks();

    expect(sockets.length).toBe(2);
    expect(sockets[1].next).not.toHaveBeenCalled();
  });

  it("does not reconnect after the service is destroyed", () => {
    const childInjector = createEnvironmentInjector(
      [WebsocketService],
      TestBed.inject(EnvironmentInjector),
    );
    const scopedSocketIndex = sockets.length;
    const scopedService = childInjector.get(WebsocketService);
    scopedService.addListener(
      Opcode.UPDATED,
      ObjectType.ZAAK,
      "zaak-1",
      jest.fn(),
    );

    childInjector.destroy();
    sockets[scopedSocketIndex].complete();
    jest.advanceTimersByTime(3000);

    expect(sockets.length).toBe(scopedSocketIndex + 1);
  });
});
