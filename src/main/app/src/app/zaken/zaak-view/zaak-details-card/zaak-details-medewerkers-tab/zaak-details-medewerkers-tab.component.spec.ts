/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatDialogRef } from "@angular/material/dialog";
import { Router } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { notifyManager } from "@tanstack/query-core";
import { within } from "@testing-library/angular";
import { Observable, of } from "rxjs";
import { createMutationOptions, fromPartial } from "src/test-helpers";
import { testQueryClient } from "../../../../../../setupJest";
import { UtilService } from "../../../../core/service/util.service";
import { IdentityService } from "../../../../identity/identity.service";
import { GeneratedType } from "../../../../shared/utils/generated-types";
import { ZaakDialogService } from "../../../zaak-dialog.service";
import { ZakenService } from "../../../zaken.service";
import { ZaakDetailsMedewerkersTabComponent } from "./zaak-details-medewerkers-tab.component";

describe(ZaakDetailsMedewerkersTabComponent.name, () => {
  let fixture: ComponentFixture<ZaakDetailsMedewerkersTabComponent>;
  let zakenService: ZakenService;
  let utilService: UtilService;
  let dialogRef: MatDialogRef<unknown>;
  let openOntkoppelSpy: jest.Mock;
  let deleteMedewerkerMutation: ReturnType<
    typeof createMutationOptions<void, { medewerkerId: string; reden: string }>
  >;

  const zaakUuid = "fake-zaak-uuid";
  const ontkoppelLabel =
    "actie.zaakspecifiek-geautoriseerde-medewerker.ontkoppelen";

  const behandelaar: GeneratedType<"RestGeautoriseerdeMedewerker"> = {
    id: "fake-behandelaar-id",
    naam: "Anna de Vries",
    roltype: "Behandelaar",
    isOntkoppelbaar: false,
  };
  const addedMedewerker: GeneratedType<"RestGeautoriseerdeMedewerker"> = {
    id: "fake-medewerker-id",
    naam: "Emma Jansen",
    roltype: "Zaakspecifiek geautoriseerde medewerker",
    isOntkoppelbaar: true,
  };

  const screen = () => within(fixture.nativeElement as HTMLElement);

  const renderTab = (
    medewerkers: GeneratedType<"RestGeautoriseerdeMedewerker">[],
    canOntkoppelen = false,
  ) => {
    testQueryClient.setQueryData(
      zakenService.listGeautoriseerdeMedewerkersQuery(zaakUuid).queryKey,
      medewerkers,
    );
    fixture = TestBed.createComponent(ZaakDetailsMedewerkersTabComponent);
    fixture.componentRef.setInput("zaakUuid", zaakUuid);
    fixture.componentRef.setInput("canOntkoppelen", canOntkoppelen);
    fixture.detectChanges();
  };

  beforeEach(async () => {
    notifyManager.setScheduler((fn) => fn());
    dialogRef = fromPartial<MatDialogRef<unknown>>({
      afterClosed: jest.fn().mockReturnValue(of(false)),
    });
    openOntkoppelSpy = jest.fn().mockReturnValue(dialogRef);

    await TestBed.configureTestingModule({
      imports: [ZaakDetailsMedewerkersTabComponent, TranslateModule.forRoot()],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideQueryClient(testQueryClient),
        {
          provide: ZaakDialogService,
          useValue: {
            openOntkoppelZaakspecifiekGeautoriseerdeMedewerker:
              openOntkoppelSpy,
          },
        },
      ],
    }).compileComponents();

    zakenService = TestBed.inject(ZakenService);
    utilService = TestBed.inject(UtilService);
    jest.spyOn(utilService, "openSnackbar").mockImplementation();
    deleteMedewerkerMutation = createMutationOptions<
      void,
      { medewerkerId: string; reden: string }
    >(undefined);
    jest
      .spyOn(zakenService, "deleteZaakspecifiekGeautoriseerdeMedewerker")
      .mockReturnValue(deleteMedewerkerMutation as never);
  });

  afterEach(() => {
    notifyManager.setScheduler(queueMicrotask);
    testQueryClient.clear();
  });

  it("shows each medewerker with access to the zaak together with their rol, in the order the backend sorted them", () => {
    renderTab([behandelaar, addedMedewerker]);

    const rows = screen().getAllByRole("row").slice(1);
    expect(
      rows.map((row) => row.textContent?.replace(/\s+/g, " ").trim()),
    ).toEqual([
      "Anna de Vries Behandelaar",
      "Emma Jansen Zaakspecifiek geautoriseerde medewerker",
    ]);
  });

  it("labels the columns with the naam and the rol", () => {
    renderTab([]);

    expect(
      screen().getByRole("columnheader", { name: "naam" }),
    ).toBeInTheDocument();
    expect(
      screen().getByRole("columnheader", { name: "rol" }),
    ).toBeInTheDocument();
  });

  it("tells the user that no medewerkers were found when nobody holds a rol on the zaak", () => {
    renderTab([]);

    expect(
      screen().getByRole("cell", { name: "msg.geen.gegevens.gevonden" }),
    ).toBeInTheDocument();
  });

  it("shows a placeholder for a medewerker whose name is unknown", () => {
    renderTab([
      { id: "fake-behandelaar-id", naam: null, roltype: "Behandelaar" },
    ]);

    expect(screen().getByRole("cell", { name: "-" })).toBeInTheDocument();
  });

  it("offers ontkoppelen only for the medewerkers the backend marks as ontkoppelbaar", () => {
    renderTab([behandelaar, addedMedewerker], true);

    const ontkoppelButtons = screen().getAllByRole("button", {
      name: ontkoppelLabel,
    });
    expect(ontkoppelButtons).toHaveLength(1);
    expect(
      within(screen().getAllByRole("row")[2]).getByRole("button", {
        name: ontkoppelLabel,
      }),
    ).toBe(ontkoppelButtons[0]);
  });

  it("offers no ontkoppelen for a medewerker without an id, since the backend needs it to ontkoppel", () => {
    renderTab([{ ...addedMedewerker, id: null }], true);

    expect(
      screen().queryByRole("button", { name: ontkoppelLabel }),
    ).not.toBeInTheDocument();
  });

  it("offers no ontkoppelen to a user who may not change the zaak", () => {
    renderTab([behandelaar, addedMedewerker], false);

    expect(
      screen().queryByRole("button", { name: ontkoppelLabel }),
    ).not.toBeInTheDocument();
  });

  it("asks for a reden and ontkoppelt the medewerker with it", async () => {
    renderTab([addedMedewerker], true);

    screen().getByRole("button", { name: ontkoppelLabel }).click();
    const [naam, callback] = openOntkoppelSpy.mock.calls[0] as [
      string,
      (reden: string) => Observable<unknown>,
    ];
    callback("fake-reden").subscribe();
    await new Promise(requestAnimationFrame);

    expect(naam).toBe("Emma Jansen");
    expect(
      zakenService.deleteZaakspecifiekGeautoriseerdeMedewerker,
    ).toHaveBeenCalledWith(zaakUuid);
    expect(deleteMedewerkerMutation.mutationFn).toHaveBeenCalledWith(
      {
        medewerkerId: "fake-medewerker-id",
        reden: "fake-reden",
        isLoggedInUser: false,
      },
      expect.anything(),
    );
  });

  it("confirms the ontkoppeling once the dialog closes successfully and stays on the zaak", () => {
    jest.spyOn(dialogRef, "afterClosed").mockReturnValue(of(true));
    const navigateByUrl = jest.spyOn(TestBed.inject(Router), "navigateByUrl");
    renderTab([addedMedewerker], true);

    screen().getByRole("button", { name: ontkoppelLabel }).click();

    expect(utilService.openSnackbar).toHaveBeenCalledWith(
      "msg.zaakspecifiek-geautoriseerde-medewerker.ontkoppeld",
      { medewerker: "Emma Jansen" },
    );
    expect(navigateByUrl).not.toHaveBeenCalled();
  });

  it("goes to the dashboard after the user ontkoppelt themselves, since they may no longer see the zaak", async () => {
    jest.spyOn(dialogRef, "afterClosed").mockReturnValue(of(true));
    const navigateByUrl = jest
      .spyOn(TestBed.inject(Router), "navigateByUrl")
      .mockResolvedValue(true);
    testQueryClient.setQueryData(
      TestBed.inject(IdentityService).readLoggedInUser().queryKey,
      fromPartial<GeneratedType<"RestLoggedInUser">>({
        id: "fake-medewerker-id",
      }),
    );
    renderTab([addedMedewerker], true);

    screen().getByRole("button", { name: ontkoppelLabel }).click();
    const [, callback] = openOntkoppelSpy.mock.calls[0] as [
      string,
      (reden: string) => Observable<unknown>,
    ];
    callback("fake-reden").subscribe();
    await new Promise(requestAnimationFrame);

    expect(deleteMedewerkerMutation.mutationFn).toHaveBeenCalledWith(
      expect.objectContaining({ isLoggedInUser: true }),
      expect.anything(),
    );
    expect(navigateByUrl).toHaveBeenCalledWith("/");
  });

  it("shows no confirmation when the dialog is cancelled", () => {
    renderTab([addedMedewerker], true);

    screen().getByRole("button", { name: ontkoppelLabel }).click();

    expect(utilService.openSnackbar).not.toHaveBeenCalled();
  });
});
