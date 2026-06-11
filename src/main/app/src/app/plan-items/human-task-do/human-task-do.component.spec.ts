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
import { AbstractControl, FormBuilder } from "@angular/forms";
import { MatFormFieldHarness } from "@angular/material/form-field/testing";
import { MatInputHarness } from "@angular/material/input/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { of } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../setupJest";
import { FoutAfhandelingService } from "../../fout-afhandeling/fout-afhandeling.service";
import { TaakFormulierenService } from "../../formulieren/taken/taak-formulieren.service";
import { IdentityService } from "../../identity/identity.service";
import { InformatieObjectenService } from "../../informatie-objecten/informatie-objecten.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { HumanTaskDoComponent } from "./human-task-do.component";

describe("HumanTaskDoComponent", () => {
  let component: HumanTaskDoComponent;
  let fixture: ComponentFixture<typeof component>;
  let loader: HarnessLoader;

  let identityService: IdentityService;
  let taakFormulierenService: TaakFormulierenService;
  let foutAfhandelingService: FoutAfhandelingService;
  let httpTestingController: HttpTestingController;

  // `form` is built up dynamically, so its controls are not statically typed
  function getFormControl(key: string) {
    return component["form"].get(key) as AbstractControl<unknown> | null;
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        HumanTaskDoComponent,
        TranslateModule.forRoot(),
        NoopAnimationsModule,
      ],
      providers: [
        FormBuilder,
        TaakFormulierenService,
        InformatieObjectenService,
        IdentityService,
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideQueryClient(testQueryClient),
      ],
    }).compileComponents();

    taakFormulierenService = TestBed.inject(TaakFormulierenService);
    foutAfhandelingService = TestBed.inject(FoutAfhandelingService);
    httpTestingController = TestBed.inject(HttpTestingController);

    identityService = TestBed.inject(IdentityService);
    jest
      .spyOn(identityService, "listBehandelaarGroupsForZaaktype")
      .mockReturnValue(
        of([
          fromPartial<GeneratedType<"RestGroup">>({
            id: "1",
            naam: "groep1",
          }),
        ]),
      );

    fixture = TestBed.createComponent(HumanTaskDoComponent);

    component = fixture.componentInstance;
    component.planItem = fromPartial<GeneratedType<"RESTPlanItem">>({
      type: "HUMAN_TASK",
      formulierDefinitie: "ADVIES",
    });
    component.zaak = fromPartial<GeneratedType<"RestZaak">>({
      zaaktype: {
        uuid: "test-zaaktype-uuid",
        omschrijving: "test-zaaktype-omschrijving",
      },
    });

    loader = TestbedHarnessEnvironment.loader(fixture);
  });

  describe("basic angular form", () => {
    beforeEach(() => {
      jest
        .spyOn(taakFormulierenService, "getAngularRequestFormBuilder")
        .mockResolvedValue([]);
    });

    it("should create the default form controls", async () => {
      await component.ngOnInit();

      const fields = await loader.getAllHarnesses(MatFormFieldHarness);
      expect(fields).toHaveLength(2); // `Group` and `User` inputs
    });

    it("should call listBehandelaarGroupsForZaaktype with the zaaktype omschrijving", async () => {
      await component.ngOnInit();

      expect(
        identityService.listBehandelaarGroupsForZaaktype,
      ).toHaveBeenCalledWith("test-zaaktype-omschrijving");
    });

    it("should pre-select the group and load users when planItem.groepId matches a group", async () => {
      const listUsersInGroupSpy = jest
        .spyOn(identityService, "listUsersInGroup")
        .mockReturnValue(of([]));
      component.planItem = fromPartial<GeneratedType<"RESTPlanItem">>({
        type: "HUMAN_TASK",
        formulierDefinitie: "ADVIES",
        groepId: "1",
      });

      await component.ngOnInit();

      expect(listUsersInGroupSpy).toHaveBeenCalledWith("1");
    });

    it("should prefill group control, populate user options, and enable user control when groepId matches", async () => {
      const mockUsers = [
        fromPartial<GeneratedType<"RestUser">>({ id: "u1", naam: "User One" }),
        fromPartial<GeneratedType<"RestUser">>({ id: "u2", naam: "User Two" }),
      ];
      jest
        .spyOn(identityService, "listUsersInGroup")
        .mockReturnValue(of(mockUsers));
      component.planItem = fromPartial<GeneratedType<"RESTPlanItem">>({
        type: "HUMAN_TASK",
        formulierDefinitie: "ADVIES",
        groepId: "1",
      });

      await component.ngOnInit();
      fixture.detectChanges();

      expect(component["form"].get("group")?.value).toEqual(
        fromPartial<GeneratedType<"RestGroup">>({ id: "1", naam: "groep1" }),
      );
      expect(component["form"].get("user")?.enabled).toBe(true);
      const userField = component["formFields"].find(
        (f) => f.type === "auto-complete" && f.key === "user",
      ) as { options?: unknown[] } | undefined;
      expect(userField?.options).toEqual(mockUsers);
    });

    it("should not pre-select a group when planItem.groepId is not set", async () => {
      const listUsersInGroupSpy = jest.spyOn(
        identityService,
        "listUsersInGroup",
      );
      component.planItem = fromPartial<GeneratedType<"RESTPlanItem">>({
        type: "HUMAN_TASK",
        formulierDefinitie: "ADVIES",
      });

      await component.ngOnInit();

      expect(listUsersInGroupSpy).not.toHaveBeenCalled();
    });

    it("should enable the user control and load its options when a group is selected", async () => {
      const mockUsers = [
        fromPartial<GeneratedType<"RestUser">>({ id: "u1", naam: "User One" }),
      ];
      jest
        .spyOn(identityService, "listUsersInGroup")
        .mockReturnValue(of(mockUsers));

      await component.ngOnInit();

      getFormControl("group")?.setValue(
        fromPartial<GeneratedType<"RestGroup">>({ id: "1", naam: "groep1" }),
      );

      expect(identityService.listUsersInGroup).toHaveBeenCalledWith("1");
      expect(component["form"].get("user")?.enabled).toBe(true);
      const userField = component["formFields"].find(
        (formField) =>
          formField.type === "auto-complete" && formField.key === "user",
      ) as { options?: unknown[] } | undefined;
      expect(userField?.options).toEqual(mockUsers);
    });

    it("should reset and disable the user control when the group is cleared", async () => {
      jest.spyOn(identityService, "listUsersInGroup").mockReturnValue(of([]));

      await component.ngOnInit();

      getFormControl("group")?.setValue(
        fromPartial<GeneratedType<"RestGroup">>({ id: "1", naam: "groep1" }),
      );
      getFormControl("group")?.setValue(null);

      expect(component["form"].get("user")?.disabled).toBe(true);
      expect(component["form"].get("user")?.value).toBeNull();
    });
  });

  describe("angular form with fields", () => {
    beforeEach(() => {
      jest
        .spyOn(taakFormulierenService, "getAngularRequestFormBuilder")
        .mockResolvedValue([{ type: "input", key: "question" }]);
    });

    it("should create the form controls", async () => {
      await component.ngOnInit();

      const input = await loader.getHarness(MatInputHarness);
      expect(input).not.toBeNull(); // `question` input
    });
  });

  describe("plan item that is not a human task", () => {
    it("should not build a form", async () => {
      const getAngularRequestFormBuilderSpy = jest.spyOn(
        taakFormulierenService,
        "getAngularRequestFormBuilder",
      );
      component.planItem = fromPartial<GeneratedType<"RESTPlanItem">>({
        type: "PROCESS_TASK",
      });

      await component.ngOnInit();

      expect(getAngularRequestFormBuilderSpy).not.toHaveBeenCalled();
      expect(component["formFields"]).toHaveLength(0);
    });
  });

  describe("submitting the form", () => {
    beforeEach(async () => {
      jest
        .spyOn(taakFormulierenService, "getAngularRequestFormBuilder")
        .mockResolvedValue([{ type: "input", key: "question" }]);
      jest
        .spyOn(foutAfhandelingService, "foutAfhandelen")
        .mockReturnValue(undefined as never);
      jest.spyOn(identityService, "listUsersInGroup").mockReturnValue(of([]));
      component.planItem = fromPartial<GeneratedType<"RESTPlanItem">>({
        id: "test-plan-item-id",
        type: "HUMAN_TASK",
        formulierDefinitie: "ADVIES",
      });

      await component.ngOnInit();
      getFormControl("question")?.setValue("test answer");
      getFormControl("group")?.setValue(
        fromPartial<GeneratedType<"RestGroup">>({ id: "1", naam: "groep1" }),
      );
    });

    it("should send the filled in task fields to the backend", async () => {
      component["onFormSubmit"](component["form"]);
      await sleep();

      const request = httpTestingController.expectOne(
        "/rest/planitems/doHumanTaskPlanItem",
      );
      expect(request.request.method).toBe("POST");
      expect(request.request.body).toEqual(
        expect.objectContaining({
          planItemInstanceId: "test-plan-item-id",
          groep: expect.objectContaining({ id: "1", naam: "groep1" }),
          taakdata: expect.objectContaining({ question: "test answer" }),
        }),
      );
      request.flush({});
    });

    it("should emit done when saving succeeds", async () => {
      const doneSpy = jest.spyOn(component.done, "emit");

      component["onFormSubmit"](component["form"]);
      await sleep();

      httpTestingController
        .expectOne("/rest/planitems/doHumanTaskPlanItem")
        .flush({});
      await sleep();

      expect(doneSpy).toHaveBeenCalled();
    });

    it("should show an error message and not emit done when saving fails", async () => {
      const doneSpy = jest.spyOn(component.done, "emit");

      component["onFormSubmit"](component["form"]);
      await sleep();

      httpTestingController
        .expectOne("/rest/planitems/doHumanTaskPlanItem")
        .flush("something went wrong", {
          status: 500,
          statusText: "Internal Server Error",
        });
      await sleep();

      expect(foutAfhandelingService.foutAfhandelen).toHaveBeenCalled();
      expect(doneSpy).not.toHaveBeenCalled();
    });

    it("should emit done without sending anything when no form group is provided", async () => {
      const doneSpy = jest.spyOn(component.done, "emit");

      component["onFormSubmit"](undefined);
      await sleep();

      httpTestingController.expectNone("/rest/planitems/doHumanTaskPlanItem");
      expect(doneSpy).toHaveBeenCalled();
    });
  });

  describe("cancelling the form", () => {
    it("should emit done", () => {
      const doneSpy = jest.spyOn(component.done, "emit");

      component["onFormCancel"]();

      expect(doneSpy).toHaveBeenCalled();
    });
  });
});
