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
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { MatToolbarHarness } from "@angular/material/toolbar/testing";
import { By } from "@angular/platform-browser";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideTranslateService } from "@ngx-translate/core";
import {
  mutationOptions,
  provideTanStackQuery,
} from "@tanstack/angular-query-experimental";
import { screen } from "@testing-library/angular";
import { userEvent } from "@testing-library/user-event";
import { fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../setupJest";
import { IdentityService } from "../../identity/identity.service";
import { ZacAutoComplete } from "../../shared/form/auto-complete/auto-complete";
import { GeneratedType } from "../../shared/utils/generated-types";
import { TaakZoekObject } from "../../zoeken/model/taken/taak-zoek-object";
import { TakenVerdelenDialogComponent } from "./taken-verdelen-dialog.component";

const mockGroup: GeneratedType<"RestGroup"> = {
  id: "group-1",
  naam: "Test Group",
};

const mockUser: GeneratedType<"RestUser"> = {
  username: "user-1",
  naam: "Test User",
};

const makeTaak = (id: string): TaakZoekObject =>
  fromPartial<TaakZoekObject>({
    id,
    zaakUuid: `zaak-${id}`,
    zaaktypeOmschrijving: "fakeZaaktypeOmschrijving",
  });

const makeDialogData = (
  taken: TaakZoekObject[],
  screenEventResourceId = "screen-event-1",
) => ({ taken, screenEventResourceId });

async function setup(
  data = makeDialogData([makeTaak("1"), makeTaak("2")]),
  groups: GeneratedType<"RestGroup">[] = [mockGroup],
) {
  const dialogRef = fromPartial<MatDialogRef<TakenVerdelenDialogComponent>>({
    close: jest.fn(),
  });

  TestBed.resetTestingModule();
  await TestBed.configureTestingModule({
    imports: [TakenVerdelenDialogComponent, NoopAnimationsModule],
    providers: [
      provideTranslateService(),
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
    let fixture: ComponentFixture<TakenVerdelenDialogComponent>;
    let component: TakenVerdelenDialogComponent;
    let loader: HarnessLoader;
    let httpTestingController: HttpTestingController;
    let dialogRef: MatDialogRef<TakenVerdelenDialogComponent>;

    beforeEach(async () => {
      ({ fixture, component, loader, httpTestingController, dialogRef } =
        await setup(makeDialogData([makeTaak("1"), makeTaak("2")])));
    });

    async function pickGroup(group: unknown) {
      component["form"].controls.groep.setValue(
        group as GeneratedType<"RestGroup">,
      );
      fixture.detectChanges();
      await sleep();
    }

    async function respondWithUsers(users: GeneratedType<"RestUser">[]) {
      httpTestingController
        .expectOne(`/rest/identity/groups/${mockGroup.id}/users`)
        .flush(users);
      await sleep();
      fixture.detectChanges();
      await sleep();
      fixture.detectChanges();
    }

    async function fillInForm() {
      await pickGroup(mockGroup);
      await respondWithUsers([mockUser]);
      component["form"].patchValue({
        medewerker: mockUser,
        reden: "test-reden",
      });
      component["form"].markAsDirty();
      fixture.detectChanges();
    }

    it("should not let the user pick a medewerker before a groep is picked", () => {
      expect(component["form"].controls.medewerker.disabled).toBe(true);
    });

    it("should offer the users of the picked groep as medewerker", async () => {
      await pickGroup(mockGroup);
      await respondWithUsers([mockUser]);

      expect(component["form"].controls.medewerker.enabled).toBe(true);
      expect(component["users"]()).toEqual([mockUser]);
    });

    it("should not read users while the user types in the groep field", async () => {
      await pickGroup("Test");

      httpTestingController.expectNone((request) =>
        request.url.endsWith("/users"),
      );
      expect(component["form"].controls.medewerker.disabled).toBe(true);
    });

    it("should keep the picked medewerker when the users of the groep are read again", async () => {
      await fillInForm();

      void testQueryClient.invalidateQueries({
        queryKey: ["/rest/identity/groups/{groupId}/users"],
      });
      await sleep();
      await respondWithUsers([
        mockUser,
        { username: "user-2", naam: "Other User" },
      ]);

      expect(component["form"].controls.medewerker.value).toEqual(mockUser);
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
      await fillInForm();

      const user = userEvent.setup();
      await user.click(screen.getByRole("button", { name: /actie.verdelen/i }));
      await sleep();

      const req = httpTestingController.expectOne("/rest/taken/lijst/verdelen");
      expect(req.request.method).toBe("PUT");
      expect(req.request.body).toEqual({
        taken: [
          { zaakUuid: "zaak-1", taakId: "1" },
          { zaakUuid: "zaak-2", taakId: "2" },
        ],
        groepId: mockGroup.id,
        behandelaarGebruikersnaam: mockUser.username,
        reden: "test-reden",
        screenEventResourceId: "screen-event-1",
      });
      req.flush({});
    });

    it("should close the dialog with form data after successful mutation", async () => {
      await fillInForm();

      component["verdeel"]();
      await sleep();

      httpTestingController.expectOne("/rest/taken/lijst/verdelen").flush({});
      await sleep();

      expect(dialogRef.close).toHaveBeenCalledWith({
        groep: mockGroup,
        medewerker: mockUser,
        reden: "test-reden",
      });
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
      expect(component["noAuthorisedGroups"]()).toBe(true);
    });

    it("shows no authorised groups message", async () => {
      const { fixture } = await setup(makeDialogData([makeTaak("1")]), []);
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain(
        "msg.error.dialog.taken-verdelen.no-authorised-groups",
      );
    });

    it("sets the groep autocomplete to readonly", async () => {
      const { fixture } = await setup(makeDialogData([makeTaak("1")]), []);
      fixture.detectChanges();

      const groepAutoComplete = fixture.debugElement
        .queryAll(By.directive(ZacAutoComplete))
        .find((el) => el.componentInstance.key() === "groep");

      expect(groepAutoComplete).toBeDefined();
      expect(groepAutoComplete!.componentInstance.readonly()).toBe(true);
    });
  });
});
