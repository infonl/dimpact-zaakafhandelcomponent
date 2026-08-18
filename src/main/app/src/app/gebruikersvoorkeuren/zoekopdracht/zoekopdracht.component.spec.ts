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
import { EventEmitter } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatDialog, MatDialogRef } from "@angular/material/dialog";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { render, screen, within } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { of } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../setupJest";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZoekopdrachtSaveDialogComponent } from "../zoekopdracht-save-dialog/zoekopdracht-save-dialog.component";
import { ZoekFilters } from "./zoekfilters.model";
import { ZoekopdrachtComponent } from "./zoekopdracht.component";

const WERKLIJST = "MIJN_ZAKEN";
const ZOEKOPDRACHTEN_URL = `/rest/gebruikersvoorkeuren/zoekopdracht/${WERKLIJST}`;
const ACTIEF_URL = "/rest/gebruikersvoorkeuren/zoekopdracht/actief";
const REMOVE_ACTIEF_URL = `/rest/gebruikersvoorkeuren/zoekopdracht/${WERKLIJST}/actief`;

const makeZoekopdracht = (
  fields: Partial<GeneratedType<"RESTZoekopdracht">> = {},
) =>
  fromPartial<GeneratedType<"RESTZoekopdracht">>({
    id: 1,
    naam: "Zoekopdracht A",
    actief: false,
    lijstID: WERKLIJST,
    ...fields,
  });

const makeZoekFilters = (fields: Partial<ZoekFilters> = {}): ZoekFilters => ({
  filtersType: "ZoekParameters",
  zoeken: {},
  filters: {},
  datums: {},
  ...fields,
});

describe(ZoekopdrachtComponent.name, () => {
  let fixture: ComponentFixture<ZoekopdrachtComponent>;
  let httpTestingController: HttpTestingController;
  let filtersChanged: EventEmitter<void>;
  let dialogOpen: jest.SpyInstance;
  let zoekopdrachtEmitted: jest.Mock;

  const user = userEvent.setup();

  async function setup(zoekFilters: ZoekFilters = makeZoekFilters()) {
    filtersChanged = new EventEmitter<void>();
    zoekopdrachtEmitted = jest.fn();

    const rendered = await render(ZoekopdrachtComponent, {
      inputs: {
        werklijst: WERKLIJST,
        zoekFilters,
        filtersChanged,
      },
      on: { zoekopdracht: zoekopdrachtEmitted },
      imports: [NoopAnimationsModule, TranslateModule.forRoot()],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideQueryClient(testQueryClient),
      ],
    });

    fixture = rendered.fixture;
    httpTestingController = TestBed.inject(HttpTestingController);
    dialogOpen = jest.spyOn(TestBed.inject(MatDialog), "open").mockReturnValue(
      fromPartial<MatDialogRef<ZoekopdrachtSaveDialogComponent>>({
        afterClosed: () => of(null),
      }),
    );
  }

  async function loadZoekopdrachten(
    zoekopdrachten: GeneratedType<"RESTZoekopdracht">[] = [],
  ) {
    httpTestingController.expectOne(ZOEKOPDRACHTEN_URL).flush(zoekopdrachten);
    await sleep();
    fixture.detectChanges();
  }

  async function flushRemoveActief() {
    await sleep();
    httpTestingController.expectOne(REMOVE_ACTIEF_URL).flush(null);
    await sleep();
    fixture.detectChanges();
  }

  function clearButton() {
    return screen.getByRole("button", { name: "actie.zoekopdracht.wissen" });
  }

  function selectButton() {
    return screen.getByRole("button", { name: "actie.zoekopdracht.selecteer" });
  }

  function saveButton() {
    return screen.getByRole("button", { name: "actie.zoekopdracht.opslaan" });
  }

  describe("without saved searches", () => {
    it("cannot clear the filters when none are active", async () => {
      await setup();
      await loadZoekopdrachten();

      expect(clearButton()).toBeDisabled();
      expect(
        within(clearButton()).queryByTitle("actie.zoekopdracht.wissen"),
      ).not.toBeInTheDocument();
    });

    it("offers to clear the filters when some are active", async () => {
      await setup(makeZoekFilters({ zoeken: { zaakIdentificatie: "ZAAK-1" } }));
      await loadZoekopdrachten();

      expect(clearButton()).toBeEnabled();
      expect(
        within(clearButton()).getByTitle("actie.zoekopdracht.wissen"),
      ).toBeInTheDocument();
    });

    it("clears the stored active search when the filters are cleared", async () => {
      await setup(makeZoekFilters({ zoeken: { zaakIdentificatie: "ZAAK-1" } }));
      await loadZoekopdrachten();

      await user.click(clearButton());
      await sleep();

      const request = httpTestingController.expectOne(REMOVE_ACTIEF_URL);

      expect(request.request.method).toBe("DELETE");

      request.flush(null);
      await sleep();
    });

    it.each([
      [
        "offers to clear the filters when a search term is set",
        makeZoekFilters({ zoeken: { q: "test" } }),
        true,
      ],
      [
        "cannot clear the filters when nothing is searched for",
        makeZoekFilters({ zoeken: {} }),
        false,
      ],
      [
        "offers to clear the filters when a zaak is set",
        makeZoekFilters({
          filtersType: "DetachedDocumentListParameters",
          zaakID: "ZAAK-001",
        }),
        true,
      ],
      [
        "cannot clear the filters when no detached document filter is set",
        makeZoekFilters({ filtersType: "DetachedDocumentListParameters" }),
        false,
      ],
      [
        "offers to clear the filters when a document identification is set",
        makeZoekFilters({
          filtersType: "InboxDocumentListParameters",
          identificatie: "DOC-001",
        }),
        true,
      ],
      [
        "cannot clear the filters when no inbox document filter is set",
        makeZoekFilters({ filtersType: "InboxDocumentListParameters" }),
        false,
      ],
    ])(
      "%s",
      async (
        _description: string,
        zoekFilters: ZoekFilters,
        clearable: boolean,
      ) => {
        await setup(zoekFilters);
        await loadZoekopdrachten();

        expect(clearButton().hasAttribute("disabled")).toBe(!clearable);
      },
    );
  });

  describe("with saved searches", () => {
    it("offers to select a saved search when nothing is filtered", async () => {
      await setup();
      await loadZoekopdrachten([makeZoekopdracht()]);

      expect(selectButton()).toBeVisible();
      expect(
        screen.queryByRole("button", { name: "actie.zoekopdracht.wissen" }),
      ).not.toBeInTheDocument();
    });

    it("offers to clear instead when a saved search is active", async () => {
      await setup();
      await loadZoekopdrachten([makeZoekopdracht({ actief: true })]);

      expect(clearButton()).toBeVisible();
      expect(
        screen.queryByRole("button", { name: "actie.zoekopdracht.selecteer" }),
      ).not.toBeInTheDocument();
    });

    it("offers to clear instead when filters are active", async () => {
      await setup(makeZoekFilters({ zoeken: { zaakIdentificatie: "ZAAK-1" } }));
      await loadZoekopdrachten([makeZoekopdracht()]);

      expect(clearButton()).toBeVisible();
      expect(
        screen.queryByRole("button", { name: "actie.zoekopdracht.selecteer" }),
      ).not.toBeInTheDocument();
    });

    it("lists every saved search in the menu", async () => {
      await setup();
      await loadZoekopdrachten([
        makeZoekopdracht({ id: 1, naam: "Zoek A" }),
        makeZoekopdracht({ id: 2, naam: "Zoek B" }),
      ]);

      await user.click(selectButton());

      expect(screen.getByRole("menuitem", { name: /Zoek A/ })).toBeVisible();
      expect(screen.getByRole("menuitem", { name: /Zoek B/ })).toBeVisible();
    });

    it("activates and reports the saved search that is chosen", async () => {
      await setup();
      await loadZoekopdrachten([
        makeZoekopdracht({ id: 42, naam: "Klik mij" }),
      ]);

      await user.click(selectButton());
      await user.click(screen.getByRole("menuitem", { name: /Klik mij/ }));
      await sleep();

      const request = httpTestingController.expectOne(ACTIEF_URL);

      expect(request.request.method).toBe("PUT");
      expect(request.request.body).toEqual(
        expect.objectContaining({ id: 42, naam: "Klik mij" }),
      );
      expect(zoekopdrachtEmitted).toHaveBeenCalledWith(
        expect.objectContaining({ id: 42 }),
      );

      request.flush(null);
      await sleep();
    });

    it("reports the saved search that is already active on load", async () => {
      await setup();
      await loadZoekopdrachten([makeZoekopdracht({ id: 3, actief: true })]);

      expect(zoekopdrachtEmitted).toHaveBeenCalledWith(
        expect.objectContaining({ id: 3 }),
      );
    });

    it("deletes a saved search without activating it", async () => {
      await setup();
      await loadZoekopdrachten([makeZoekopdracht({ id: 7 })]);

      await user.click(selectButton());
      await user.click(
        within(screen.getByRole("menuitem")).getByTitle(
          "actie.zoekopdracht.verwijderen",
        ),
      );
      await sleep();

      const request = httpTestingController.expectOne(
        "/rest/gebruikersvoorkeuren/zoekopdracht/7",
      );

      expect(request.request.method).toBe("DELETE");

      request.flush(null);
      await sleep();

      httpTestingController.expectOne(ZOEKOPDRACHTEN_URL).flush([]);
      await sleep();
      fixture.detectChanges();
    });

    it("clears the active saved search when the clear button is clicked", async () => {
      await setup();
      await loadZoekopdrachten([makeZoekopdracht({ actief: true })]);

      await user.click(clearButton());
      await flushRemoveActief();

      expect(selectButton()).toBeVisible();
    });

    it("keeps offering to clear when the current filters are still set", async () => {
      await setup(makeZoekFilters({ zoeken: { zaakIdentificatie: "ZAAK-1" } }));
      await loadZoekopdrachten([makeZoekopdracht({ actief: true })]);

      await user.click(clearButton());
      await flushRemoveActief();

      expect(clearButton()).toBeVisible();
    });

    it("clears the active saved search when the filters change", async () => {
      await setup();
      await loadZoekopdrachten([makeZoekopdracht({ actief: true })]);

      filtersChanged.emit();
      await flushRemoveActief();

      expect(selectButton()).toBeVisible();
    });
  });

  describe("saving the current filters", () => {
    it("can save when no saved search is active", async () => {
      await setup();
      await loadZoekopdrachten([makeZoekopdracht()]);

      expect(saveButton()).toBeEnabled();
    });

    it("cannot save while a saved search is active", async () => {
      await setup();
      await loadZoekopdrachten([makeZoekopdracht({ actief: true })]);

      expect(saveButton()).toBeDisabled();
    });

    it("opens the save dialog for the current werklijst", async () => {
      await setup();
      await loadZoekopdrachten();

      await user.click(saveButton());

      expect(dialogOpen).toHaveBeenCalledWith(
        ZoekopdrachtSaveDialogComponent,
        expect.objectContaining({
          data: expect.objectContaining({ lijstID: WERKLIJST }),
        }),
      );
    });

    it("reloads the saved searches after one was saved", async () => {
      await setup();
      await loadZoekopdrachten();

      dialogOpen.mockReturnValue(
        fromPartial<MatDialogRef<ZoekopdrachtSaveDialogComponent>>({
          afterClosed: () => of(true),
        }),
      );

      await user.click(saveButton());
      await loadZoekopdrachten([makeZoekopdracht({ naam: "Nieuw" })]);

      await user.click(selectButton());

      expect(screen.getByRole("menuitem", { name: /Nieuw/ })).toBeVisible();
    });

    it("does not reload the saved searches when the dialog was cancelled", async () => {
      await setup();
      await loadZoekopdrachten();

      await user.click(saveButton());
      await sleep();

      httpTestingController.expectNone(ZOEKOPDRACHTEN_URL);
    });
  });
});
