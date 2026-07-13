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
import { MatDialogRef } from "@angular/material/dialog";
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
import { BetrokkeneIdentificatie } from "../model/betrokkeneIdentificatie";
import { ZaakDialogService } from "../zaak-dialog.service";
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
  let openOntkoppelBetrokkeneSpy: jest.Mock;

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
    openOntkoppelBetrokkeneSpy = jest.fn().mockReturnValue(dialogRef);

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
          provide: ZaakDialogService,
          useValue: { openOntkoppelBetrokkene: openOntkoppelBetrokkeneSpy },
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

    it.each(["NIET_NATUURLIJK_PERSOON", "VESTIGING"] as const)(
      "fetches and formats the naam and adres for a %s",
      async (type) => {
        const betrokkene: GeneratedType<"RestZaakBetrokkene"> & {
          gegevens?: string | null;
        } = makeBetrokkene({
          type,
          identificatieType: "VN",
          vestigingsnummer: "11112222",
          kvkNummer: "87654321",
        });
        testQueryClient.setQueryData(
          klantenService.readBedrijf(new BetrokkeneIdentificatie(betrokkene))
            .queryKey,
          fromPartial<GeneratedType<"RestBedrijf">>({
            naam: "fakeBedrijfNaam",
            adres: fromPartial<GeneratedType<"RestBedrijfAdres">>({
              volledigAdres: "fakeStraat 1, 1234AB fakePlaats",
            }),
          }),
        );

        await component["betrokkeneGegevensOphalen"](betrokkene);

        expect(betrokkene.gegevens).toContain("fakeBedrijfNaam");
        expect(betrokkene.gegevens).toContain(
          "fakeStraat 1, 1234AB fakePlaats",
        );
      },
    );

    it("does not append an adres when the bedrijf has none", async () => {
      const betrokkene: GeneratedType<"RestZaakBetrokkene"> & {
        gegevens?: string | null;
      } = makeBetrokkene({
        type: "NIET_NATUURLIJK_PERSOON",
        identificatieType: "VN",
        vestigingsnummer: "11112222",
        kvkNummer: "87654321",
      });
      testQueryClient.setQueryData(
        klantenService.readBedrijf(new BetrokkeneIdentificatie(betrokkene))
          .queryKey,
        fromPartial<GeneratedType<"RestBedrijf">>({
          naam: "fakeBedrijfNaam",
          adres: null,
        }),
      );

      await component["betrokkeneGegevensOphalen"](betrokkene);

      expect(betrokkene.gegevens).toBe("fakeBedrijfNaam");
    });
  });

  describe("deleteBetrokkene", () => {
    it("suspends the zaakRollenListener before opening the confirmation dialog", () => {
      component["deleteBetrokkene"](makeBetrokkene());

      expect(websocketService.suspendListener).toHaveBeenCalledWith(
        component.zaakRollenListener(),
      );
    });

    it("calls zakenService.deleteBetrokkene with the betrokkene's rolid and the entered reden", () => {
      const betrokkene = makeBetrokkene({ rolid: "fake-rol-id" });

      component["deleteBetrokkene"](betrokkene);

      const callback = openOntkoppelBetrokkeneSpy.mock.calls[0][1] as (
        reden: string,
      ) => unknown;
      callback("fake-reden");

      expect(zakenService.deleteBetrokkene).toHaveBeenCalledWith(
        "fake-rol-id",
        "fake-reden",
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
