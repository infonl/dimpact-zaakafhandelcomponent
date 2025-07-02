/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable, OnDestroy } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { Observable, Subject, forkJoin, of, throwError } from "rxjs";
import {
  catchError,
  delay,
  retryWhen,
  switchMap,
  takeUntil,
  timeout,
} from "rxjs/operators";
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

  private listeners: EventCallback[][] = [];

  private suspended: EventSuspension[] = [];

  constructor(
    private translate: TranslateService,
    private utilService: UtilService,
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
        error: this.onError,
      });
  }

  private send(data: SubscriptionMessage) {
    if (this.connection$) {
      this.connection$.next(data);
    } else {
      console.error("Websocket is niet open");
    }
  }

  private close() {
    if (this.connection$) {
      this.connection$.complete();
      console.warn("Websocket gesloten");
      this.connection$ = null;
    }
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
    const callbacks: EventCallback[] = this.getCallbacks(key);
    for (const listenerId in callbacks) {
      try {
        if (!this.isSuspended(listenerId)) {
          console.debug("listener call: " + key);
          callbacks[listenerId](event);
        }
      } catch (error) {
        console.warn("Websocket callback error: ");
        console.error(error);
      }
    }
  }

  private onError = (error: unknown) => {
    console.error("Websocket error:");
    console.error(error);
  };

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
        msgPart1: this.translate.get(
          "msg.gewijzigd.objecttype." + event.objectType,
        ),
        msgPart2: this.translate.get(
          event.objectType.indexOf("_") < 0
            ? "msg.gewijzigd.2"
            : "msg.gewijzigd.2.details",
        ),
        msgPart3: this.translate.get("msg.gewijzigd.operatie." + event.opcode),
        msgPart4: this.translate.get("msg.gewijzigd.4"),
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
    timeout: number = WebsocketService.DEFAULT_SUSPENSION_TIMEOUT,
  ): void {
    if (!listener) return;

    const suspension: EventSuspension = this.suspended[listener.id];
    if (suspension) {
      suspension.increment();
    } else {
      this.suspended[listener.id] = new EventSuspension(timeout);
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

  public removeListeners(listeners: WebsocketListener[]): void {
    listeners.forEach((listener) => {
      this.removeListener(listener);
    });
  }

  public longRunningOperation<T = void>(
    opcode: Opcode,
    objectType: ObjectType,
    objectId: string,
    operation: () => Observable<void>,
  ): Observable<T> {
    /**
     * In the unlikely scenario that the back end never responds with an event,
     * we want to eventually clean up the websocket connection to prevent memory leaks
     * The back end process can take quite a while, so we chose a timeout of one hour.
     */
    const ARBITRARY_ONE_HOUR_TIMEOUT_TO_PREVENT_MEMORY_LEAKS_IN_EDGE_CASES =
      60 * 60 * 1000;

    const subject = new Subject<T>();

    const subscription = this.addListener(opcode, objectType, objectId, (e) => {
      this.removeListener(subscription);
      const response = e.objectId.detail && JSON.parse(e.objectId.detail);
      subject.next(response);
      subject.complete();
    });

    return operation().pipe(
      switchMap(() => subject),
      timeout(ARBITRARY_ONE_HOUR_TIMEOUT_TO_PREVENT_MEMORY_LEAKS_IN_EDGE_CASES),
      catchError((e) => {
        this.removeListener(subscription);
        return throwError(() => e);
      }),
    );
  }

  private addCallback(event: ScreenEvent, callback: EventCallback) {
    const listener: WebsocketListener = new WebsocketListener(event, callback);
    const callbacks: EventCallback[] = this.getCallbacks(event.key);
    callbacks[listener.id] = callback;
    return listener;
  }

  private removeCallback(listener: WebsocketListener): void {
    const callbacks: EventCallback[] = this.getCallbacks(listener.event.key);
    delete callbacks[listener.id];
    delete this.suspended[listener.id];
  }

  private getCallbacks(key: string): EventCallback[] {
    if (!this.listeners[key]) {
      this.listeners[key] = [];
    }
    return this.listeners[key];
  }

  private isSuspended(listenerId: string): boolean {
    const suspension: EventSuspension = this.suspended[listenerId];
    if (suspension) {
      const expired: boolean = suspension.isExpired();
      const done = suspension.isDone(); // Do not short circuit calling this method (here be side effects)
      if (done || expired) {
        delete this.suspended[listenerId];
      }
      return !expired;
    }
    return false;
  }
}
