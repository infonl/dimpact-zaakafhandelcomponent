/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { EnvironmentInjector, createEnvironmentInjector } from "@angular/core";
import { TestBed } from "@angular/core/testing";
import { TranslateService } from "@ngx-translate/core";
import { QueryClient } from "@tanstack/angular-query-experimental";
import { Subject, of } from "rxjs";
import type { WebSocketSubjectConfig } from "rxjs/webSocket";
import { flushMicrotasks, fromPartial } from "src/test-helpers";
import { IdentityService } from "../../identity/identity.service";
import { UtilService } from "../service/util.service";
import { ObjectType } from "./model/object-type";
import { Opcode } from "./model/opcode";
import { SubscriptionMessage } from "./model/subscription-message";
import { SubscriptionType } from "./model/subscription-type";
import { WEBSOCKET_FACTORY } from "./websocket-factory";
import { WebsocketService } from "./websocket.service";

const LONGEST_FIRST_RECONNECT_DELAY_MS = 6000;
const LONGEST_SECOND_RECONNECT_DELAY_MS = 11_000;
const LONGEST_RECONNECT_DELAY_MS = 61_000;
const POLICY_VIOLATION_CLOSE_CODE = 1008;

describe(WebsocketService.name, () => {
  let service: WebsocketService;
  let sockets: Subject<unknown>[];
  let socketConfigs: WebSocketSubjectConfig<unknown>[];
  let isServerReachable: boolean;
  let fetchQuery: jest.Mock;
  let invalidateQueries: jest.Mock;

  const closeWithCode = (socketIndex: number, code: number) => {
    socketConfigs[socketIndex].closeObserver?.next(
      fromPartial<CloseEvent>({ code }),
    );
    sockets[socketIndex].complete();
  };

  const makeTabVisible = (visibilityState: DocumentVisibilityState) => {
    jest
      .spyOn(document, "visibilityState", "get")
      .mockReturnValue(visibilityState);
    document.dispatchEvent(new Event("visibilitychange"));
  };

  beforeEach(() => {
    jest.useFakeTimers();
    jest.spyOn(Math, "random").mockReturnValue(1);
    sockets = [];
    socketConfigs = [];
    isServerReachable = true;
    fetchQuery = jest.fn().mockResolvedValue({});
    invalidateQueries = jest.fn().mockResolvedValue(undefined);
    const webSocketFactory = jest.fn(
      (config: WebSocketSubjectConfig<unknown>) => {
        const socket = new Subject<unknown>();
        // Unlike a plain Subject, the real WebSocketSubject.next() sends a message to the server and
        // does not deliver it back to this connection's own subscribers, so the spy must not call through.
        jest.spyOn(socket, "next").mockImplementation(() => {});
        sockets.push(socket);
        socketConfigs.push(config);
        // Deferred to a microtask because the real connection opens asynchronously too: `open()` only
        // assigns `this.connection$` after this mock returns, and resubscribing reads that field.
        if (isServerReachable)
          queueMicrotask(() => config.openObserver?.next(new Event("open")));
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
        {
          provide: QueryClient,
          useValue: { getQueryData: jest.fn(), fetchQuery, invalidateQueries },
        },
        {
          provide: IdentityService,
          useValue: {
            readLoggedInUser: jest.fn().mockReturnValue({ queryKey: [] }),
          },
        },
        { provide: WEBSOCKET_FACTORY, useValue: webSocketFactory },
      ],
    });

    service = TestBed.inject(WebsocketService);
  });

  afterEach(() => {
    jest.useRealTimers();
    jest.restoreAllMocks();
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
    jest.advanceTimersByTime(LONGEST_FIRST_RECONNECT_DELAY_MS);
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
    jest.advanceTimersByTime(LONGEST_FIRST_RECONNECT_DELAY_MS);
    await flushMicrotasks();

    expect(sockets.length).toBe(2);
    expect(sockets[1].next).toHaveBeenCalledWith(
      new SubscriptionMessage(SubscriptionType.CREATE, listener.event),
    );
  });

  it("does not refetch any data when the first connection opens", async () => {
    await flushMicrotasks();

    expect(invalidateQueries).not.toHaveBeenCalled();
  });

  it("refetches all data once a connection has opened again, to catch up on events missed while disconnected", async () => {
    await flushMicrotasks();

    sockets[0].complete();
    jest.advanceTimersByTime(LONGEST_FIRST_RECONNECT_DELAY_MS);
    await flushMicrotasks();

    expect(invalidateQueries).toHaveBeenCalledTimes(1);
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
    jest.advanceTimersByTime(LONGEST_FIRST_RECONNECT_DELAY_MS);
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
    jest.advanceTimersByTime(LONGEST_FIRST_RECONNECT_DELAY_MS);

    expect(sockets.length).toBe(scopedSocketIndex + 1);
  });

  it("waits longer before each next attempt while the server stays unreachable", () => {
    jest.advanceTimersByTime(0);
    isServerReachable = false;

    sockets[0].error(new Error("connection reset"));
    jest.advanceTimersByTime(LONGEST_FIRST_RECONNECT_DELAY_MS);
    sockets[1].error(new Error("connection refused"));
    jest.advanceTimersByTime(LONGEST_SECOND_RECONNECT_DELAY_MS - 1);

    expect(sockets.length).toBe(2);

    jest.advanceTimersByTime(1);

    expect(sockets.length).toBe(3);
  });

  it("starts over at the shortest wait once a connection has opened again", async () => {
    isServerReachable = false;
    sockets[0].error(new Error("connection reset"));
    jest.advanceTimersByTime(LONGEST_FIRST_RECONNECT_DELAY_MS);
    sockets[1].error(new Error("connection refused"));
    isServerReachable = true;
    jest.advanceTimersByTime(LONGEST_SECOND_RECONNECT_DELAY_MS);
    await flushMicrotasks();

    sockets[2].error(new Error("connection reset"));
    jest.advanceTimersByTime(LONGEST_FIRST_RECONNECT_DELAY_MS);

    expect(sockets.length).toBe(4);
  });

  it("reconnects immediately when the tab becomes visible while waiting to reconnect", () => {
    sockets[0].error(new Error("connection reset"));

    makeTabVisible("visible");

    expect(sockets.length).toBe(2);
  });

  it("does not reconnect when the tab becomes hidden while waiting to reconnect", () => {
    sockets[0].error(new Error("connection reset"));

    makeTabVisible("hidden");

    expect(sockets.length).toBe(1);
  });

  it("does not open another connection when the tab becomes visible while connected", () => {
    makeTabVisible("visible");

    expect(sockets.length).toBe(1);
  });

  it("checks that the user is still logged in before reconnecting after the server denies the connection", async () => {
    closeWithCode(0, POLICY_VIOLATION_CLOSE_CODE);
    await flushMicrotasks();

    expect(fetchQuery).toHaveBeenCalledWith(
      expect.objectContaining({ staleTime: 0 }),
    );
    expect(sockets.length).toBe(1);

    jest.advanceTimersByTime(LONGEST_FIRST_RECONNECT_DELAY_MS);

    expect(sockets.length).toBe(2);
  });

  it("stops reconnecting after the server denies the connection to a user who is no longer logged in", async () => {
    fetchQuery.mockRejectedValue(new Error("logged out"));

    closeWithCode(0, POLICY_VIOLATION_CLOSE_CODE);
    await flushMicrotasks();
    jest.advanceTimersByTime(LONGEST_RECONNECT_DELAY_MS);

    expect(sockets.length).toBe(1);
  });
});
