/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { MatDialogRef } from "@angular/material/dialog";
import { MatSidenav } from "@angular/material/sidenav";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { of, Subject } from "rxjs";
import { testQueryClient } from "../../../../../setupJest";
import { fromPartial } from "../../../../test-helpers";
import { UtilService } from "../../../core/service/util.service";
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

  beforeEach(() => {
    closed = new Subject<unknown>();

    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot()],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideQueryClient(testQueryClient),
        ZaakSideActionService,
        ZaakBetrokkenenService,
      ],
    });

    service = TestBed.inject(ZaakBetrokkenenService);
    sideActions = TestBed.inject(ZaakSideActionService);
    zakenService = TestBed.inject(ZakenService);
    zaakDialogService = TestBed.inject(ZaakDialogService);
    utilService = TestBed.inject(UtilService);

    sidenav = { open: jest.fn(), close: jest.fn() };
    sideActions.register(fromPartial<MatSidenav>(sidenav));

    jest.spyOn(utilService, "openSnackbar").mockImplementation();
    jest.spyOn(zakenService, "cacheZaak").mockImplementation();
    invalidateSpy = jest.spyOn(testQueryClient, "invalidateQueries");
  });

  describe("initiatorGeselecteerd", () => {
    it("closes the side action panel, because the choice has been made", () => {
      jest
        .spyOn(zakenService, "updateInitiator")
        .mockReturnValue(of(gekoppeldeZaak));

      service.initiatorGeselecteerd(zaakZonderInitiator, initiator);

      expect(sidenav.close).toHaveBeenCalled();
    });

    it("couples the initiator straight away when the zaak has none yet", () => {
      const updateInitiator = jest
        .spyOn(zakenService, "updateInitiator")
        .mockReturnValue(of(gekoppeldeZaak));

      service.initiatorGeselecteerd(zaakZonderInitiator, initiator);

      expect(updateInitiator).toHaveBeenCalledWith({
        zaakUUID: "fakeZaakUuid",
        betrokkeneIdentificatie: expect.objectContaining({
          type: "BSN",
          bsn: "fakeInitiatorBsn",
        }),
      });
    });

    it("reports the coupling and refreshes the historie when the zaak had no initiator", () => {
      jest
        .spyOn(zakenService, "updateInitiator")
        .mockReturnValue(of(gekoppeldeZaak));

      service.initiatorGeselecteerd(zaakZonderInitiator, initiator);

      expect(zakenService.cacheZaak).toHaveBeenCalledWith(gekoppeldeZaak);
      expect(utilService.openSnackbar).toHaveBeenCalledWith(
        "msg.initiator.gekoppeld",
        { naam: "fakeKvkNummer - fakeVestigingsnummer" },
      );
      expect(invalidateSpy).toHaveBeenCalledWith(
        {
          queryKey:
            zakenService.listHistorieVoorZaakQuery("fakeZaakUuid").queryKey,
        },
        { cancelRefetch: false },
      );
    });

    it("asks for a reason before replacing an initiator the zaak already has", () => {
      const openWijzigInitiator = jest
        .spyOn(zaakDialogService, "openWijzigInitiator")
        .mockImplementation(dialogRefClosingWithReden);
      const updateInitiator = jest.spyOn(zakenService, "updateInitiator");

      service.initiatorGeselecteerd(zaakMetInitiator, initiator);

      expect(openWijzigInitiator).toHaveBeenCalledWith(
        "fakeInitiatorNaam",
        expect.any(Function),
      );
      expect(updateInitiator).not.toHaveBeenCalled();
    });

    it("reports the change once the reason dialog has replaced the initiator", () => {
      jest
        .spyOn(zaakDialogService, "openWijzigInitiator")
        .mockImplementation(dialogRefClosingWithReden);

      service.initiatorGeselecteerd(zaakMetInitiator, initiator);
      closedWith(gekoppeldeZaak);

      expect(zakenService.cacheZaak).toHaveBeenCalledWith(gekoppeldeZaak);
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

    it("passes the reason the dialog collected on to the update", () => {
      jest
        .spyOn(zaakDialogService, "openWijzigInitiator")
        .mockImplementation(dialogRefClosingWithReden);
      const updateInitiator = jest
        .spyOn(zakenService, "updateInitiator")
        .mockReturnValue(of(gekoppeldeZaak));

      service.initiatorGeselecteerd(zaakMetInitiator, initiator);
      jest
        .mocked(zaakDialogService.openWijzigInitiator)
        .mock.calls.at(-1)![1]("fakeReden");

      expect(updateInitiator).toHaveBeenCalledWith({
        zaakUUID: "fakeZaakUuid",
        betrokkeneIdentificatie: expect.objectContaining({ type: "BSN" }),
        toelichting: "fakeReden",
      });
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

    it("reports the ontkoppelen and refetches the zaak when the dialog confirms", () => {
      const readZaak = jest
        .spyOn(zakenService, "readZaak")
        .mockReturnValue(of(zaakZonderInitiator));

      service.deleteInitiator(zaakMetInitiator);
      closedWith(true);

      expect(utilService.openSnackbar).toHaveBeenCalledWith(
        "msg.initiator.ontkoppelen.uitgevoerd",
      );
      expect(readZaak).toHaveBeenCalledWith("fakeZaakUuid");
      expect(zakenService.cacheZaak).toHaveBeenCalledWith(zaakZonderInitiator);
      expect(invalidateSpy).toHaveBeenCalledWith(
        {
          queryKey:
            zakenService.listHistorieVoorZaakQuery("fakeZaakUuid").queryKey,
        },
        { cancelRefetch: false },
      );
    });
  });

  describe("betrokkeneGeselecteerd", () => {
    it("closes the side action panel and couples the betrokkene in the chosen roltype", () => {
      const createBetrokkene = jest
        .spyOn(zakenService, "createBetrokkene")
        .mockReturnValue(of(zaakMetInitiator));

      service.betrokkeneGeselecteerd(zaakMetInitiator, createKlantGegevens());

      expect(sidenav.close).toHaveBeenCalled();
      expect(createBetrokkene).toHaveBeenCalledWith({
        zaakUUID: "fakeZaakUuid",
        roltypeUUID: "fakeRoltypeUuid",
        roltoelichting: "fakeToelichting",
        betrokkeneIdentificatie: expect.objectContaining({
          type: "BSN",
          bsn: "fakeKlantBsn",
        }),
      });
    });

    it("reports the roltype and refreshes both the historie and the betrokkenen", () => {
      jest
        .spyOn(zakenService, "createBetrokkene")
        .mockReturnValue(of(zaakMetInitiator));

      service.betrokkeneGeselecteerd(zaakMetInitiator, createKlantGegevens());

      expect(zakenService.cacheZaak).toHaveBeenCalledWith(zaakMetInitiator);
      expect(utilService.openSnackbar).toHaveBeenCalledWith(
        "msg.betrokkene.gekoppeld",
        { roltype: "fakeRoltypeNaam" },
      );
      expect(invalidateSpy).toHaveBeenCalledWith(
        {
          queryKey:
            zakenService.listHistorieVoorZaakQuery("fakeZaakUuid").queryKey,
        },
        { cancelRefetch: false },
      );
      expect(invalidateSpy).toHaveBeenCalledWith({
        queryKey:
          zakenService.listBetrokkenenVoorZaakQuery("fakeZaakUuid").queryKey,
      });
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
