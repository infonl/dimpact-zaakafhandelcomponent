/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient, withXhr } from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatDialogRef } from "@angular/material/dialog";
import { Router } from "@angular/router";
import { provideTranslateService } from "@ngx-translate/core";
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
  let openDetachSpy: jest.Mock;
  let deleteMedewerkerMutation: ReturnType<
    typeof createMutationOptions<void, { medewerkerId: string; reden: string }>
  >;

  const zaakUuid = "fake-zaak-uuid";
  const detachLabel =
    "actie.zaakspecifiek-geautoriseerde-medewerker.ontkoppelen";

  const behandelaar: GeneratedType<"RestGeautoriseerdeMedewerker"> = {
    id: "fake-behandelaar-id",
    naam: "Anna de Vries",
    roltype: "Behandelaar",
    isDetachable: false,
  };
  const addedMedewerker: GeneratedType<"RestGeautoriseerdeMedewerker"> = {
    id: "fake-medewerker-id",
    naam: "Emma Jansen",
    roltype: "Zaakspecifiek geautoriseerde medewerker",
    isDetachable: true,
  };

  const screen = () => within(fixture.nativeElement as HTMLElement);

  const renderTab = (
    medewerkers: GeneratedType<"RestGeautoriseerdeMedewerker">[],
    canDetach = false,
    loggedInUserId: string | null = "fake-logged-in-user-id",
  ) => {
    testQueryClient.setQueryData(
      zakenService.listGeautoriseerdeMedewerkersQuery(zaakUuid).queryKey,
      medewerkers,
    );
    if (loggedInUserId) {
      testQueryClient.setQueryData(
        TestBed.inject(IdentityService).readLoggedInUser().queryKey,
        fromPartial<GeneratedType<"RestLoggedInUser">>({
          username: loggedInUserId,
        }),
      );
    }
    fixture = TestBed.createComponent(ZaakDetailsMedewerkersTabComponent);
    fixture.componentRef.setInput("zaakUuid", zaakUuid);
    fixture.componentRef.setInput("canDetach", canDetach);
    fixture.detectChanges();
  };

  beforeEach(async () => {
    notifyManager.setScheduler((fn) => fn());
    dialogRef = fromPartial<MatDialogRef<unknown>>({
      afterClosed: jest.fn().mockReturnValue(of(false)),
    });
    openDetachSpy = jest.fn().mockReturnValue(dialogRef);

    await TestBed.configureTestingModule({
      imports: [ZaakDetailsMedewerkersTabComponent],
      providers: [
        provideTranslateService(),
        provideHttpClient(withXhr()),
        provideHttpClientTesting(),
        provideQueryClient(testQueryClient),
        {
          provide: ZaakDialogService,
          useValue: {
            openDetachZaakspecifiekGeautoriseerdeMedewerker: openDetachSpy,
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

  it("offers detaching only for the medewerkers the backend marks as detachable", () => {
    renderTab([behandelaar, addedMedewerker], true);

    const detachButtons = screen().getAllByRole("button", {
      name: detachLabel,
    });
    expect(detachButtons).toHaveLength(1);
    expect(
      within(screen().getAllByRole("row")[2]).getByRole("button", {
        name: detachLabel,
      }),
    ).toBe(detachButtons[0]);
  });

  it("offers no detaching until the logged-in user is known, so detaching yourself can never refresh the zaak", () => {
    renderTab([addedMedewerker], true, null);

    expect(
      screen().queryByRole("button", { name: detachLabel }),
    ).not.toBeInTheDocument();
  });

  it("offers no detaching to a user who may not change the zaak", () => {
    renderTab([behandelaar, addedMedewerker], false);

    expect(
      screen().queryByRole("button", { name: detachLabel }),
    ).not.toBeInTheDocument();
  });

  it("asks for a reden and detaches the medewerker with it", async () => {
    renderTab([addedMedewerker], true);

    screen().getByRole("button", { name: detachLabel }).click();
    const [naam, callback] = openDetachSpy.mock.calls[0] as [
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

  it("confirms the detachment once the dialog closes successfully and stays on the zaak", () => {
    jest.spyOn(dialogRef, "afterClosed").mockReturnValue(of(true));
    const navigateByUrl = jest.spyOn(TestBed.inject(Router), "navigateByUrl");
    renderTab([addedMedewerker], true);

    screen().getByRole("button", { name: detachLabel }).click();

    expect(utilService.openSnackbar).toHaveBeenCalledWith(
      "msg.zaakspecifiek-geautoriseerde-medewerker.ontkoppeld",
      { medewerker: "Emma Jansen" },
    );
    expect(navigateByUrl).not.toHaveBeenCalled();
  });

  it("goes to the dashboard after the user detaches themselves, since they may no longer see the zaak", async () => {
    jest.spyOn(dialogRef, "afterClosed").mockReturnValue(of(true));
    const navigateByUrl = jest
      .spyOn(TestBed.inject(Router), "navigateByUrl")
      .mockResolvedValue(true);
    renderTab([addedMedewerker], true, "fake-medewerker-id");

    screen().getByRole("button", { name: detachLabel }).click();
    const [, callback] = openDetachSpy.mock.calls[0] as [
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

    screen().getByRole("button", { name: detachLabel }).click();

    expect(utilService.openSnackbar).not.toHaveBeenCalled();
  });
});
