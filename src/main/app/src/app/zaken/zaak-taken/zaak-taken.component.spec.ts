/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  provideHttpClient,
  withInterceptorsFromDi,
} from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { notifyManager } from "@tanstack/query-core";
import { render, screen, within } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../setupJest";
import { UtilService } from "../../core/service/util.service";
import { ObjectType } from "../../core/websocket/model/object-type";
import { Opcode } from "../../core/websocket/model/opcode";
import { ScreenEvent } from "../../core/websocket/model/screen-event";
import { WebsocketListener } from "../../core/websocket/model/websocket-listener";
import { WebsocketService } from "../../core/websocket/websocket.service";
import { SessionStorageUtil } from "../../shared/storage/session-storage.util";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZaakTakenComponent } from "./zaak-taken.component";

const zaak = fromPartial<GeneratedType<"RestZaak">>({ uuid: "fakeZaakUuid" });

const loggedInUser = fromPartial<GeneratedType<"RestLoggedInUser">>({
  id: "fakeLoggedInUserId",
  naam: "fakeLoggedInUserNaam",
  groupIds: ["fakeGroepId"],
});

const taak = (fields: Partial<GeneratedType<"RestTask">> = {}) =>
  fromPartial<GeneratedType<"RestTask">>({
    id: "fakeTaakId",
    naam: "fakeTaakNaam",
    status: "TOEGEKEND",
    creatiedatumTijd: "2026-01-01T00:00:00Z",
    fataledatum: "2026-06-01",
    groep: { id: "fakeGroepId", naam: "fakeGroepNaam" },
    behandelaar: { id: "fakeBehandelaarId", naam: "fakeBehandelaarNaam" },
    zaakUuid: "fakeZaakUuid",
    rechten: {
      lezen: true,
      toekennen: true,
      wijzigen: true,
      toevoegenDocument: true,
    },
    taakdata: {},
    tabellen: {},
    taakdocumenten: [],
    taakinformatie: {},
    ...fields,
  });

describe(ZaakTakenComponent.name, () => {
  let fixture: ComponentFixture<ZaakTakenComponent>;
  let httpTestingController: HttpTestingController;
  let websocketService: WebsocketService;
  let openSnackbar: jest.SpyInstance;

  const user = userEvent.setup();

  beforeEach(() => {
    SessionStorageUtil.clearSessionStorage();
    notifyManager.setScheduler((fn) => fn());
  });

  afterEach(() => {
    notifyManager.setScheduler(queueMicrotask);
  });

  async function setup(
    taken: GeneratedType<"RestTask">[] = [taak()],
    zaakToShow: GeneratedType<"RestZaak"> = zaak,
  ) {
    websocketService = fromPartial<WebsocketService>({
      addListener: jest.fn(() => fromPartial<WebsocketListener>({})),
      removeListener: jest.fn(),
      suspendListener: jest.fn(),
    });

    const rendered = await render(ZaakTakenComponent, {
      inputs: { zaak: zaakToShow },
      imports: [NoopAnimationsModule, TranslateModule.forRoot()],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideRouter([]),
        provideQueryClient(testQueryClient),
        { provide: WebsocketService, useValue: websocketService },
      ],
    });

    fixture = rendered.fixture;
    httpTestingController = TestBed.inject(HttpTestingController);
    openSnackbar = jest
      .spyOn(TestBed.inject(UtilService), "openSnackbar")
      .mockReturnValue(undefined as never);

    httpTestingController
      .expectOne("/rest/identity/loggedInUser")
      .flush(loggedInUser);
    await respondWithTaken(taken, zaakToShow.uuid);

    return rendered;
  }

  async function respondWithTaken(
    taken: GeneratedType<"RestTask">[],
    uuid = zaak.uuid,
  ) {
    httpTestingController
      .match(`/rest/taken/zaak/${uuid}`)
      .forEach((request) => request.flush(taken));
    await sleep();
    // the table creates the row views in one pass and binds their cells in the next
    fixture.detectChanges();
    fixture.detectChanges();
  }

  function taakRows() {
    return screen
      .getAllByRole("row")
      .filter((row) => within(row).queryAllByRole("cell").length > 1);
  }

  function taaknamenInRowOrder() {
    return taakRows().map((row) =>
      within(row).getAllByRole("cell")[0].textContent?.trim(),
    );
  }

  function rowOf(taaknaam: string) {
    return screen.getByRole("row", { name: new RegExp(taaknaam) });
  }

  function assignToMeButtonIn(taaknaam: string) {
    return within(rowOf(taaknaam)).queryByRole("button", {
      name: "actie.mij.toekennen",
    });
  }

  function toonAfgerondeTakenToggle() {
    return screen.getByRole("switch", { name: "toonAfgerondeTaken" });
  }

  async function showAfgerondeTaken() {
    await user.click(toonAfgerondeTakenToggle());
    // the table creates the row views in one pass and binds their cells in the next
    fixture.detectChanges();
    fixture.detectChanges();
  }

  it("shows the taken of the zaak it is given", async () => {
    await setup(
      [taak({ naam: "andereTaakNaam" })],
      fromPartial<GeneratedType<"RestZaak">>({ uuid: "andereZaakUuid" }),
    );

    expect(rowOf("andereTaakNaam")).toBeVisible();
  });

  it("shows the no-data message when the zaak has no taken", async () => {
    await setup([]);

    expect(screen.getByText("msg.geen.gegevens.gevonden")).toBeVisible();
  });

  it("listens for taak updates on the zaak", async () => {
    await setup();

    expect(websocketService.addListener).toHaveBeenCalledWith(
      Opcode.UPDATED,
      ObjectType.ZAAK_TAKEN,
      zaak.uuid,
      expect.any(Function),
    );
  });

  it("stops listening when it is taken off the screen", async () => {
    await setup();

    fixture.destroy();

    expect(websocketService.removeListener).toHaveBeenCalled();
  });

  it("fetches the taken again when the websocket reports an update", async () => {
    await setup();
    const [[, , , onZaakTakenUpdated]] = jest.mocked(
      websocketService.addListener,
    ).mock.calls;

    onZaakTakenUpdated(fromPartial<ScreenEvent>({}));
    await sleep();
    await respondWithTaken([taak({ naam: "bijgewerkteTaakNaam" })]);

    expect(rowOf("bijgewerkteTaakNaam")).toBeVisible();
  });

  describe("sorting", () => {
    const takenInDifferentOrders = [
      taak({
        id: "eersteTaakId",
        naam: "aTaakNaam",
        groep: { id: "fakeGroepId", naam: "zGroepNaam" },
        behandelaar: { id: "fakeBehandelaarId", naam: "zBehandelaarNaam" },
      }),
      taak({
        id: "tweedeTaakId",
        naam: "zTaakNaam",
        groep: { id: "fakeGroepId", naam: "aGroepNaam" },
        behandelaar: { id: "fakeBehandelaarId", naam: "aBehandelaarNaam" },
      }),
    ];

    it("sorts the taken on their groep", async () => {
      await setup(takenInDifferentOrders);

      await user.click(screen.getByRole("columnheader", { name: "groep" }));
      fixture.detectChanges();

      expect(taaknamenInRowOrder()).toEqual(["zTaakNaam", "aTaakNaam"]);
    });

    it("sorts the taken on their behandelaar", async () => {
      await setup(takenInDifferentOrders);

      await user.click(
        screen.getByRole("columnheader", { name: "behandelaar" }),
      );
      fixture.detectChanges();

      expect(taaknamenInRowOrder()).toEqual(["zTaakNaam", "aTaakNaam"]);
    });

    it("sorts the taken on their naam", async () => {
      await setup(takenInDifferentOrders);

      await user.click(screen.getByRole("columnheader", { name: "naam" }));
      fixture.detectChanges();
      expect(taaknamenInRowOrder()).toEqual(["aTaakNaam", "zTaakNaam"]);

      await user.click(screen.getByRole("columnheader", { name: "naam" }));
      fixture.detectChanges();
      expect(taaknamenInRowOrder()).toEqual(["zTaakNaam", "aTaakNaam"]);
    });
  });

  describe("expanding taken", () => {
    function expandAllControl() {
      return screen.getByLabelText("actie.alles.uitklappen");
    }

    function collapseAllControl() {
      return screen.getByLabelText("actie.alles.inklappen");
    }

    function detailOf(taaknaam: string) {
      const rows = screen.getAllByRole("row");
      const detailRow = rows[rows.indexOf(rowOf(taaknaam)) + 1];
      return within(detailRow).getByRole("cell").firstElementChild;
    }

    it("expands and collapses every taak at once", async () => {
      await setup([taak({ id: "eersteTaakId" }), taak({ id: "tweedeTaakId" })]);

      await user.click(expandAllControl());
      fixture.detectChanges();
      expect(collapseAllControl()).toBeVisible();

      await user.click(collapseAllControl());
      fixture.detectChanges();
      expect(expandAllControl()).toBeVisible();
    });

    it("expands and collapses a single taak", async () => {
      await setup();

      await user.click(rowOf("fakeTaakNaam"));
      fixture.detectChanges();
      expect(detailOf("fakeTaakNaam")).not.toHaveStyle({ height: "0px" });

      await user.click(rowOf("fakeTaakNaam"));
      fixture.detectChanges();
      expect(detailOf("fakeTaakNaam")).toHaveStyle({ height: "0px" });
    });

    it("counts the taak that was expanded as the only one left", async () => {
      await setup();

      await user.click(rowOf("fakeTaakNaam"));
      fixture.detectChanges();

      expect(collapseAllControl()).toBeVisible();
    });

    it("counts the afgeronde taken as well while those are shown", async () => {
      await setup([
        taak({ id: "eersteTaakId" }),
        taak({ id: "afgerondeTaakId", status: "AFGEROND" }),
      ]);

      await showAfgerondeTaken();
      await user.click(expandAllControl());
      fixture.detectChanges();

      expect(collapseAllControl()).toBeVisible();
    });
  });

  describe("showing afgeronde taken", () => {
    const takenWithAfgeronde = [
      taak({ id: "lopendeTaakId", naam: "lopendeTaakNaam" }),
      taak({
        id: "afgerondeTaakId",
        naam: "afgerondeTaakNaam",
        status: "AFGEROND",
      }),
    ];

    it("hides the afgeronde taken", async () => {
      await setup(takenWithAfgeronde);

      expect(rowOf("lopendeTaakNaam")).toBeVisible();
      expect(
        screen.queryByRole("row", { name: /afgerondeTaakNaam/ }),
      ).toBeNull();
    });

    it("shows the afgeronde taken once the toggle is switched on", async () => {
      await setup(takenWithAfgeronde);

      await showAfgerondeTaken();

      expect(rowOf("afgerondeTaakNaam")).toBeVisible();
    });

    it("remembers that the afgeronde taken were shown", async () => {
      SessionStorageUtil.setItem("toonAfgerondeTaken", true);

      await setup(takenWithAfgeronde);

      expect(toonAfgerondeTakenToggle()).toBeChecked();
    });
  });

  describe("the status chip", () => {
    function chipOf(status: string) {
      return screen.getByText(`taak.status.${status}`).closest("mat-chip");
    }

    it("marks an afgeronde taak as a success", async () => {
      await setup([taak({ status: "AFGEROND" })]);

      await showAfgerondeTaken();

      expect(chipOf("AFGEROND")).toHaveClass("mat-success");
    });

    it("highlights a toegekende taak", async () => {
      await setup([taak({ status: "TOEGEKEND" })]);

      expect(chipOf("TOEGEKEND")).toHaveClass("mat-primary");
    });

    it("leaves any other status with the default styling", async () => {
      await setup([taak({ status: "NIET_TOEGEKEND" })]);

      expect(chipOf("NIET_TOEGEKEND")).toHaveClass("mat-primary");
    });
  });

  describe("assigning a taak to yourself", () => {
    it("does not offer an afgeronde taak", async () => {
      await setup([taak({ status: "AFGEROND" })]);

      await showAfgerondeTaken();

      expect(assignToMeButtonIn("fakeTaakNaam")).toBeNull();
    });

    it("does not offer a taak you may not assign", async () => {
      await setup([
        taak({
          rechten: fromPartial<GeneratedType<"RestTaakRechten">>({
            toekennen: false,
          }),
        }),
      ]);

      expect(assignToMeButtonIn("fakeTaakNaam")).toBeNull();
    });

    it("does not offer a taak without a groep", async () => {
      await setup([taak({ groep: undefined })]);

      expect(assignToMeButtonIn("fakeTaakNaam")).toBeNull();
    });

    it("does not offer a taak you are already the behandelaar of", async () => {
      await setup([
        taak({
          behandelaar: { id: loggedInUser.id, naam: loggedInUser.naam },
        }),
      ]);

      expect(assignToMeButtonIn("fakeTaakNaam")).toBeNull();
    });

    it("does not offer a taak of a groep you are not a member of", async () => {
      await setup([
        taak({ groep: { id: "andereGroepId", naam: "andereGroepNaam" } }),
      ]);

      expect(assignToMeButtonIn("fakeTaakNaam")).toBeNull();
    });

    it("offers a taak of your groep that somebody else is handling", async () => {
      await setup();

      expect(assignToMeButtonIn("fakeTaakNaam")).toBeVisible();
    });

    it("assigns the taak of the row it was clicked on", async () => {
      await setup();

      await user.click(assignToMeButtonIn("fakeTaakNaam")!);
      await sleep();

      const request = httpTestingController.expectOne(
        "/rest/taken/toekennen/mij",
      );
      expect(request.request.body).toEqual({
        taakId: "fakeTaakId",
        zaakUuid: "fakeZaakUuid",
        groepId: "fakeGroepId",
      });

      request.flush(taak());
      await sleep();
    });

    it("shows the assigned behandelaar on the row it was clicked on", async () => {
      await setup();

      await user.click(assignToMeButtonIn("fakeTaakNaam")!);
      await sleep();
      httpTestingController.expectOne("/rest/taken/toekennen/mij").flush(
        taak({
          behandelaar: { id: loggedInUser.id, naam: loggedInUser.naam },
        }),
      );
      await sleep();
      fixture.detectChanges();

      expect(screen.getByText("fakeLoggedInUserNaam")).toBeVisible();
      expect(openSnackbar).toHaveBeenCalledWith("msg.taak.toegekend", {
        behandelaar: "fakeLoggedInUserNaam",
      });
      expect(assignToMeButtonIn("fakeTaakNaam")).toBeNull();
    });

    it("suspends the taak updates while the assignment is in flight", async () => {
      await setup();

      await user.click(assignToMeButtonIn("fakeTaakNaam")!);
      await sleep();

      expect(websocketService.suspendListener).toHaveBeenCalled();

      httpTestingController
        .expectOne("/rest/taken/toekennen/mij")
        .flush(taak());
      await sleep();
    });

    it("refuses a second click while the assignment is in flight", async () => {
      await setup();

      await user.click(assignToMeButtonIn("fakeTaakNaam")!);
      await sleep();
      fixture.detectChanges();

      expect(assignToMeButtonIn("fakeTaakNaam")).toBeDisabled();

      const requests = httpTestingController.match("/rest/taken/toekennen/mij");
      expect(requests).toHaveLength(1);

      requests[0].flush(taak());
      await sleep();
    });

    it("offers the taak again once the assignment settled on somebody else", async () => {
      await setup();

      await user.click(assignToMeButtonIn("fakeTaakNaam")!);
      await sleep();
      httpTestingController
        .expectOne("/rest/taken/toekennen/mij")
        .flush(
          taak({ behandelaar: { id: "andereUserId", naam: "andereNaam" } }),
        );
      await sleep();
      fixture.detectChanges();

      expect(assignToMeButtonIn("fakeTaakNaam")).toBeEnabled();
    });
  });
});
