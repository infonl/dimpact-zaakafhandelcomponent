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
import { MatDialogRef } from "@angular/material/dialog";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { notifyManager } from "@tanstack/query-core";
import { render, screen, within } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { Observable, of } from "rxjs";
import { createMutationOptions, fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../setupJest";
import { UtilService } from "../../core/service/util.service";
import { WebsocketListener } from "../../core/websocket/model/websocket-listener";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZaakDialogService } from "../zaak-dialog.service";
import { ZakenService } from "../zaken.service";
import { ZaakBetrokkeneListComponent } from "./zaak-betrokkene-list.component";

const BETROKKENEN_URL = "/rest/zaken/zaak/fake-zaak-uuid/betrokkene";

const fakeZaak = fromPartial<GeneratedType<"RestZaak">>({
  uuid: "fake-zaak-uuid",
  zaaktype: fromPartial<GeneratedType<"RestZaaktype">>({
    uuid: "fake-zaaktype-uuid",
  }),
  rechten: fromPartial<GeneratedType<"RestZaakRechten">>({
    verwijderenBetrokkene: true,
  }),
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

/**
 * The template gates the gegevens button on `doesExistInKvK`, which the generated
 * betrokkene type does not declare.
 */
const makeKvkBetrokkene = (
  fields: Partial<GeneratedType<"RestZaakBetrokkene">>,
) => ({ ...makeBetrokkene(fields), doesExistInKvK: true });

describe(ZaakBetrokkeneListComponent.name, () => {
  const user = userEvent.setup();

  let fixture: ComponentFixture<ZaakBetrokkeneListComponent>;
  let httpTestingController: HttpTestingController;

  const setup = async (betrokkenen = [makeBetrokkene()]) => {
    notifyManager.setScheduler((fn) => fn());

    const dialogRef = fromPartial<MatDialogRef<unknown>>({
      afterClosed: jest.fn().mockReturnValue(of(undefined)),
    });
    const openOntkoppelBetrokkene = jest.fn().mockReturnValue(dialogRef);

    const deleteBetrokkeneMutation = createMutationOptions<
      GeneratedType<"RestZaak">,
      { reden: string }
    >(fromPartial<GeneratedType<"RestZaak">>({}));
    const deleteBetrokkene = jest
      .spyOn(ZakenService.prototype, "deleteBetrokkene")
      .mockReturnValue(deleteBetrokkeneMutation as never);

    const zaakRollenListener = fromPartial<WebsocketListener>({});

    const rendered = await render(ZaakBetrokkeneListComponent, {
      inputs: { zaak: fakeZaak, zaakRollenListener },
      imports: [NoopAnimationsModule, TranslateModule.forRoot()],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideQueryClient(testQueryClient),
        {
          provide: ZaakDialogService,
          useValue: { openOntkoppelBetrokkene },
        },
      ],
    });

    fixture = rendered.fixture;
    httpTestingController = TestBed.inject(HttpTestingController);

    const utilService = TestBed.inject(UtilService);
    jest.spyOn(utilService, "openSnackbar").mockImplementation();
    const zakenService = TestBed.inject(ZakenService);
    jest.spyOn(zakenService, "cacheZaak");
    httpTestingController.expectOne(BETROKKENEN_URL).flush(betrokkenen);
    await sleep();
    // the table creates the row views in one pass and binds their cells in the next
    fixture.detectChanges();
    fixture.detectChanges();

    return {
      dialogRef,
      openOntkoppelBetrokkene,
      deleteBetrokkene,
      deleteBetrokkeneMutation,
      utilService,
      zakenService,
    };
  };

  afterEach(() => {
    notifyManager.setScheduler(queueMicrotask);
  });

  const betrokkeneRow = () => screen.getByRole("row", { name: /fakeRoltype/ });

  it("shows a row for every betrokkene of the zaak", async () => {
    await setup();

    expect(screen.getAllByRole("row", { name: /fakeRoltype/ })).toHaveLength(1);
    expect(betrokkeneRow()).toHaveTextContent("fakeRoltype");
  });

  describe("looking up the gegevens of a betrokkene", () => {
    const fetchGegevens = async (url: string, response: unknown) => {
      await user.click(
        within(betrokkeneRow()).getByRole("button", {
          name: "actie.betrokkene.gegevens.ophalen",
        }),
      );
      await sleep();
      httpTestingController.expectOne(url).flush(response);
      await sleep();
      fixture.detectChanges();
    };

    it("shows the naam and geboortedatum of a natuurlijk persoon", async () => {
      await setup([
        makeBetrokkene({ temporaryPersonId: "fake-temporary-person-id" }),
      ]);

      await fetchGegevens(
        "/rest/klanten/person/fake-temporary-person-id",
        fromPartial<GeneratedType<"RestPersoon">>({
          naam: "fakeNaam",
          geboortedatum: "2000-01-01",
        }),
      );

      expect(betrokkeneRow()).toHaveTextContent("fakeNaam");
    });

    it.each(["NIET_NATUURLIJK_PERSOON", "VESTIGING"] as const)(
      "shows the naam and adres of a %s",
      async (type) => {
        await setup([
          makeKvkBetrokkene({
            type,
            identificatieType: "VN",
            vestigingsnummer: "11112222",
            kvkNummer: "87654321",
          }),
        ]);

        await fetchGegevens(
          "/rest/klanten/vestiging/11112222/87654321",
          fromPartial<GeneratedType<"RestBedrijf">>({
            naam: "fakeBedrijfNaam",
            adres: fromPartial<GeneratedType<"RestBedrijfAdres">>({
              volledigAdres: "fakeStraat 1, 1234AB fakePlaats",
            }),
          }),
        );

        expect(betrokkeneRow()).toHaveTextContent("fakeBedrijfNaam");
        expect(betrokkeneRow()).toHaveTextContent(
          "fakeStraat 1, 1234AB fakePlaats",
        );
      },
    );

    it("shows only the naam when the bedrijf has no adres", async () => {
      await setup([
        makeKvkBetrokkene({
          type: "NIET_NATUURLIJK_PERSOON",
          identificatieType: "VN",
          vestigingsnummer: "11112222",
          kvkNummer: "87654321",
        }),
      ]);

      await fetchGegevens(
        "/rest/klanten/vestiging/11112222/87654321",
        fromPartial<GeneratedType<"RestBedrijf">>({
          naam: "fakeBedrijfNaam",
          adres: null,
        }),
      );

      expect(
        within(betrokkeneRow()).getByText("fakeBedrijfNaam", { exact: true }),
      ).toBeVisible();
    });

    it("shows a placeholder for a medewerker", async () => {
      await setup([makeBetrokkene({ type: "MEDEWERKER" })]);

      await user.click(
        within(betrokkeneRow()).getByRole("button", {
          name: "actie.betrokkene.gegevens.ophalen",
        }),
      );
      await sleep();
      fixture.detectChanges();

      expect(
        within(betrokkeneRow()).getByText("-", { exact: true }),
      ).toBeVisible();
    });
  });

  describe("ontkoppelen of a betrokkene", () => {
    const clickOntkoppelen = () =>
      user.click(
        within(betrokkeneRow()).getByRole("button", {
          name: "actie.betrokkene.ontkoppelen",
        }),
      );

    it("opens the confirmation dialog", async () => {
      const { openOntkoppelBetrokkene } = await setup();

      await clickOntkoppelen();

      expect(openOntkoppelBetrokkene).toHaveBeenCalled();
    });

    it("deletes the betrokkene with the entered reden only once the dialog subscribes", async () => {
      const {
        openOntkoppelBetrokkene,
        deleteBetrokkene,
        deleteBetrokkeneMutation,
      } = await setup();

      await clickOntkoppelen();
      const request = (
        openOntkoppelBetrokkene.mock.calls[0][1] as (
          reden: string,
        ) => Observable<unknown>
      )("fake-reden");

      expect(deleteBetrokkeneMutation.mutationFn).not.toHaveBeenCalled();

      request.subscribe();
      await sleep();

      expect(deleteBetrokkene).toHaveBeenCalledWith("fake-rol-id");
      expect(deleteBetrokkeneMutation.mutationFn).toHaveBeenCalledWith(
        { reden: "fake-reden" },
        expect.objectContaining({
          client: testQueryClient,
          mutationKey: deleteBetrokkeneMutation.mutationKey,
        }),
      );
    });

    it("shows a snackbar and refetches the betrokkenen once the dialog confirms", async () => {
      const { dialogRef, utilService } = await setup();
      jest.spyOn(dialogRef, "afterClosed").mockReturnValue(of(true));
      const invalidateQueries = jest.spyOn(
        testQueryClient,
        "invalidateQueries",
      );

      await clickOntkoppelen();

      expect(utilService.openSnackbar).toHaveBeenCalledWith(
        "msg.betrokkene.ontkoppelen.uitgevoerd",
        expect.anything(),
      );
      expect(invalidateQueries).toHaveBeenCalled();
      httpTestingController.expectOne(BETROKKENEN_URL).flush([]);
    });

    it("writes the returned zaak into the cache when the dialog closes with one", async () => {
      const { dialogRef, zakenService } = await setup();
      const fakeReturnedZaak = fromPartial<GeneratedType<"RestZaak">>({
        uuid: fakeZaak.uuid,
      });
      jest
        .spyOn(dialogRef, "afterClosed")
        .mockReturnValue(of(fakeReturnedZaak));

      await clickOntkoppelen();

      expect(zakenService.cacheZaak).toHaveBeenCalledWith(fakeReturnedZaak);
    });

    it("does not write to the cache when the dialog closes with a confirmation-only result", async () => {
      const { dialogRef, zakenService } = await setup();
      jest.spyOn(dialogRef, "afterClosed").mockReturnValue(of(true));

      await clickOntkoppelen();

      expect(zakenService.cacheZaak).not.toHaveBeenCalled();
    });

    it("does nothing when the dialog is cancelled", async () => {
      const { utilService } = await setup();

      await clickOntkoppelen();

      expect(utilService.openSnackbar).not.toHaveBeenCalled();
    });
  });
});
