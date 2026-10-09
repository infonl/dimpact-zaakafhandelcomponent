/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpTestingController } from "@angular/common/http/testing";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { render, screen, within } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { fromPartial } from "src/test-helpers";
import { sleep } from "../../../../setupJest";
import { ObjectType } from "../../core/websocket/model/object-type";
import { Opcode } from "../../core/websocket/model/opcode";
import { ScreenEvent } from "../../core/websocket/model/screen-event";
import { WebsocketListener } from "../../core/websocket/model/websocket-listener";
import { WebsocketService } from "../../core/websocket/websocket.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { DashboardCard } from "../model/dashboard-card";
import { DashboardCardId } from "../model/dashboard-card-id";
import { DashboardCardType } from "../model/dashboard-card-type";
import { TakenCardComponent } from "./taken-card.component";

const TAKEN_URL = "/rest/signaleringen/taken/TAAK_OP_NAAM";

const taak = (
  fields: Partial<GeneratedType<"RestSignaleringTaskSummary">> = {},
) =>
  fromPartial<GeneratedType<"RestSignaleringTaskSummary">>({
    naam: "Test taak",
    zaakIdentificatie: "ZAAK-001",
    zaaktypeOmschrijving: "Testtype",
    ...fields,
  });

const signaleringChangedEvent = (signaleringType: GeneratedType<"Type">) =>
  fromPartial<ScreenEvent>({ objectId: { detail: signaleringType } });

describe(TakenCardComponent.name, () => {
  let fixture: ComponentFixture<TakenCardComponent>;
  let httpTestingController: HttpTestingController;
  let websocketService: WebsocketService;
  let signaleringenChanged: (event: ScreenEvent) => void;

  const user = userEvent.setup();

  async function setup(
    card = new DashboardCard(
      DashboardCardId.MIJN_TAKEN,
      DashboardCardType.TAKEN,
      "TAAK_OP_NAAM",
    ),
  ) {
    websocketService = fromPartial<WebsocketService>({
      addListener: jest.fn((_opcode, _objectType, _objectId, callback) => {
        signaleringenChanged = callback as (event: ScreenEvent) => void;
        return fromPartial<WebsocketListener>({});
      }),
    });

    ({ fixture } = await render(TakenCardComponent, {
      inputs: { data: card },
      imports: [NoopAnimationsModule, TranslateModule.forRoot()],
      providers: [{ provide: WebsocketService, useValue: websocketService }],
    }));

    httpTestingController = TestBed.inject(HttpTestingController);
    if (card.signaleringType == null) return;

    httpTestingController
      .expectOne("/rest/identity/loggedInUser")
      .flush(fromPartial<GeneratedType<"RestLoggedInUser">>({ id: "user" }));
    await sleep();
  }

  async function respondWithTaken(
    taken: GeneratedType<"RestSignaleringTaskSummary">[],
  ) {
    httpTestingController.expectOne(TAKEN_URL).flush(taken);
    await sleep();
    await sleep();
    fixture.detectChanges();
  }

  function takenNamen() {
    const [, ...rows] = screen.getAllByRole("row");
    return rows.map(
      (row) => within(row).getAllByRole("cell")[0].textContent?.trim() ?? "",
    );
  }

  it("shows the taken of the card's signaleringType", async () => {
    await setup();

    await respondWithTaken([
      taak({ naam: "Taak A" }),
      taak({ naam: "Taak B" }),
    ]);

    expect(takenNamen()).toEqual(["Taak A", "Taak B"]);
  });

  it("shows a loading indicator until the taken have arrived", async () => {
    await setup();

    expect(
      screen.getByRole("progressbar", { name: "msg.loading" }),
    ).toBeInTheDocument();

    await respondWithTaken([]);

    expect(screen.queryByRole("progressbar")).not.toBeInTheDocument();
    expect(screen.getByText("msg.geen.gegevens.gevonden")).toBeInTheDocument();
  });

  it("reads no taken for a card without a signaleringType", async () => {
    await setup(
      new DashboardCard(DashboardCardId.MIJN_TAKEN, DashboardCardType.TAKEN),
    );

    httpTestingController.expectNone((request) =>
      request.url.startsWith("/rest/signaleringen/taken/"),
    );
    expect(screen.getByText("msg.geen.gegevens.gevonden")).toBeInTheDocument();
  });

  it("listens for changes to the signaleringen of the logged-in user", async () => {
    await setup();
    await respondWithTaken([]);

    expect(websocketService.addListener).toHaveBeenCalledWith(
      Opcode.UPDATED,
      ObjectType.SIGNALERINGEN,
      "user",
      expect.any(Function),
    );
  });

  it("reads the taken again when a signalering of the card's type changes", async () => {
    await setup();
    await respondWithTaken([taak({ naam: "Taak A" })]);

    signaleringenChanged(signaleringChangedEvent("TAAK_OP_NAAM"));
    await respondWithTaken([taak({ naam: "Taak B" })]);

    expect(takenNamen()).toEqual(["Taak B"]);
  });

  it("does not read the taken again when a signalering of another type changes", async () => {
    await setup();
    await respondWithTaken([]);

    signaleringenChanged(signaleringChangedEvent("ZAAK_OP_NAAM"));
    await sleep();

    httpTestingController.expectNone(TAKEN_URL);
  });

  it("sorts the taken ascending and then descending by naam", async () => {
    await setup();
    await respondWithTaken([
      taak({ naam: "Charlie" }),
      taak({ naam: "Alpha" }),
      taak({ naam: "Bravo" }),
    ]);

    await user.click(screen.getByRole("columnheader", { name: "naam" }));
    fixture.detectChanges();

    expect(takenNamen()).toEqual(["Alpha", "Bravo", "Charlie"]);

    await user.click(screen.getByRole("columnheader", { name: "naam" }));
    fixture.detectChanges();

    expect(takenNamen()).toEqual(["Charlie", "Bravo", "Alpha"]);
  });
});
