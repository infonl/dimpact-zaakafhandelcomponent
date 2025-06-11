/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable, OnDestroy } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { Subject, forkJoin, of } from "rxjs";
import { delay, retryWhen, switchMap, takeUntil } from "rxjs/operators";
import { WebSocketSubject, webSocket } from "rxjs/webSocket";
import { UtilService } from "../service/util.service";
import { EventCallback } from "./model/event-callback";
import { EventSuspension } from "./model/event-suspension";
import { ObjectType } from "./model/object-type";
import { Opcode } from "./model/opcode";
import { ScreenEvent } from "./model/screen-event";
import { ScreenEventId } from "./model/screen-event-id";
import { SubscriptionMessage } from "./model/subscription-message";
import { SubscriptionType } from "./model/subscription-type";
import { WebsocketListener } from "./model/websocket-listener";

type SocketMessage = {
  opcode: Opcode;
  objectType: ObjectType;
  objectId: ScreenEventId;
  timestamp?: number;
};

@Injectable({
  providedIn: "root",
})
export class WebsocketService implements OnDestroy {
  // This must be bigger than the SECONDS_TO_DELAY defined in ScreenEventObserver.java
  private static DEFAULT_SUSPENSION_TIMEOUT = 5; // seconds

  private readonly PROTOCOL: string = window.location.protocol.replace(
    /^http/,
    "ws",
  );

  private readonly HOST: string = window.location.host.replace(
    "localhost:4200",
    "localhost:8080",
  );

  private readonly URL: string =
    this.PROTOCOL + "//" + this.HOST + "/websocket";

  private connection$: WebSocketSubject<
    SocketMessage | SubscriptionMessage
  > | null = null;

  private destroyed$ = new Subject<void>();

  private listeners = new Map<string, Map<string, EventCallback>>();

  private suspended = new Map<string, EventSuspension>();

  constructor(
    private readonly translateService: TranslateService,
    private readonly utilService: UtilService,
  ) {
    this.receive(this.URL);
  }

  ngOnDestroy(): void {
    this.destroyed$.next();
    this.destroyed$.complete();
    this.close();
  }

  private open(url: string) {
    return of(url).pipe(
      switchMap((openUrl) => {
        if (!this.connection$) {
          this.connection$ = webSocket(openUrl);
          console.log("Websocket geopend: " + openUrl);
        }
        return this.connection$;
      }),
      retryWhen((errors) => errors.pipe(delay(7))),
    );
  }

  private receive(url: string) {
    this.open(url)
      .pipe(takeUntil(this.destroyed$))
      .subscribe({
        next: (message) => this.onMessage(message as SocketMessage),
        error: (error) => console.error("Websocket error:", error),
      });
  }

  private send(data: SubscriptionMessage) {
    if (!this.connection$) {
      console.error("Websocket is niet open");
      return;
    }

    this.connection$.next(data);
  }

  private close() {
    this.connection$?.complete();
    this.connection$ = null;
    console.info("Websocket gesloten");
  }

  private onMessage = (message: SocketMessage) => {
    // message is a JSON representation of ScreenEvent.java
    const event = new ScreenEvent(
      message.opcode,
      message.objectType,
      message.objectId,
      message.timestamp,
    );
    this.dispatch(event, event.key);
    this.dispatch(event, event.keyAnyOpcode);
    this.dispatch(event, event.keyAnyObjectType);
    this.dispatch(event, event.keyAnyOpcodeAndObjectType);
  };

  private dispatch(event: ScreenEvent, key: string) {
    const callbacks = this.getCallbacks(key);
    for (const listenerId in callbacks) {
      try {
        if (this.isSuspended(listenerId)) {
          return;
        }
        console.debug("listener call: " + key);
        callbacks.get(listenerId)?.(event);
      } catch (error) {
        console.warn("Websocket callback error: ", error);
      }
    }
  }

  public addListener(
    opcode: Opcode,
    objectType: ObjectType,
    objectId: string,
    callback: EventCallback,
  ) {
    const event = new ScreenEvent(
      opcode,
      objectType,
      new ScreenEventId(objectId),
    );
    const listener = this.addCallback(event, callback);
    this.send(new SubscriptionMessage(SubscriptionType.CREATE, event));
    console.debug("listener added: " + listener.key);
    return listener;
  }

  public addListenerWithSnackbar(
    opcode: Opcode,
    objectType: ObjectType,
    objectId: string,
    callback: EventCallback,
  ) {
    return this.addListener(opcode, objectType, objectId, (event) => {
      forkJoin({
        msgPart1: this.translateService.get(
          "msg.gewijzigd.objecttype." + event.objectType,
        ),
        msgPart2: this.translateService.get(
          event.objectType.indexOf("_") < 0
            ? "msg.gewijzigd.2"
            : "msg.gewijzigd.2.details",
        ),
        msgPart3: this.translateService.get(
          "msg.gewijzigd.operatie." + event.opcode,
        ),
        msgPart4: this.translateService.get("msg.gewijzigd.4"),
      }).subscribe((result) => {
        callback(event);
        this.utilService.openSnackbar(
          result.msgPart1 + result.msgPart2 + result.msgPart3 + result.msgPart4,
        );
      });
    });
  }

  public suspendListener(
    listener?: WebsocketListener,
    timeout = WebsocketService.DEFAULT_SUSPENSION_TIMEOUT,
  ) {
    if (!listener) return;

    const suspension = this.suspended.get(listener.id);
    if (suspension) {
      suspension.increment();
    } else {
      this.suspended.set(listener.id, new EventSuspension(timeout));
    }
    console.debug("listener suspended: " + listener.key);
  }

  public doubleSuspendListener(listener: WebsocketListener) {
    this.suspendListener(listener);
    this.suspendListener(listener);
  }

  public removeListener(listener?: WebsocketListener) {
    if (!listener) return;

    this.removeCallback(listener);
    this.send(new SubscriptionMessage(SubscriptionType.DELETE, listener.event));
    console.debug("listener removed: " + listener.key);
  }

  public removeListeners(listeners: WebsocketListener[]) {
    listeners.forEach((listener) => {
      this.removeListener(listener);
    });
  }

  private addCallback(event: ScreenEvent, callback: EventCallback) {
    const listener: WebsocketListener = new WebsocketListener(event, callback);
    const callbacks = this.getCallbacks(event.key);
    callbacks.set(listener.id, callback);
    return listener;
  }

  private removeCallback(listener: WebsocketListener) {
    const callbacks = this.getCallbacks(listener.event.key);
    callbacks.delete(listener.id);
    this.suspended.delete(listener.id);
  }

  private getCallbacks(key: string) {
    return this.listeners.get(key) ?? new Map<string, EventCallback>();
  }

  private isSuspended(listenerId: string) {
    const suspension = this.suspended.get(listenerId);

    if (!suspension) return false;

    const expired: boolean = suspension.isExpired();
    const done = suspension.isDone(); // Do not short circuit calling this method (here be side effects)
    if (done || expired) {
      this.suspended.delete(listenerId);
    }
    return !expired;
  }
}
