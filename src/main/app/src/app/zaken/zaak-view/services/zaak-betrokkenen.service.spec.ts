/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { MatDialogRef } from "@angular/material/dialog";
import { MatSidenav } from "@angular/material/sidenav";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { Subject } from "rxjs";
import { testQueryClient } from "../../../../../setupJest";
import { fromPartial } from "../../../../test-helpers";
import { UtilService } from "../../../core/service/util.service";
import { FoutAfhandelingService } from "../../../fout-afhandeling/fout-afhandeling.service";
import { KlantGegevens } from "../../../klanten/model/klanten/klant-gegevens";
import { RedenDialogFormComponent } from "../../../shared/dialog/reden-dialog-form/reden-dialog-form.component";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { ZaakDialogService } from "../../zaak-dialog.service";
import { ZakenService } from "../../zaken.service";
import { ZaakBetrokkenenService } from "./zaak-betrokkenen.service";
import { ZaakSideActionService } from "./zaak-side-action.service";

describe(ZaakBetrokkenenService.name, () => {
  let service: ZaakBetrokkenenService;
  let sideActions: ZaakSideActionService;
  let zakenService: ZakenService;
  let zaakDialogService: ZaakDialogService;
  let utilService: UtilService;
  let httpTestingController: HttpTestingController;
  let foutAfhandelen: jest.Mock;
  let invalidateSpy: jest.SpyInstance;
  let sidenav: { open: jest.Mock; close: jest.Mock };
  let closed: Subject<unknown>;

  const zaakMetInitiator = fromPartial<GeneratedType<"RestZaak">>({
    uuid: "fakeZaakUuid",
    initiatorIdentificatie: fromPartial<
      GeneratedType<"BetrokkeneIdentificatie">
    >({ type: "BSN", bsn: "fakeBsn" }),
  });

  const zaakZonderInitiator = fromPartial<GeneratedType<"RestZaak">>({
    uuid: "fakeZaakUuid",
    initiatorIdentificatie: undefined,
  });

  const initiator = fromPartial<GeneratedType<"RestPersoon">>({
    naam: "fakeInitiatorNaam",
    identificatieType: "BSN",
    bsn: "fakeInitiatorBsn",
    temporaryPersonId: "fakeTemporaryPersonId",
  });

  const gekoppeldeZaak = fromPartial<GeneratedType<"RestZaak">>({
    uuid: "fakeZaakUuid",
    initiatorIdentificatie: fromPartial<
      GeneratedType<"BetrokkeneIdentificatie">
    >({
      type: "VN",
      kvkNummer: "fakeKvkNummer",
      vestigingsnummer: "fakeVestigingsnummer",
    }),
  });

  const dialogRefClosingWithReden = () =>
    fromPartial<MatDialogRef<RedenDialogFormComponent>>({
      afterClosed: () => closed.asObservable(),
    });

  const closedWith = (result: unknown) => {
    closed.next(result);
    closed.complete();
  };

  const createKlantGegevens = () => {
    const klantGegevens = new KlantGegevens(
      fromPartial<GeneratedType<"RestPersoon">>({
        naam: "fakeKlantNaam",
        identificatieType: "BSN",
        bsn: "fakeKlantBsn",
        temporaryPersonId: "fakeKlantTemporaryPersonId",
      }),
    );
    klantGegevens.betrokkeneRoltype = fromPartial<GeneratedType<"RestRoltype">>(
      { uuid: "fakeRoltypeUuid", naam: "fakeRoltypeNaam" },
    );
    klantGegevens.betrokkeneToelichting = "fakeToelichting";
    return klantGegevens;
  };

  /** The mutation runs as a promise, so the request is only in flight a tick later. */
  const pendingRequest = async (url: string) => {
    await new Promise(requestAnimationFrame);
    return httpTestingController.expectOne(url);
  };

  const settle = () => new Promise(requestAnimationFrame);

  beforeEach(() => {
    closed = new Subject<unknown>();
    foutAfhandelen = jest.fn();

    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot()],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideQueryClient(testQueryClient),
        { provide: FoutAfhandelingService, useValue: { foutAfhandelen } },
        ZaakSideActionService,
        ZaakBetrokkenenService,
      ],
    });

    service = TestBed.inject(ZaakBetrokkenenService);
    sideActions = TestBed.inject(ZaakSideActionService);
    zakenService = TestBed.inject(ZakenService);
    zaakDialogService = TestBed.inject(ZaakDialogService);
    utilService = TestBed.inject(UtilService);
    httpTestingController = TestBed.inject(HttpTestingController);

    sidenav = { open: jest.fn(), close: jest.fn() };
    sideActions.register(fromPartial<MatSidenav>(sidenav));

    jest.spyOn(utilService, "openSnackbar").mockImplementation();
    jest.spyOn(zakenService, "cacheZaak").mockImplementation();
    invalidateSpy = jest.spyOn(testQueryClient, "invalidateQueries");
  });

  describe("initiatorGeselecteerd", () => {
    it("closes the side action panel, because the choice has been made", () => {
      service.initiatorGeselecteerd(zaakZonderInitiator, initiator);

      expect(sidenav.close).toHaveBeenCalled();
    });

    it("couples the initiator straight away when the zaak has none yet", async () => {
      service.initiatorGeselecteerd(zaakZonderInitiator, initiator);

      const request = await pendingRequest("/rest/zaken/initiator");

      expect(request.request.method).toBe("PATCH");
      expect(request.request.body).toEqual(
        expect.objectContaining({
          zaakUUID: "fakeZaakUuid",
          betrokkeneIdentificatie: expect.objectContaining({
            type: "BSN",
            bsn: "fakeInitiatorBsn",
          }),
        }),
      );
    });

    it("reports the coupling when the zaak had no initiator", async () => {
      service.initiatorGeselecteerd(zaakZonderInitiator, initiator);
      (await pendingRequest("/rest/zaken/initiator")).flush(gekoppeldeZaak);
      await settle();

      expect(utilService.openSnackbar).toHaveBeenCalledWith(
        "msg.initiator.gekoppeld",
        { naam: "fakeKvkNummer - fakeVestigingsnummer" },
      );
    });

    it("caches the zaak and refreshes the historie once the server has accepted the coupling", async () => {
      service.initiatorGeselecteerd(zaakZonderInitiator, initiator);
      (await pendingRequest("/rest/zaken/initiator")).flush(gekoppeldeZaak);
      await settle();

      expect(zakenService.cacheZaak).toHaveBeenCalledWith(gekoppeldeZaak);
      expect(invalidateSpy).toHaveBeenCalledWith(
        {
          queryKey:
            zakenService.listHistorieVoorZaakQuery("fakeZaakUuid").queryKey,
        },
        { cancelRefetch: false },
      );
    });

    it("reports a failing coupling through the error handler, without announcing success", async () => {
      service.initiatorGeselecteerd(zaakZonderInitiator, initiator);
      (await pendingRequest("/rest/zaken/initiator")).flush(null, {
        status: 500,
        statusText: "Server Error",
      });
      await settle();

      expect(foutAfhandelen).toHaveBeenCalled();
      expect(utilService.openSnackbar).not.toHaveBeenCalled();
      expect(zakenService.cacheZaak).not.toHaveBeenCalled();
    });

    it("asks for a reason before replacing an initiator the zaak already has", async () => {
      const openWijzigInitiator = jest
        .spyOn(zaakDialogService, "openWijzigInitiator")
        .mockImplementation(dialogRefClosingWithReden);

      service.initiatorGeselecteerd(zaakMetInitiator, initiator);
      await settle();

      expect(openWijzigInitiator).toHaveBeenCalledWith(
        "fakeInitiatorNaam",
        expect.any(Function),
      );
      httpTestingController.expectNone("/rest/zaken/initiator");
    });

    it("reports the change once the reason dialog has replaced the initiator", () => {
      jest
        .spyOn(zaakDialogService, "openWijzigInitiator")
        .mockImplementation(dialogRefClosingWithReden);

      service.initiatorGeselecteerd(zaakMetInitiator, initiator);
      closedWith(gekoppeldeZaak);

      expect(utilService.openSnackbar).toHaveBeenCalledWith(
        "msg.initiator.gewijzigd",
        { naam: "fakeKvkNummer - fakeVestigingsnummer" },
      );
    });

    it("leaves the zaak alone when the reason dialog is cancelled", () => {
      jest
        .spyOn(zaakDialogService, "openWijzigInitiator")
        .mockImplementation(dialogRefClosingWithReden);

      service.initiatorGeselecteerd(zaakMetInitiator, initiator);
      closedWith(undefined);

      expect(zakenService.cacheZaak).not.toHaveBeenCalled();
      expect(utilService.openSnackbar).not.toHaveBeenCalled();
    });

    it("passes the reason the dialog collected on to the update", async () => {
      jest
        .spyOn(zaakDialogService, "openWijzigInitiator")
        .mockImplementation(dialogRefClosingWithReden);

      service.initiatorGeselecteerd(zaakMetInitiator, initiator);
      jest
        .mocked(zaakDialogService.openWijzigInitiator)
        .mock.calls.at(-1)![1]("fakeReden")
        .subscribe({ error: () => undefined });

      const request = await pendingRequest("/rest/zaken/initiator");

      expect(request.request.body).toEqual(
        expect.objectContaining({
          zaakUUID: "fakeZaakUuid",
          toelichting: "fakeReden",
        }),
      );
    });
  });

  describe("deleteInitiator", () => {
    beforeEach(() => {
      jest
        .spyOn(zaakDialogService, "openOntkoppelInitiator")
        .mockImplementation(dialogRefClosingWithReden);
    });

    it("forgets the active panel whether or not the ontkoppelen went through", () => {
      sideActions.activeAction.set("actie.initiator.koppelen");

      service.deleteInitiator(zaakMetInitiator);
      closedWith(undefined);

      expect(sideActions.activeAction()).toBeNull();
    });

    it("reports nothing when the reason dialog is cancelled", () => {
      service.deleteInitiator(zaakMetInitiator);
      closedWith(undefined);

      expect(utilService.openSnackbar).not.toHaveBeenCalled();
    });

    it("sends the reason the dialog collected to the initiator of the zaak", async () => {
      service.deleteInitiator(zaakMetInitiator);
      jest
        .mocked(zaakDialogService.openOntkoppelInitiator)
        .mock.calls.at(-1)![0]("fakeReden")
        .subscribe({ error: () => undefined });

      const request = await pendingRequest(
        "/rest/zaken/fakeZaakUuid/initiator",
      );

      expect(request.request.method).toBe("DELETE");
      expect(request.request.body).toEqual({ reden: "fakeReden" });
    });

    it("reports the ontkoppelen when the dialog confirms, leaving the zaak to the mutation", () => {
      const readZaak = jest.spyOn(zakenService, "readZaak");

      service.deleteInitiator(zaakMetInitiator);
      closedWith(zaakZonderInitiator);

      expect(utilService.openSnackbar).toHaveBeenCalledWith(
        "msg.initiator.ontkoppelen.uitgevoerd",
      );
      expect(readZaak).not.toHaveBeenCalled();
    });
  });

  describe("betrokkeneGeselecteerd", () => {
    it("closes the side action panel and couples the betrokkene in the chosen roltype", async () => {
      service.betrokkeneGeselecteerd(zaakMetInitiator, createKlantGegevens());

      const request = await pendingRequest("/rest/zaken/betrokkene");

      expect(sidenav.close).toHaveBeenCalled();
      expect(request.request.method).toBe("POST");
      expect(request.request.body).toEqual(
        expect.objectContaining({
          zaakUUID: "fakeZaakUuid",
          roltypeUUID: "fakeRoltypeUuid",
          roltoelichting: "fakeToelichting",
          betrokkeneIdentificatie: expect.objectContaining({
            type: "BSN",
            bsn: "fakeKlantBsn",
          }),
        }),
      );
    });

    it("reports the roltype and refreshes the betrokkenen", async () => {
      service.betrokkeneGeselecteerd(zaakMetInitiator, createKlantGegevens());
      (await pendingRequest("/rest/zaken/betrokkene")).flush(zaakMetInitiator);
      await settle();

      expect(utilService.openSnackbar).toHaveBeenCalledWith(
        "msg.betrokkene.gekoppeld",
        { roltype: "fakeRoltypeNaam" },
      );
      expect(invalidateSpy).toHaveBeenCalledWith({
        queryKey:
          zakenService.listBetrokkenenVoorZaakQuery("fakeZaakUuid").queryKey,
      });
    });

    it("refreshes the historie even though the zaak comes back exactly as it went in, because a rol is not part of it", async () => {
      service.betrokkeneGeselecteerd(zaakMetInitiator, createKlantGegevens());
      (await pendingRequest("/rest/zaken/betrokkene")).flush(zaakMetInitiator);
      await settle();

      expect(invalidateSpy).toHaveBeenCalledWith(
        {
          queryKey:
            zakenService.listHistorieVoorZaakQuery("fakeZaakUuid").queryKey,
        },
        { cancelRefetch: false },
      );
    });

    it("reports a failing coupling through the error handler, without announcing success", async () => {
      service.betrokkeneGeselecteerd(zaakMetInitiator, createKlantGegevens());
      (await pendingRequest("/rest/zaken/betrokkene")).flush(null, {
        status: 500,
        statusText: "Server Error",
      });
      await settle();

      expect(foutAfhandelen).toHaveBeenCalled();
      expect(utilService.openSnackbar).not.toHaveBeenCalled();
    });
  });

  describe("invalidateBetrokkenen", () => {
    it("invalidates the betrokkenen of the zaak it is given", () => {
      service.invalidateBetrokkenen(zaakMetInitiator);

      expect(invalidateSpy).toHaveBeenCalledWith({
        queryKey:
          zakenService.listBetrokkenenVoorZaakQuery("fakeZaakUuid").queryKey,
      });
    });
  });
});
