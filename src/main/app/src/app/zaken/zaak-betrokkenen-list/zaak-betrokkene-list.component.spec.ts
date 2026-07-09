/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import {
  provideHttpClient,
  withInterceptorsFromDi,
} from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatDialog, MatDialogRef } from "@angular/material/dialog";
import { MatTableHarness } from "@angular/material/table/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { notifyManager } from "@tanstack/query-core";
import { of } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { testQueryClient } from "../../../../setupJest";
import { UtilService } from "../../core/service/util.service";
import { WebsocketListener } from "../../core/websocket/model/websocket-listener";
import { WebsocketService } from "../../core/websocket/websocket.service";
import { KlantenService } from "../../klanten/klanten.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenService } from "../zaken.service";
import { ZaakBetrokkeneListComponent } from "./zaak-betrokkene-list.component";

const makeZaak = (
  fields: Partial<GeneratedType<"RestZaak">> = {},
): GeneratedType<"RestZaak"> =>
  fromPartial<GeneratedType<"RestZaak">>({
    uuid: "fake-zaak-uuid",
    zaaktype: fromPartial<GeneratedType<"RestZaaktype">>({
      uuid: "fake-zaaktype-uuid",
    }),
    rechten: fromPartial<GeneratedType<"RestZaakRechten">>({
      verwijderenBetrokkene: true,
    }),
    ...fields,
  });

const makeBetrokkene = (
  fields: Partial<GeneratedType<"RestZaakBetrokkene">> = {},
): GeneratedType<"RestZaakBetrokkene"> =>
  fromPartial<GeneratedType<"RestZaakBetrokkene">>({
    rolid: "fake-rol-id",
    roltype: "fakeRoltype",
    roltoelichting: "fakeRoltoelichting",
    type: "NATUURLIJK_PERSOON",
    identificatieType: "BSN",
    bsn: "123456789",
    ...fields,
  });

describe(ZaakBetrokkeneListComponent.name, () => {
  let fixture: ComponentFixture<ZaakBetrokkeneListComponent>;
  let component: ZaakBetrokkeneListComponent;
  let loader: HarnessLoader;
  let zakenService: ZakenService;
  let klantenService: KlantenService;
  let websocketService: WebsocketService;
  let utilService: UtilService;
  let dialogRef: MatDialogRef<unknown>;

  const fakeZaak = makeZaak();

  beforeEach(() => {
    notifyManager.setScheduler((fn) => fn());
  });

  afterEach(() => {
    notifyManager.setScheduler(queueMicrotask);
    testQueryClient.clear();
    jest.clearAllMocks();
  });

  beforeEach(async () => {
    dialogRef = fromPartial<MatDialogRef<unknown>>({
      afterClosed: jest.fn().mockReturnValue(of(undefined)),
    });

    await TestBed.configureTestingModule({
      imports: [
        ZaakBetrokkeneListComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideQueryClient(testQueryClient),
        {
          provide: MatDialog,
          useValue: { open: jest.fn().mockReturnValue(dialogRef) },
        },
      ],
    }).compileComponents();

    zakenService = TestBed.inject(ZakenService);
    klantenService = TestBed.inject(KlantenService);
    utilService = TestBed.inject(UtilService);
    jest.spyOn(utilService, "openSnackbar").mockImplementation();

    websocketService = TestBed.inject(WebsocketService);
    jest.spyOn(websocketService, "suspendListener").mockImplementation();

    jest
      .spyOn(zakenService, "deleteBetrokkene")
      .mockReturnValue(of(fromPartial<GeneratedType<"RestZaak">>({})));

    testQueryClient.setQueryData(
      zakenService.listBetrokkenenVoorZaakQuery(fakeZaak.uuid).queryKey,
      [makeBetrokkene()],
    );

    fixture = TestBed.createComponent(ZaakBetrokkeneListComponent);
    fixture.componentRef.setInput("zaak", fakeZaak);
    fixture.componentRef.setInput(
      "zaakRollenListener",
      fromPartial<WebsocketListener>({}),
    );
    fixture.detectChanges();
    component = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);
  });

  describe("table rendering", () => {
    it("renders the betrokkenen fetched for the zaak", async () => {
      const table = await loader.getHarness(MatTableHarness);
      const rows = await table.getRows();
      expect(rows.length).toBe(1);
    });

    it("renders the roltype of the fetched betrokkene", async () => {
      const table = await loader.getHarness(MatTableHarness);
      const rows = await table.getRows();
      const cellText = await rows[0].getCellTextByColumnName();
      expect(cellText["roltype"]).toBe("fakeRoltype");
    });
  });

  describe("betrokkeneGegevensOphalen", () => {
    it("fetches and formats the naam and geboortedatum for a natuurlijk persoon", async () => {
      const betrokkene: GeneratedType<"RestZaakBetrokkene"> & {
        gegevens?: string | null;
      } = makeBetrokkene({
        type: "NATUURLIJK_PERSOON",
        temporaryPersonId: "fake-temporary-person-id",
      });
      testQueryClient.setQueryData(
        klantenService.readPersoon(
          betrokkene.temporaryPersonId!,
          fakeZaak.zaaktype.uuid,
        ).queryKey,
        fromPartial<GeneratedType<"RestPersoon">>({
          naam: "fakeNaam",
          geboortedatum: "2000-01-01",
        }),
      );

      await component["betrokkeneGegevensOphalen"](betrokkene);

      expect(betrokkene.gegevens).toContain("fakeNaam");
    });

    it("sets a placeholder for organisatorische eenheid and medewerker betrokkenen", async () => {
      const betrokkene: GeneratedType<"RestZaakBetrokkene"> & {
        gegevens?: string | null;
      } = makeBetrokkene({ type: "MEDEWERKER" });

      await component["betrokkeneGegevensOphalen"](betrokkene);

      expect(betrokkene.gegevens).toBe("-");
    });
  });

  describe("deleteBetrokkene", () => {
    it("suspends the zaakRollenListener before opening the confirmation dialog", () => {
      component["deleteBetrokkene"](makeBetrokkene());

      expect(websocketService.suspendListener).toHaveBeenCalledWith(
        component.zaakRollenListener(),
      );
    });

    it("shows a snackbar and invalidates the betrokkenen query on confirmation", () => {
      jest.spyOn(dialogRef, "afterClosed").mockReturnValue(of(true));
      const invalidateSpy = jest.spyOn(testQueryClient, "invalidateQueries");

      component["deleteBetrokkene"](makeBetrokkene());

      expect(utilService.openSnackbar).toHaveBeenCalledWith(
        "msg.betrokkene.ontkoppelen.uitgevoerd",
        expect.anything(),
      );
      expect(invalidateSpy).toHaveBeenCalledWith({
        queryKey: zakenService.listBetrokkenenVoorZaakQuery(fakeZaak.uuid)
          .queryKey,
      });
    });

    it("does nothing when the dialog is cancelled", () => {
      jest.spyOn(dialogRef, "afterClosed").mockReturnValue(of(undefined));

      component["deleteBetrokkene"](makeBetrokkene());

      expect(utilService.openSnackbar).not.toHaveBeenCalled();
    });
  });
});
