/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ComponentFixture, TestBed } from "@angular/core/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import {
  ActivatedRoute,
  convertToParamMap,
  provideRouter,
  Router,
} from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { of } from "rxjs";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { UtilService } from "../../core/service/util.service";
import { SharedModule } from "../../shared/shared.module";
import { BpmnService } from "../bpmn.service";
import { FormioBuilderStylesService } from "./formio-builder-styles.service";
import { FormulierBuilderComponent } from "./formulier-builder.component";

const PROCESS_DEFINITION_KEY = "aanvraagBehandelen";
const FORM_KEY = "aanvullendeInformatie";

type SetupOptions = {
  isEditingExistingForm?: boolean;
  storedContent?: string;
};

describe(FormulierBuilderComponent.name, () => {
  let fixture: ComponentFixture<FormulierBuilderComponent>;
  let component: FormulierBuilderComponent;
  let bpmnService: jest.Mocked<BpmnService>;
  let utilService: jest.Mocked<UtilService>;
  let navigateSpy: jest.SpyInstance;

  async function setup({
    isEditingExistingForm = false,
    storedContent = JSON.stringify({
      display: "form",
      name: FORM_KEY,
      title: "Aanvullende informatie",
      components: [{ type: "button", key: "submit", label: "Afronden" }],
    }),
  }: SetupOptions = {}) {
    await TestBed.configureTestingModule({
      imports: [
        FormulierBuilderComponent,
        SharedModule,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap({
                processDefinitionKey: PROCESS_DEFINITION_KEY,
                formKey: FORM_KEY,
              }),
              queryParamMap: convertToParamMap(
                isEditingExistingForm ? { bewerken: "true" } : {},
              ),
            },
          },
        },
        {
          provide: BpmnService,
          useValue: {
            readProcessDefinitionForm: jest
              .fn()
              .mockReturnValue(
                of({ filename: `${FORM_KEY}.json`, content: storedContent }),
              ),
            uploadProcessDefinitionForm: jest.fn().mockReturnValue(of(null)),
          },
        },
        {
          provide: UtilService,
          useValue: { setTitle: jest.fn(), openSnackbar: jest.fn() },
        },
        { provide: ConfiguratieService, useValue: {} },
        {
          provide: FormioBuilderStylesService,
          useValue: {
            // never resolves, so the Form.io builder itself stays out of these tests
            link: jest.fn().mockReturnValue(new Promise<void[]>(() => {})),
            unlink: jest.fn(),
          },
        },
      ],
    }).compileComponents();

    bpmnService = TestBed.inject(BpmnService) as jest.Mocked<BpmnService>;
    utilService = TestBed.inject(UtilService) as jest.Mocked<UtilService>;
    navigateSpy = jest
      .spyOn(TestBed.inject(Router), "navigate")
      .mockResolvedValue(true);

    fixture = TestBed.createComponent(FormulierBuilderComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  }

  function savedForm() {
    const [, body] = bpmnService.uploadProcessDefinitionForm.mock.calls[0];
    return JSON.parse(body.content) as {
      name?: string;
      title?: string;
      components?: { key?: string }[];
    };
  }

  describe("a form key that has no form yet", () => {
    it("should not read a stored form", async () => {
      await setup();

      expect(bpmnService.readProcessDefinitionForm).not.toHaveBeenCalled();
    });

    it("should start from a form that already holds a button, so the task can be completed", async () => {
      await setup();

      component["opslaan"]();

      expect(savedForm().components).toEqual(
        expect.arrayContaining([expect.objectContaining({ key: "submit" })]),
      );
    });

    it("should report no issues for the starting point", async () => {
      await setup();

      expect(component["issues"]()).toEqual([]);
    });
  });

  describe("saving", () => {
    it("should store the form under the form key from the route", async () => {
      await setup();

      component["opslaan"]();

      expect(bpmnService.uploadProcessDefinitionForm).toHaveBeenCalledWith(
        PROCESS_DEFINITION_KEY,
        expect.objectContaining({ filename: `${FORM_KEY}.json` }),
      );
      expect(savedForm().name).toBe(FORM_KEY);
    });

    it("should keep the form key even when the form was edited to hold another name", async () => {
      await setup();
      component["form"].name = "iets-anders";

      component["opslaan"]();

      expect(savedForm().name).toBe(FORM_KEY);
    });

    it("should report the saved form and return to its process definition", async () => {
      await setup();

      component["opslaan"]();

      expect(utilService.openSnackbar).toHaveBeenCalledWith(
        "msg.bpmn.task-forms.upload.success",
        { namen: FORM_KEY },
      );
      expect(navigateSpy).toHaveBeenCalledWith(
        ["/admin/bpmn-procesdefinities"],
        { queryParams: { key: PROCESS_DEFINITION_KEY } },
      );
    });
  });

  describe("cancelling", () => {
    it("should return to the process definition without saving", async () => {
      await setup();

      component["annuleren"]();

      expect(bpmnService.uploadProcessDefinitionForm).not.toHaveBeenCalled();
      expect(navigateSpy).toHaveBeenCalledWith(
        ["/admin/bpmn-procesdefinities"],
        { queryParams: { key: PROCESS_DEFINITION_KEY } },
      );
    });
  });

  describe("a form key that already has a form", () => {
    it("should load the stored form", async () => {
      await setup({ isEditingExistingForm: true });

      expect(bpmnService.readProcessDefinitionForm).toHaveBeenCalledWith(
        PROCESS_DEFINITION_KEY,
        FORM_KEY,
      );
      expect(component["form"].title).toBe("Aanvullende informatie");
    });

    it("should report a stored form that cannot complete its task", async () => {
      await setup({
        isEditingExistingForm: true,
        storedContent: JSON.stringify({
          display: "form",
          name: FORM_KEY,
          components: [{ type: "textfield", key: "opmerking" }],
        }),
      });

      expect(component["issues"]()).toEqual([
        { messageKey: "msg.bpmn.task-forms.issue.no-button" },
      ]);
    });

    it("should load a form holding a textarea, whose 'rows' is a line count rather than nested cells", async () => {
      await setup({
        isEditingExistingForm: true,
        storedContent: JSON.stringify({
          display: "form",
          name: FORM_KEY,
          title: "Aanvullende informatie",
          components: [
            { type: "textarea", key: "toelichtingVeld", rows: 3 },
            { type: "button", key: "submit" },
          ],
        }),
      });

      expect(component["isFormLoaded"]()).toBe(true);
      expect(component["form"].title).toBe("Aanvullende informatie");
    });

    it("should override a stored name that drifted from the form key", async () => {
      await setup({
        isEditingExistingForm: true,
        storedContent: JSON.stringify({
          display: "form",
          name: "verouderde-naam",
          components: [{ type: "button", key: "submit" }],
        }),
      });

      expect(component["form"].name).toBe(FORM_KEY);
    });
  });
});
