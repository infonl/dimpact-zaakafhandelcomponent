/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { TestBed } from "@angular/core/testing";
import { TranslateService } from "@ngx-translate/core";
import {Observable, of} from "rxjs";
import { UtilService } from "../../core/service/util.service";
import { ObjectType } from "../../core/websocket/model/object-type";
import { Opcode } from "../../core/websocket/model/opcode";
import { ScreenEvent } from "../../core/websocket/model/screen-event";
import { ScreenEventId } from "../../core/websocket/model/screen-event-id";
import { WebsocketListener } from "../../core/websocket/model/websocket-listener";
import { WebsocketService } from "../../core/websocket/websocket.service";
import { BatchProcessService } from "./batch-process.service";

describe(BatchProcessService.name, () => {
  let service: BatchProcessService;
  let websocketService: WebsocketService;
  let utilService: UtilService;
  let translateService: TranslateService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        WebsocketService,
        UtilService,
        {
          provide: TranslateService,
          useValue: {
            instant: jest.fn((message: string) => message),
            get: jest.fn().mockReturnValue(of("")), // Mocking the get method to return an observable
          },
        },
      ],
    });

    service = TestBed.inject(BatchProcessService);

    websocketService = TestBed.inject(WebsocketService);
    jest.spyOn(websocketService, 'removeListener').mockReturnValue()
    jest
      .spyOn(websocketService, "addListener")
      .mockReturnValue(
        new WebsocketListener(
          new ScreenEvent(Opcode.ANY, ObjectType.ANY, new ScreenEventId("1")),
          jest.fn(),
        ),
      );

    utilService = TestBed.inject(UtilService);
    jest.spyOn(utilService, "closeProgressDialog").mockReturnValue();
    jest.spyOn(utilService, 'openProgressDialog').mockReturnValue(new Observable());

    translateService = TestBed.inject(TranslateService);
    jest.spyOn(translateService, "instant").mockReturnValue("mock-translation");
  });

  describe(BatchProcessService.prototype.subscribe.name, () => {
    test("should set websocket subscriptions", () => {
      service.subscribe({
        ids: ["1", "2"],
        progressSubscription: {
          opcode: Opcode.ANY,
          objectType: ObjectType.ANY,
          onNotification: jest.fn(),
        },
        finally: jest.fn(),
      });

      expect(websocketService.addListener).toHaveBeenCalledTimes(2);
      expect(websocketService.addListener).toHaveBeenCalledWith(
        Opcode.ANY,
        ObjectType.ANY,
        "1",
        expect.any(Function),
      );
      expect(websocketService.addListener).toHaveBeenCalledWith(
        Opcode.ANY,
        ObjectType.ANY,
        "2",
        expect.any(Function),
      );
    });
  });

  describe(BatchProcessService.prototype.stop.name, () => {
    test("should close the process dialog", () => {
      service.stop();

      expect(utilService.closeProgressDialog).toHaveBeenCalledTimes(1);
    });

    test("remove all subscriptions", () => {
      service.subscribe({
        ids: ["1", "2"],
        progressSubscription: {
          opcode: Opcode.ANY,
          objectType: ObjectType.ANY,
          onNotification: jest.fn(),
        },
        finally: jest.fn(),
      });

      service.stop();

      expect(websocketService.removeListener).toHaveBeenCalledTimes(2);
    });
  });

  describe(BatchProcessService.prototype.showProgress.name, () => {
    test("should open the process dialog", () => {
      service.showProgress("message");

      expect(utilService.openProgressDialog).toHaveBeenCalledTimes(1);
    });

    test("should set a timeout when a timeout is passed", () => {
    const setTimeout = jest.spyOn(window,'setTimeout')

      service.showProgress("message", {
        onTimeout: jest.fn(),
      });

      expect(setTimeout).toHaveBeenCalledTimes(1);
    });

    test("should call the timeout callback after the timeout", () => {
      jest.useFakeTimers();
      const timeout = 100;
      const onTimeout = jest.fn();
      service.showProgress("message", {
        onTimeout: onTimeout,
        durationInMs: timeout,
      });

      jest.advanceTimersByTime(timeout + 1);
      expect(onTimeout).toHaveBeenCalledTimes(1);
    });

    test("after the timeout it should call to stop the progress", async () => {
        const stop = jest.spyOn(service, 'stop')
        jest.useFakeTimers();
        const timeout = 100;
        const onTimeout = jest.fn();
        service.showProgress("message", {
            onTimeout: onTimeout,
            durationInMs: timeout,
        });

        jest.advanceTimersByTime(timeout + 1);
        await jest.runAllTimersAsync(); // To call the `.finally()` of the promise
        expect(stop).toHaveBeenCalledTimes(1);
    })
  });
});
