/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, inject } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { createQueryOptions, fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../setupJest";
import { ObjectType } from "../../core/websocket/model/object-type";
import { Opcode } from "../../core/websocket/model/opcode";
import { ScreenEvent } from "../../core/websocket/model/screen-event";
import { ScreenEventId } from "../../core/websocket/model/screen-event-id";
import { WebsocketService } from "../../core/websocket/websocket.service";
import { IdentityService } from "../../identity/identity.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { DashboardCard } from "../model/dashboard-card";
import { DashboardCardId } from "../model/dashboard-card-id";
import { DashboardCardType } from "../model/dashboard-card-type";
import { DashboardCardComponent } from "./dashboard-card.component";

const onLoad = jest.fn();

@Component({ selector: "zac-test-dashboard-card", template: "" })
class TestDashboardCardComponent extends DashboardCardComponent {
  readonly columns = [];

  constructor() {
    super(inject(IdentityService), inject(WebsocketService));
  }

  protected onLoad() {
    onLoad();
  }
}

const signaleringUpdate = (signaleringType: GeneratedType<"Type">) => {
  const objectId = new ScreenEventId("fakeUserId");
  objectId.detail = signaleringType;
  return new ScreenEvent(Opcode.UPDATED, ObjectType.SIGNALERINGEN, objectId);
};

describe(DashboardCardComponent.name, () => {
  let fixture: ComponentFixture<TestDashboardCardComponent>;
  const addListener = jest.fn<
    ReturnType<WebsocketService["addListener"]>,
    Parameters<WebsocketService["addListener"]>
  >();

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestDashboardCardComponent],
      providers: [
        provideQueryClient(testQueryClient),
        {
          provide: IdentityService,
          useValue: fromPartial<IdentityService>({
            readLoggedInUser: () =>
              createQueryOptions(
                fromPartial<GeneratedType<"RestUser">>({ id: "fakeUserId" }),
              ),
          }),
        },
        {
          provide: WebsocketService,
          useValue: fromPartial<WebsocketService>({ addListener }),
        },
      ],
    }).compileComponents();
  });

  async function createCard(signaleringType?: GeneratedType<"Type">) {
    fixture = TestBed.createComponent(TestDashboardCardComponent);
    fixture.componentRef.setInput(
      "data",
      new DashboardCard(
        DashboardCardId.MIJN_TAKEN_NIEUW,
        DashboardCardType.TAKEN,
        signaleringType,
      ),
    );
    fixture.detectChanges();
    await sleep();
  }

  function sendSignaleringUpdate(signaleringType: GeneratedType<"Type">) {
    const [, , , callback] = addListener.mock.lastCall!;
    callback(signaleringUpdate(signaleringType));
  }

  it("loads its content once when it is shown", async () => {
    await createCard("TAAK_OP_NAAM");

    expect(onLoad).toHaveBeenCalledTimes(1);
  });

  it("listens for signalering updates of the logged-in user when its card has a signaleringtype", async () => {
    await createCard("TAAK_OP_NAAM");

    expect(addListener).toHaveBeenCalledWith(
      Opcode.UPDATED,
      ObjectType.SIGNALERINGEN,
      "fakeUserId",
      expect.any(Function),
    );
  });

  it("does not listen for signalering updates when its card has no signaleringtype", async () => {
    await createCard(undefined);

    expect(addListener).not.toHaveBeenCalled();
  });

  it("reloads its content when a signalering of the type of its card is updated", async () => {
    await createCard("TAAK_OP_NAAM");

    sendSignaleringUpdate("TAAK_OP_NAAM");

    expect(onLoad).toHaveBeenCalledTimes(2);
  });

  it("does not reload its content when a signalering of another type is updated", async () => {
    await createCard("TAAK_OP_NAAM");

    sendSignaleringUpdate("ZAAK_OP_NAAM");

    expect(onLoad).toHaveBeenCalledTimes(1);
  });

  it("stops reloading its content once it is destroyed", async () => {
    await createCard("TAAK_OP_NAAM");
    fixture.destroy();

    sendSignaleringUpdate("TAAK_OP_NAAM");

    expect(onLoad).toHaveBeenCalledTimes(1);
  });
});
