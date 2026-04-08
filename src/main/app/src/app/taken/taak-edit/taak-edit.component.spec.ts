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
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatButtonHarness } from "@angular/material/button/testing";
import { MatSidenav } from "@angular/material/sidenav";
import { MatToolbarHarness } from "@angular/material/toolbar/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { notifyManager } from "@tanstack/query-core";
import { of } from "rxjs";
import { sleep, testQueryClient } from "../../../../setupJest";
import { IdentityService } from "../../identity/identity.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { TakenService } from "../taken.service";
import { TaakEditComponent } from "./taak-edit.component";

const makeTask = (
  fields: Partial<GeneratedType<"RestTask">> = {},
): GeneratedType<"RestTask"> =>
  ({
    id: "task-1",
    zaakUuid: "zaak-uuid-1",
    zaaktypeUUID: "zaaktype-uuid-1",
    naam: "Test Task",
    status: "TOEGEKEND",
    groep: { id: "group-1", naam: "Group 1" },
    behandelaar: undefined,
    rechten: {
      lezen: true,
      toekennen: true,
      wijzigen: true,
      toevoegenDocument: true,
    },
    taakdata: {},
    tabellen: {},
    taakdocumenten: [],
    taakinformatie: {},
    formioFormulier: {},
    formulierDefinitieId: "DEFAULT_TAAKFORMULIER",
    creatiedatumTijd: new Date().toISOString(),
    toekenningsdatumTijd: new Date().toISOString(),
    fataledatum: new Date().toISOString(),
    zaaktypeOmschrijving: "Test Zaaktype",
    zaakIdentificatie: "ZAAK-001",
    toelichting: undefined,
    ...fields,
  }) as Partial<
    GeneratedType<"RestTask">
  > as unknown as GeneratedType<"RestTask">;

const makeGroup = (
  fields: Partial<GeneratedType<"RestGroup">> = {},
): GeneratedType<"RestGroup"> =>
  ({
    id: "group-1",
    naam: "Group 1",
    ...fields,
  }) as Partial<
    GeneratedType<"RestGroup">
  > as unknown as GeneratedType<"RestGroup">;

const makeUser = (
  fields: Partial<GeneratedType<"RestUser">> = {},
): GeneratedType<"RestUser"> =>
  ({
    id: "user-1",
    naam: "User 1",
    ...fields,
  }) as Partial<
    GeneratedType<"RestUser">
  > as unknown as GeneratedType<"RestUser">;

describe(TaakEditComponent.name, () => {
  let fixture: ComponentFixture<TaakEditComponent>;
  let component: TaakEditComponent;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  let sideNavSpy: Pick<MatSidenav, "close">;
  let identityService: Pick<
    IdentityService,
    "listBehandelaarGroupsForZaaktype" | "listUsersInGroup"
  >;

  beforeEach(() => {
    notifyManager.setScheduler((fn) => fn());

    sideNavSpy = { close: jest.fn().mockReturnValue(Promise.resolve(true)) };

    identityService = {
      listBehandelaarGroupsForZaaktype: jest
        .fn()
        .mockReturnValue(of([makeGroup()])),
      listUsersInGroup: jest.fn().mockReturnValue(of([makeUser()])),
    };

    TestBed.configureTestingModule({
      imports: [
        TaakEditComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [
        { provide: IdentityService, useValue: identityService },
        TakenService,
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideQueryClient(testQueryClient),
      ],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(TaakEditComponent);
    component = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);
  });

  afterEach(() => {
    notifyManager.setScheduler((fn) => setTimeout(fn, 0));
    httpTestingController.verify();
  });

  const setInputsAndDetect = (task: GeneratedType<"RestTask">) => {
    fixture.componentRef.setInput("task", task);
    fixture.componentRef.setInput("sideNav", sideNavSpy);
    fixture.detectChanges();
  };

  describe("toolbar", () => {
    it("renders the heading translation key", async () => {
      setInputsAndDetect(makeTask());
      const toolbar = await loader.getHarness(MatToolbarHarness);
      const text = await toolbar.host().then((h) => h.text());
      expect(text).toContain("actie.taak.wijzigen");
    });

    it("close button calls sideNav().close()", async () => {
      setInputsAndDetect(makeTask());
      const closeButton = await loader.getHarness(
        MatButtonHarness.with({ selector: "[mat-icon-button]" }),
      );
      await closeButton.click();
      expect(sideNavSpy.close).toHaveBeenCalledTimes(1);
    });
  });

  describe("form initialisation", () => {
    it("loads groups for the task zaaktype on init", () => {
      setInputsAndDetect(makeTask());
      expect(
        identityService.listBehandelaarGroupsForZaaktype,
      ).toHaveBeenCalledWith("zaaktype-uuid-1");
    });

    it("patches the group form control with the task groep", () => {
      const task = makeTask({
        groep: makeGroup({ id: "g-42", naam: "Groep 42" }),
      });
      setInputsAndDetect(task);
      expect(component["form"].value.groep).toEqual(
        expect.objectContaining({ id: "g-42" }),
      );
    });

    it("patches the behandelaar form control with the task behandelaar", () => {
      const behandelaar = makeUser({ id: "u-7", naam: "User Seven" });
      const task = makeTask({ behandelaar });
      setInputsAndDetect(task);
      expect(component["form"].value.behandelaar).toEqual(
        expect.objectContaining({ id: "u-7" }),
      );
    });

    it("unshifts the task group into groups list if it is absent from the loaded groups", () => {
      const absentGroup = makeGroup({
        id: "absent-group",
        naam: "Absent Group",
      });
      (
        identityService.listBehandelaarGroupsForZaaktype as jest.Mock
      ).mockReturnValue(of([makeGroup({ id: "other-group" })]));

      setInputsAndDetect(makeTask({ groep: absentGroup }));

      expect(component["groups"][0]).toEqual(
        expect.objectContaining({ id: "absent-group" }),
      );
    });

    it("does NOT unshift the task group when it is already in the loaded groups list", () => {
      const existingGroup = makeGroup({ id: "group-1" });
      (
        identityService.listBehandelaarGroupsForZaaktype as jest.Mock
      ).mockReturnValue(of([existingGroup]));

      setInputsAndDetect(makeTask({ groep: existingGroup }));

      const ids = component["groups"].map((g) => g.id);
      expect(ids.filter((id) => id === "group-1")).toHaveLength(1);
    });
  });

  describe("form disabling", () => {
    it("disables the form when the task status is AFGEROND", () => {
      setInputsAndDetect(makeTask({ status: "AFGEROND" }));
      expect(component["form"].disabled).toBe(true);
    });

    it("disables the form when rechten.toekennen is false", () => {
      setInputsAndDetect(
        makeTask({
          rechten: {
            lezen: true,
            toekennen: false,
            wijzigen: true,
            toevoegenDocument: true,
          },
        }),
      );
      expect(component["form"].disabled).toBe(true);
    });

    it("keeps the form enabled when the task is active and toekennen is true", () => {
      setInputsAndDetect(makeTask({ status: "TOEGEKEND" }));
      expect(component["form"].disabled).toBe(false);
    });
  });

  describe("group selection change", () => {
    it("resets behandelaar to null when group changes", () => {
      setInputsAndDetect(makeTask({ behandelaar: makeUser() }));

      component["form"].controls.groep.setValue(
        makeGroup({ id: "new-group", naam: "New Group" }),
      );
      fixture.detectChanges();

      expect(component["form"].value.behandelaar).toBeNull();
    });

    it("loads users for the newly selected group", () => {
      setInputsAndDetect(makeTask());

      (identityService.listUsersInGroup as jest.Mock).mockReturnValue(
        of([makeUser({ id: "u-99", naam: "User 99" })]),
      );

      component["form"].controls.groep.setValue(
        makeGroup({ id: "group-99", naam: "Group 99" }),
      );
      fixture.detectChanges();

      expect(identityService.listUsersInGroup).toHaveBeenCalledWith("group-99");
      expect(component["users"]).toEqual([
        expect.objectContaining({ id: "u-99" }),
      ]);
    });

    it("enables behandelaar control after users are loaded", () => {
      setInputsAndDetect(makeTask());

      component["form"].controls.groep.setValue(
        makeGroup({ id: "group-2", naam: "Group 2" }),
      );
      fixture.detectChanges();

      expect(component["form"].controls.behandelaar.enabled).toBe(true);
    });

    it("disables behandelaar control when group is cleared", () => {
      setInputsAndDetect(makeTask());

      component["form"].controls.groep.setValue(null);
      fixture.detectChanges();

      expect(component["form"].controls.behandelaar.disabled).toBe(true);
    });
  });

  describe("formSubmit", () => {
    it("sends a PATCH request to /rest/taken/toekennen with the correct payload", async () => {
      const task = makeTask({
        id: "t-1",
        zaakUuid: "z-1",
        groep: makeGroup({ id: "g-1" }),
        behandelaar: makeUser({ id: "u-1" }),
      });
      setInputsAndDetect(task);

      component["form"].controls.reden.setValue("test reden");
      component["form"].markAsDirty();
      fixture.detectChanges();

      component["formSubmit"]();
      await new Promise(requestAnimationFrame);

      const req = httpTestingController.expectOne("/rest/taken/toekennen");
      expect(req.request.method).toBe("PATCH");
      expect(req.request.body).toEqual(
        expect.objectContaining({
          taakId: "t-1",
          zaakUuid: "z-1",
          groepId: "g-1",
          behandelaarId: "u-1",
          reden: "test reden",
        }),
      );
      req.flush({});
    });

    it("sends undefined behandelaarId when no behandelaar is selected", async () => {
      const task = makeTask({
        id: "t-2",
        zaakUuid: "z-2",
        groep: makeGroup({ id: "g-2" }),
        behandelaar: undefined,
      });
      setInputsAndDetect(task);
      component["form"].controls.behandelaar.setValue(null);
      component["form"].markAsDirty();

      component["formSubmit"]();
      await new Promise(requestAnimationFrame);

      const req = httpTestingController.expectOne("/rest/taken/toekennen");
      expect(req.request.body).toEqual(
        expect.objectContaining({
          behandelaarId: undefined,
        }),
      );
      req.flush({});
    });

    it("closes the sidenav after a successful mutation", async () => {
      setInputsAndDetect(makeTask());
      component["form"].markAsDirty();

      component["formSubmit"]();
      await new Promise(requestAnimationFrame);

      const req = httpTestingController.expectOne("/rest/taken/toekennen");
      req.flush({});

      await sleep();

      expect(sideNavSpy.close).toHaveBeenCalledTimes(1);
    });
  });
});
