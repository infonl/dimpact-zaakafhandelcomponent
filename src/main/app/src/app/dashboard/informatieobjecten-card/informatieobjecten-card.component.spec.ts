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
import { InformatieobjectenCardComponent } from "./informatieobjecten-card.component";

const INFORMATIEOBJECTEN_URL =
  "/rest/signaleringen/informatieobjecten/ZAAK_DOCUMENT_TOEGEVOEGD";

const informatieobject = (
  fields: Partial<GeneratedType<"RestEnkelvoudigInformatieobject">> = {},
) =>
  fromPartial<GeneratedType<"RestEnkelvoudigInformatieobject">>({
    titel: "Test document",
    auteur: "Test auteur",
    ...fields,
  });

const signaleringChangedEvent = (signaleringType: GeneratedType<"Type">) =>
  fromPartial<ScreenEvent>({ objectId: { detail: signaleringType } });

describe(InformatieobjectenCardComponent.name, () => {
  let fixture: ComponentFixture<InformatieobjectenCardComponent>;
  let httpTestingController: HttpTestingController;
  let websocketService: WebsocketService;
  let signaleringenChanged: (event: ScreenEvent) => void;

  const user = userEvent.setup();

  async function setup(
    card = new DashboardCard(
      DashboardCardId.MIJN_DOCUMENTEN_NIEUW,
      DashboardCardType.ZAKEN,
      "ZAAK_DOCUMENT_TOEGEVOEGD",
    ),
  ) {
    websocketService = fromPartial<WebsocketService>({
      addListener: jest.fn((_opcode, _objectType, _objectId, callback) => {
        signaleringenChanged = callback as (event: ScreenEvent) => void;
        return fromPartial<WebsocketListener>({});
      }),
    });

    ({ fixture } = await render(InformatieobjectenCardComponent, {
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

  async function respondWithInformatieobjecten(
    informatieobjecten: GeneratedType<"RestEnkelvoudigInformatieobject">[],
  ) {
    httpTestingController
      .expectOne(INFORMATIEOBJECTEN_URL)
      .flush(informatieobjecten);
    await sleep();
    await sleep();
    fixture.detectChanges();
  }

  function informatieobjectTitels() {
    const [, ...rows] = screen.getAllByRole("row");
    return rows.map(
      (row) => within(row).getAllByRole("cell")[0].textContent?.trim() ?? "",
    );
  }

  it("shows the informatieobjecten of the card's signaleringType", async () => {
    await setup();

    await respondWithInformatieobjecten([
      informatieobject({ titel: "Document A" }),
      informatieobject({ titel: "Document B" }),
    ]);

    expect(informatieobjectTitels()).toEqual(["Document A", "Document B"]);
  });

  it("shows a loading indicator until the informatieobjecten have arrived", async () => {
    await setup();

    expect(
      screen.getByRole("progressbar", { name: "msg.loading" }),
    ).toBeInTheDocument();

    await respondWithInformatieobjecten([]);

    expect(screen.queryByRole("progressbar")).not.toBeInTheDocument();
    expect(screen.getByText("msg.geen.gegevens.gevonden")).toBeInTheDocument();
  });

  it("reads no informatieobjecten for a card without a signaleringType", async () => {
    await setup(
      new DashboardCard(
        DashboardCardId.MIJN_DOCUMENTEN_NIEUW,
        DashboardCardType.ZAKEN,
      ),
    );

    httpTestingController.expectNone((request) =>
      request.url.startsWith("/rest/signaleringen/informatieobjecten/"),
    );
    expect(screen.getByText("msg.geen.gegevens.gevonden")).toBeInTheDocument();
  });

  it("listens for changes to the signaleringen of the logged-in user", async () => {
    await setup();
    await respondWithInformatieobjecten([]);

    expect(websocketService.addListener).toHaveBeenCalledWith(
      Opcode.UPDATED,
      ObjectType.SIGNALERINGEN,
      "user",
      expect.any(Function),
    );
  });

  it("reads the informatieobjecten again when a signalering of the card's type changes", async () => {
    await setup();
    await respondWithInformatieobjecten([
      informatieobject({ titel: "Document A" }),
    ]);

    signaleringenChanged(signaleringChangedEvent("ZAAK_DOCUMENT_TOEGEVOEGD"));
    await respondWithInformatieobjecten([
      informatieobject({ titel: "Document B" }),
    ]);

    expect(informatieobjectTitels()).toEqual(["Document B"]);
  });

  it("does not read the informatieobjecten again when a signalering of another type changes", async () => {
    await setup();
    await respondWithInformatieobjecten([]);

    signaleringenChanged(signaleringChangedEvent("ZAAK_OP_NAAM"));
    await sleep();

    httpTestingController.expectNone(INFORMATIEOBJECTEN_URL);
  });

  it("sorts the informatieobjecten ascending and then descending by titel", async () => {
    await setup();
    await respondWithInformatieobjecten([
      informatieobject({ titel: "Charlie" }),
      informatieobject({ titel: "Alpha" }),
      informatieobject({ titel: "Bravo" }),
    ]);

    await user.click(
      screen.getByRole("columnheader", { name: "documenttitel" }),
    );
    fixture.detectChanges();

    expect(informatieobjectTitels()).toEqual(["Alpha", "Bravo", "Charlie"]);

    await user.click(
      screen.getByRole("columnheader", { name: "documenttitel" }),
    );
    fixture.detectChanges();

    expect(informatieobjectTitels()).toEqual(["Charlie", "Bravo", "Alpha"]);
  });
});
