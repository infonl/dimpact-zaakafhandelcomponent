/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { provideHttpClient } from "@angular/common/http";
import { EventEmitter } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatButtonHarness } from "@angular/material/button/testing";
import { MatDialog, MatDialogRef } from "@angular/material/dialog";
import { MatIconHarness } from "@angular/material/icon/testing";
import { MatMenuHarness } from "@angular/material/menu/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { of } from "rxjs";
import { GeneratedType } from "../../shared/utils/generated-types";
import { GebruikersvoorkeurenService } from "../gebruikersvoorkeuren.service";
import { ZoekopdrachtSaveDialogComponent } from "../zoekopdracht-save-dialog/zoekopdracht-save-dialog.component";
import { ZoekFilters } from "./zoekfilters.model";
import { ZoekopdrachtComponent } from "./zoekopdracht.component";

// ---------------------------------------------------------------------------
// Factories
// ---------------------------------------------------------------------------

const makeZoekopdracht = (
  fields: Partial<GeneratedType<"RESTZoekopdracht">> = {},
): GeneratedType<"RESTZoekopdracht"> =>
  ({
    id: 1,
    naam: "Test zoekopdracht",
    actief: false,
    werklijstID: "MIJN_ZAKEN",
    ...fields,
  }) as Partial<
    GeneratedType<"RESTZoekopdracht">
  > as unknown as GeneratedType<"RESTZoekopdracht">;

const makeZoekFilters = (fields: Partial<ZoekFilters> = {}): ZoekFilters => ({
  filtersType: "ZoekParameters",
  zoeken: {},
  filters: {},
  datums: {},
  ...fields,
});

// ---------------------------------------------------------------------------
// Spec
// ---------------------------------------------------------------------------

describe(ZoekopdrachtComponent.name, () => {
  let fixture: ComponentFixture<ZoekopdrachtComponent>;
  let loader: HarnessLoader;
  let component: ZoekopdrachtComponent;
  let service: GebruikersvoorkeurenService;
  let dialog: MatDialog;
  let filtersChanged: EventEmitter<void>;

  beforeEach(async () => {
    filtersChanged = new EventEmitter<void>();

    await TestBed.configureTestingModule({
      imports: [
        ZoekopdrachtComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [provideHttpClient()],
    }).compileComponents();

    service = TestBed.inject(GebruikersvoorkeurenService);
    dialog = TestBed.inject(MatDialog);

    jest.spyOn(service, "listZoekOpdrachten").mockReturnValue(of([]));
    jest
      .spyOn(service, "deleteZoekOpdrachten")
      .mockReturnValue(of(undefined) as never);
    jest
      .spyOn(service, "setZoekopdrachtActief")
      .mockReturnValue(of(undefined) as never);
    jest
      .spyOn(service, "removeZoekopdrachtActief")
      .mockReturnValue(of(undefined) as never);
    jest.spyOn(dialog, "open").mockReturnValue(
      ({
        afterClosed: () => of(null),
      }) satisfies Pick<
        MatDialogRef<ZoekopdrachtSaveDialogComponent>,
        "afterClosed"
      > as unknown as MatDialogRef<ZoekopdrachtSaveDialogComponent>,
    );

    fixture = TestBed.createComponent(ZoekopdrachtComponent);
    component = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);

    component.werklijst = "MIJN_ZAKEN";
    component.zoekFilters = makeZoekFilters();
    component.filtersChanged = filtersChanged;

    fixture.detectChanges();
  });

  // -------------------------------------------------------------------------
  // Empty-list state: clearZoekopdrachtButton1
  // -------------------------------------------------------------------------

  describe("when no saved searches exist", () => {
    it("shows a disabled filter button when no active filters", () => {
      const nativeElement = fixture.nativeElement as HTMLElement;
      const btn = nativeElement.querySelector<HTMLButtonElement>(
        "#clearZoekopdrachtButton1",
      );
      expect(btn?.disabled).toBe(true);
    });

    it("shows filter_alt icon without the clear overlay", async () => {
      const icons = await loader.getAllHarnesses(
        MatIconHarness.with({ name: "filter_alt" }),
      );
      expect(icons).toHaveLength(1);
      const clearIcons = await loader.getAllHarnesses(
        MatIconHarness.with({ name: "filter_alt_off" }),
      );
      expect(clearIcons).toHaveLength(0);
    });

    it("renders the clear overlay icon when actieveFilters is true", async () => {
      component["actieveFilters"] = true;
      fixture.detectChanges();

      const clearIcons = await loader.getAllHarnesses(
        MatIconHarness.with({ name: "filter_alt_off" }),
      );
      expect(clearIcons).toHaveLength(1);
    });

    it("enables the filter button when actieveFilters is true", () => {
      component["actieveFilters"] = true;
      fixture.detectChanges();

      const nativeElement = fixture.nativeElement as HTMLElement;
      const btn = nativeElement.querySelector<HTMLButtonElement>(
        "#clearZoekopdrachtButton1",
      );
      expect(btn?.disabled).toBe(false);
    });

    it("calls clearActief(true) and removeZoekopdrachtActief when filter button clicked", async () => {
      component["actieveFilters"] = true;
      fixture.detectChanges();

      const btn = await loader.getHarness(
        MatButtonHarness.with({ selector: "#clearZoekopdrachtButton1" }),
      );
      await btn.click();

      expect(service.removeZoekopdrachtActief).toHaveBeenCalledWith(
        "MIJN_ZAKEN",
      );
    });
  });

  // -------------------------------------------------------------------------
  // Non-empty list — selecteerZoekopdrachtButton / clearZoekopdrachtButton2
  // -------------------------------------------------------------------------

  describe("when saved searches exist", () => {
    beforeEach(() => {
      component["zoekopdrachten"] = [makeZoekopdracht({ naam: "Mijn zoek" })];
      component["actieveZoekopdracht"] = null;
      component["actieveFilters"] = false;
      fixture.detectChanges();
    });

    it("shows selecteer button when no active search and no active filters", async () => {
      const nativeElement = fixture.nativeElement as HTMLElement;
      expect(
        nativeElement.querySelector("#selecteerZoekopdrachtButton"),
      ).not.toBeNull();
      expect(
        nativeElement.querySelector("#clearZoekopdrachtButton2"),
      ).toBeNull();
    });

    it("hides selecteer button and shows clear button when actieveZoekopdracht is set", async () => {
      component["actieveZoekopdracht"] = makeZoekopdracht({ naam: "Actief" });
      fixture.detectChanges();

      const nativeElement = fixture.nativeElement as HTMLElement;
      expect(
        nativeElement.querySelector("#selecteerZoekopdrachtButton"),
      ).toBeNull();
      expect(
        nativeElement.querySelector("#clearZoekopdrachtButton2"),
      ).not.toBeNull();
    });

    it("hides selecteer button and shows clear button when actieveFilters is true", () => {
      component["actieveFilters"] = true;
      fixture.detectChanges();

      const nativeElement = fixture.nativeElement as HTMLElement;
      expect(
        nativeElement.querySelector("#selecteerZoekopdrachtButton"),
      ).toBeNull();
      expect(
        nativeElement.querySelector("#clearZoekopdrachtButton2"),
      ).not.toBeNull();
    });

    it("renders one menu item per saved search in the filter menu", async () => {
      component["zoekopdrachten"] = [
        makeZoekopdracht({ id: 1, naam: "Zoek A" }),
        makeZoekopdracht({ id: 2, naam: "Zoek B" }),
      ];
      fixture.detectChanges();

      const menu = await loader.getHarness(MatMenuHarness);
      await menu.open();
      const items = await menu.getItems();
      expect(items).toHaveLength(2);
    });

    it("calls setZoekopdrachtActief and emits zoekopdracht when menu item clicked", async () => {
      const zoek = makeZoekopdracht({ id: 42, naam: "Klik mij" });
      component["zoekopdrachten"] = [zoek];
      fixture.detectChanges();

      const emitted: GeneratedType<"RESTZoekopdracht">[] = [];
      component.zoekopdracht.subscribe((v) => emitted.push(v));

      const menu = await loader.getHarness(MatMenuHarness);
      await menu.open();
      const [item] = await menu.getItems();
      await item.click();

      expect(service.setZoekopdrachtActief).toHaveBeenCalledWith(
        expect.objectContaining({ id: 42 }),
      );
      expect(emitted).toHaveLength(1);
      expect(emitted[0]).toEqual(expect.objectContaining({ id: 42 }));
    });
  });

  // -------------------------------------------------------------------------
  // Save button
  // -------------------------------------------------------------------------

  describe("saveZoekopdrachtButton", () => {
    it("is enabled when no active search exists", async () => {
      component["actieveZoekopdracht"] = null;
      fixture.detectChanges();

      const nativeElement = fixture.nativeElement as HTMLElement;
      const btn = nativeElement.querySelector<HTMLButtonElement>(
        "#saveZoekopdrachtButton",
      );
      expect(btn?.disabled).toBe(false);
    });

    it("is disabled when an active search exists", async () => {
      component["actieveZoekopdracht"] = makeZoekopdracht();
      fixture.detectChanges();

      const nativeElement = fixture.nativeElement as HTMLElement;
      const btn = nativeElement.querySelector<HTMLButtonElement>(
        "#saveZoekopdrachtButton",
      );
      expect(btn?.disabled).toBe(true);
    });

    it("opens the save dialog on click", async () => {
      const btn = await loader.getHarness(
        MatButtonHarness.with({ selector: "#saveZoekopdrachtButton" }),
      );
      await btn.click();
      expect(dialog.open).toHaveBeenCalledWith(
        ZoekopdrachtSaveDialogComponent,
        expect.objectContaining({
          data: expect.objectContaining({
            lijstID: "MIJN_ZAKEN",
          }),
        }),
      );
    });

    it("reloads zoekopdrachten when save dialog closes with truthy result", () => {
      (dialog.open as jest.Mock).mockReturnValue(
        ({
          afterClosed: () => of(true),
        }) satisfies Pick<
          MatDialogRef<ZoekopdrachtSaveDialogComponent>,
          "afterClosed"
        > as unknown as MatDialogRef<ZoekopdrachtSaveDialogComponent>,
      );

      component["saveSearch"]();

      expect(service.listZoekOpdrachten).toHaveBeenCalledTimes(2);
    });

    it("does not reload zoekopdrachten when save dialog closes with falsy result", () => {
      (dialog.open as jest.Mock).mockReturnValue(
        ({
          afterClosed: () => of(null),
        }) satisfies Pick<
          MatDialogRef<ZoekopdrachtSaveDialogComponent>,
          "afterClosed"
        > as unknown as MatDialogRef<ZoekopdrachtSaveDialogComponent>,
      );

      component["saveSearch"]();

      expect(service.listZoekOpdrachten).toHaveBeenCalledTimes(1);
    });
  });

  // -------------------------------------------------------------------------
  // deleteZoekopdracht
  // -------------------------------------------------------------------------

  describe("deleteZoekopdracht", () => {
    it("stops event propagation and calls deleteZoekOpdrachten", () => {
      const zoek = makeZoekopdracht({ id: 7 });
      const event = new MouseEvent("click");
      const stopSpy = jest.spyOn(event, "stopPropagation");

      component["deleteZoekopdracht"](event, zoek);

      expect(stopSpy).toHaveBeenCalled();
      expect(service.deleteZoekOpdrachten).toHaveBeenCalledWith(7);
    });

    it("reloads zoekopdrachten after deletion", () => {
      const zoek = makeZoekopdracht({ id: 7 });
      component["deleteZoekopdracht"](new MouseEvent("click"), zoek);

      expect(service.listZoekOpdrachten).toHaveBeenCalledTimes(2);
    });
  });

  // -------------------------------------------------------------------------
  // clearActief
  // -------------------------------------------------------------------------

  describe("clearActief", () => {
    it("clears the active search and calls removeZoekopdrachtActief", () => {
      component["actieveZoekopdracht"] = makeZoekopdracht();
      component["clearActief"]();
      expect(component["actieveZoekopdracht"]).toBeNull();
      expect(service.removeZoekopdrachtActief).toHaveBeenCalledWith(
        "MIJN_ZAKEN",
      );
    });

    it("updates actieveFilters based on current zoekFilters after clearing", () => {
      component["actieveZoekopdracht"] = makeZoekopdracht();
      component.zoekFilters = makeZoekFilters({
        zoeken: { zaakIdentificatie: "ZAAK-001" },
      });
      component["clearActief"]();
      expect(component["actieveFilters"]).toBe(true);
    });
  });

  // -------------------------------------------------------------------------
  // filtersChanged subscription
  // -------------------------------------------------------------------------

  describe("filtersChanged input subscription", () => {
    it("clears the active search when filtersChanged emits", () => {
      component["actieveZoekopdracht"] = makeZoekopdracht();
      filtersChanged.emit();
      expect(component["actieveZoekopdracht"]).toBeNull();
    });
  });

  // -------------------------------------------------------------------------
  // ngOnInit — loads searches and marks actief
  // -------------------------------------------------------------------------

  describe("ngOnInit", () => {
    it("sets actieveZoekopdracht to the search marked as actief after load", async () => {
      const actief = makeZoekopdracht({ id: 3, actief: true });
      (service.listZoekOpdrachten as jest.Mock).mockReturnValue(of([actief]));

      component["loadZoekopdrachten"]();

      expect(component["actieveZoekopdracht"]).toEqual(
        expect.objectContaining({ id: 3 }),
      );
    });

    it("emits the active zoekopdracht on load when one is marked actief", () => {
      const actief = makeZoekopdracht({ id: 5, actief: true });
      (service.listZoekOpdrachten as jest.Mock).mockReturnValue(of([actief]));

      const emitted: GeneratedType<"RESTZoekopdracht">[] = [];
      component.zoekopdracht.subscribe((v) => emitted.push(v));

      component["loadZoekopdrachten"]();

      expect(emitted).toHaveLength(1);
      expect(emitted[0]).toEqual(expect.objectContaining({ id: 5 }));
    });
  });

  // -------------------------------------------------------------------------
  // heeftActieveFilters — per filtersType branch
  // -------------------------------------------------------------------------

  describe("heeftActieveFilters", () => {
    it("returns true for ZoekParameters when zoeken has a value", () => {
      component.zoekFilters = makeZoekFilters({
        filtersType: "ZoekParameters",
        zoeken: { q: "test" },
      });
      expect(component["heeftActieveFilters"]()).toBe(true);
    });

    it("returns false for ZoekParameters when zoeken is empty", () => {
      component.zoekFilters = makeZoekFilters({
        filtersType: "ZoekParameters",
        zoeken: {},
      });
      expect(component["heeftActieveFilters"]()).toBe(false);
    });

    it("returns true for OntkoppeldDocumentListParameters when zaakID is set", () => {
      component.zoekFilters = makeZoekFilters({
        filtersType: "OntkoppeldDocumentListParameters",
        zaakID: "ZAAK-001",
      });
      expect(component["heeftActieveFilters"]()).toBe(true);
    });

    it("returns false for OntkoppeldDocumentListParameters when no fields set", () => {
      component.zoekFilters = makeZoekFilters({
        filtersType: "OntkoppeldDocumentListParameters",
      });
      expect(component["heeftActieveFilters"]()).toBe(false);
    });

    it("returns true for InboxDocumentListParameters when identificatie is set", () => {
      component.zoekFilters = makeZoekFilters({
        filtersType: "InboxDocumentListParameters",
        identificatie: "DOC-001",
      });
      expect(component["heeftActieveFilters"]()).toBe(true);
    });

    it("returns false for InboxDocumentListParameters when no fields set", () => {
      component.zoekFilters = makeZoekFilters({
        filtersType: "InboxDocumentListParameters",
      });
      expect(component["heeftActieveFilters"]()).toBe(false);
    });
  });
});
