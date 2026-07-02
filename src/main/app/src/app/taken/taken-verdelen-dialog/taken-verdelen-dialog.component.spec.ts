/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import {
  provideHttpClient,
  withInterceptorsFromDi,
} from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { provideZoneChangeDetection } from "@angular/core";
import { TestBed } from "@angular/core/testing";
import { MatButtonHarness } from "@angular/material/button/testing";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { MatToolbarHarness } from "@angular/material/toolbar/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import {
  mutationOptions,
  provideTanStackQuery,
} from "@tanstack/angular-query-experimental";
import { of } from "rxjs";
import { sleep, testQueryClient } from "../../../../setupJest";
import { IdentityService } from "../../identity/identity.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { TaakZoekObject } from "../../zoeken/model/taken/taak-zoek-object";
import { TakenVerdelenDialogComponent } from "./taken-verdelen-dialog.component";

const mockGroup: GeneratedType<"RestGroup"> = {
  id: "group-1",
  naam: "Test Group",
};

const mockUser: GeneratedType<"RestUser"> = {
  id: "user-1",
  naam: "Test User",
};

const makeTaak = (id: string): TaakZoekObject =>
  ({
    id,
    zaakUuid: `zaak-${id}`,
    zaaktypeOmschrijving: "fakeZaaktypeOmschrijving",
  }) as Partial<TaakZoekObject> as unknown as TaakZoekObject;

const makeDialogData = (
  taken: TaakZoekObject[],
  screenEventResourceId = "screen-event-1",
) => ({ taken, screenEventResourceId });

async function setup(
  data = makeDialogData([makeTaak("1"), makeTaak("2")]),
  groups: GeneratedType<"RestGroup">[] = [mockGroup],
) {
  const dialogRef = {
    close: jest.fn(),
  } as unknown as MatDialogRef<TakenVerdelenDialogComponent>;

  TestBed.resetTestingModule();
  await TestBed.configureTestingModule({
    imports: [
      TakenVerdelenDialogComponent,
      NoopAnimationsModule,
      TranslateModule.forRoot(),
    ],
    providers: [
      provideZoneChangeDetection(),
      provideHttpClient(withInterceptorsFromDi()),
      provideHttpClientTesting(),
      provideTanStackQuery(testQueryClient),
      { provide: MAT_DIALOG_DATA, useValue: data },
      { provide: MatDialogRef, useValue: dialogRef },
    ],
  }).compileComponents();

  const identityService = TestBed.inject(IdentityService);
  jest
    .spyOn(identityService, "listUsersInGroup")
    .mockReturnValue(of([mockUser]));
  jest
    .spyOn(identityService, "listBehandelaarGroupsForZaaktypesQuery")
    .mockReturnValue(
      mutationOptions({
        mutationKey: ["/rest/identity/behandelaar-groups"],
        mutationFn: async () => groups,
      }),
    );

  const fixture = TestBed.createComponent(TakenVerdelenDialogComponent);
  const component = fixture.componentInstance;
  const loader = TestbedHarnessEnvironment.loader(fixture);
  const httpTestingController = TestBed.inject(HttpTestingController);

  fixture.detectChanges();
  TestBed.flushEffects();

  if (data.taken.length > 0) {
    await sleep(); // mutation microtask fires → dispatch success → flush setTimeout scheduled
    await sleep(); // flush setTimeout fires → resultFromSubscriberSignal updated
    fixture.detectChanges();
  }

  return { fixture, component, loader, httpTestingController, dialogRef };
}

describe(TakenVerdelenDialogComponent.name, () => {
  describe("with multiple taken", () => {
    let component: TakenVerdelenDialogComponent;
    let loader: HarnessLoader;
    let httpTestingController: HttpTestingController;
    let dialogRef: MatDialogRef<TakenVerdelenDialogComponent>;

    beforeEach(async () => {
      ({ component, loader, httpTestingController, dialogRef } = await setup(
        makeDialogData([makeTaak("1"), makeTaak("2")]),
      ));
    });

    it("should show plural title with task count", async () => {
      const toolbar = await loader.getHarness(MatToolbarHarness);
      expect(await (await toolbar.host()).text()).toContain(
        "msg.verdelen.taken",
      );
    });

    it("should close dialog with false when close button is clicked", () => {
      component["close"]();
      expect(dialogRef.close).toHaveBeenCalledWith(false);
    });

    it("should send the form data on request", async () => {
      component["form"].patchValue({
        groep: mockGroup,
        medewerker: mockUser,
        reden: "test-reden",
      });
      component["form"].markAsDirty();

      const button = await loader.getHarness(
        MatButtonHarness.with({ text: /actie.verdelen/i }),
      );
      const clickPromise = button.click();
      await sleep();

      const req = httpTestingController.expectOne("/rest/taken/lijst/verdelen");
      expect(req.request.method).toBe("PUT");
      expect(req.request.body).toEqual({
        taken: [
          { zaakUuid: "zaak-1", taakId: "1" },
          { zaakUuid: "zaak-2", taakId: "2" },
        ],
        groepId: mockGroup.id,
        behandelaarGebruikersnaam: mockUser.id,
        reden: "test-reden",
        screenEventResourceId: "screen-event-1",
      });
      req.flush({});
      await clickPromise;
    });

    it("should close the dialog with form data after successful mutation", async () => {
      const formData = {
        groep: mockGroup,
        medewerker: mockUser,
        reden: "test-reden",
      };

      component["form"].patchValue(formData);
      component["form"].markAsDirty();

      component["verdeel"]();
      await sleep();

      httpTestingController.expectOne("/rest/taken/lijst/verdelen").flush({});
      await sleep();

      expect(dialogRef.close).toHaveBeenCalledWith(formData);
    });
  });

  describe("with single taak", () => {
    let loader: HarnessLoader;

    beforeEach(async () => {
      ({ loader } = await setup(makeDialogData([makeTaak("1")])));
    });

    it("should show singular title", async () => {
      const toolbar = await loader.getHarness(MatToolbarHarness);
      expect(await (await toolbar.host()).text()).toContain(
        "msg.verdelen.taak",
      );
    });
  });

  describe("with no taken", () => {
    let component: TakenVerdelenDialogComponent;

    beforeEach(async () => {
      ({ component } = await setup(makeDialogData([])));
    });

    it("should disable form when taken list is empty", () => {
      expect(component["form"].disabled).toBe(true);
    });
  });

  describe("when no authorised groups are found", () => {
    it("noAuthorisedGroups signal is true", async () => {
      const { component } = await setup(makeDialogData([makeTaak("1")]), []);
      console.debug("GROEPEN: ", component["groupsQuery"].data())
      console.debug("AUthorizedGrups: ", component["noAuthorisedGroups"]())

      expect(component["noAuthorisedGroups"]()).toBe(true);
    });

    it("shows no authorised groups message", async () => {
      const { fixture } = await setup(makeDialogData([makeTaak("1")]), []);
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain(
        "msg.error.dialog.taken-verdelen.no-authorised-groups",
      );
    });
  });
});
