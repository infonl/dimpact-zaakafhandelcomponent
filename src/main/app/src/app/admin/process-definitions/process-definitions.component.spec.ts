/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, EventEmitter, Input, Output } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatDialog } from "@angular/material/dialog";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { fromPartial } from "@total-typescript/shoehorn";
import { of } from "rxjs";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { UtilService } from "../../core/service/util.service";
import { FoutAfhandelingService } from "../../fout-afhandeling/fout-afhandeling.service";
import { SharedModule } from "../../shared/shared.module";
import { GeneratedType } from "../../shared/utils/generated-types";
import { BpmnService } from "../bpmn.service";
import { readFileContent } from "./file.helper";
import { ProcessDefinitionItemComponent } from "./process-definition-item/process-definition-item.component";
import { ProcessDefinitionsComponent } from "./process-definitions.component";

jest.mock("./file.helper");

@Component({
  selector: "zac-process-definition-item",
  template: "",
})
class ProcessDefinitionItemStubComponent {
  @Input() processDefinition!: unknown;
  @Output() bpmnFormListChanged = new EventEmitter<void>();
}

const baseProcessDefinition = fromPartial<
  GeneratedType<"RestBpmnProcessDefinition">
>({
  id: "pd-1",
  key: "key-a",
  name: "Process A",
  version: 1,
  details: {
    inUse: false,
    forms: [{ formKey: "f1", title: "Form 1", uploaded: true }],
    orphanedForms: [],
  },
});

describe(ProcessDefinitionsComponent.name, () => {
  let fixture: ComponentFixture<ProcessDefinitionsComponent>;
  let component: ProcessDefinitionsComponent;
  let bpmnService: jest.Mocked<BpmnService>;
  let utilService: jest.Mocked<UtilService>;
  let foutAfhandelingService: jest.Mocked<FoutAfhandelingService>;
  let dialogOpenSpy: jest.SpyInstance;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        ProcessDefinitionsComponent,
        SharedModule,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [
        provideRouter([]),
        {
          provide: BpmnService,
          useValue: {
            listProcessDefinitions: jest
              .fn()
              .mockReturnValue(of([baseProcessDefinition])),
            uploadProcessDefinition: jest.fn().mockReturnValue(of({})),
            deleteProcessDefinition: jest.fn().mockReturnValue(of({})),
          },
        },
        {
          provide: UtilService,
          useValue: {
            setLoading: jest.fn(),
            setTitle: jest.fn(),
            openSnackbar: jest.fn(),
          },
        },
        {
          provide: ConfiguratieService,
          useValue: {},
        },
        {
          provide: FoutAfhandelingService,
          useValue: { foutAfhandelen: jest.fn() },
        },
      ],
    })
      .overrideComponent(ProcessDefinitionsComponent, {
        remove: { imports: [ProcessDefinitionItemComponent] },
        add: { imports: [ProcessDefinitionItemStubComponent] },
      })
      .compileComponents();

    bpmnService = TestBed.inject(BpmnService) as jest.Mocked<BpmnService>;
    utilService = TestBed.inject(UtilService) as jest.Mocked<UtilService>;
    foutAfhandelingService = TestBed.inject(
      FoutAfhandelingService,
    ) as jest.Mocked<FoutAfhandelingService>;

    fixture = TestBed.createComponent(ProcessDefinitionsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();

    const internalDialog = (component as unknown as { dialog: MatDialog })
      .dialog;
    dialogOpenSpy = jest
      .spyOn(internalDialog, "open")
      .mockReturnValue({ afterClosed: () => of(false) } as never);
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  describe("on init", () => {
    it("should call listProcessDefinitions", () => {
      expect(bpmnService.listProcessDefinitions).toHaveBeenCalled();
    });

    it("should render a group row per process definition", () => {
      const rows: NodeList =
        fixture.nativeElement.querySelectorAll(".tree-group-row");
      expect(rows.length).toBe(1);
    });

    it("should show the process definition name", () => {
      expect(fixture.nativeElement.textContent).toContain("Process A");
    });

    it("should show empty-state message when there are no definitions", async () => {
      bpmnService.listProcessDefinitions.mockReturnValue(of([]));
      component["loadBpmnProcessDefinitions"]();
      fixture.detectChanges();
      await fixture.whenStable();

      expect(fixture.nativeElement.textContent).toContain(
        "msg.geen.gegevens.gevonden",
      );
    });
  });

  describe("toggleNode", () => {
    it("should expand a node on first click", () => {
      fixture.nativeElement.querySelector(".tree-group-row").click();
      expect(component["expandedKey"]).toBe("key-a");
    });

    it("should collapse an already-expanded node on second click", () => {
      const row: HTMLElement =
        fixture.nativeElement.querySelector(".tree-group-row");
      row.click();
      row.click();
      expect(component["expandedKey"]).toBeNull();
    });
  });

  describe("hasAllFormsUploaded", () => {
    it("should return true when all forms are uploaded", () => {
      const node = {
        name: "A",
        key: "key-a",
        definition: baseProcessDefinition,
      };
      expect(component["hasAllFormsUploaded"](node)).toBe(true);
    });

    it("should return false when a form is not uploaded", () => {
      const node = {
        name: "A",
        key: "key-a",
        definition: fromPartial<GeneratedType<"RestBpmnProcessDefinition">>({
          ...baseProcessDefinition,
          details: {
            ...baseProcessDefinition.details,
            forms: [{ formKey: "f1", title: "Form 1", uploaded: false }],
          },
        }),
      };
      expect(component["hasAllFormsUploaded"](node)).toBe(false);
    });

    it("should return false when there are no forms", () => {
      const node = {
        name: "A",
        key: "key-a",
        definition: fromPartial<GeneratedType<"RestBpmnProcessDefinition">>({
          ...baseProcessDefinition,
          details: { ...baseProcessDefinition.details, forms: [] },
        }),
      };
      expect(component["hasAllFormsUploaded"](node)).toBe(false);
    });
  });

  describe("selectBpmnProcessDefinitionFile", () => {
    it("should trigger a click on the hidden file input", () => {
      const fileInput: HTMLInputElement =
        fixture.nativeElement.querySelector('input[type="file"]');
      const clickSpy = jest.spyOn(fileInput, "click");

      component["selectBpmnProcessDefinitionFile"]();

      expect(clickSpy).toHaveBeenCalled();
    });
  });

  describe("bpmnProcessDefinitionFileSelected", () => {
    it("should do nothing when target is not an HTMLInputElement", () => {
      const event = { target: {} } as unknown as Event;
      component["bpmnProcessDefinitionFileSelected"](event);
      expect(bpmnService.uploadProcessDefinition).not.toHaveBeenCalled();
    });

    it("should upload file content and reload on success", async () => {
      const fileContent = "<bpmn/>";
      (readFileContent as jest.Mock).mockResolvedValue(fileContent);

      const file = new File([fileContent], "process.bpmn", {
        type: "text/xml",
      });
      const input = document.createElement("input");
      Object.defineProperty(input, "files", { value: [file] });
      const event = { target: input } as unknown as Event;

      component["bpmnProcessDefinitionFileSelected"](event);
      await Promise.resolve();

      expect(readFileContent).toHaveBeenCalledWith(file);
      expect(bpmnService.uploadProcessDefinition).toHaveBeenCalledWith({
        filename: "process.bpmn",
        content: fileContent,
      });
    });

    it("should call foutAfhandelingService when readFileContent rejects", async () => {
      const error = new Error("read error");
      (readFileContent as jest.Mock).mockRejectedValue(error);

      const input = document.createElement("input");
      Object.defineProperty(input, "files", {
        value: [new File(["<bad>"], "bad.bpmn")],
      });
      const event = { target: input } as unknown as Event;

      component["bpmnProcessDefinitionFileSelected"](event);
      await Promise.resolve();
      await Promise.resolve();

      expect(foutAfhandelingService.foutAfhandelen).toHaveBeenCalledWith(error);
    });
  });

  describe("deleteProcessDefinition", () => {
    it("should open a confirm dialog with the correct translation key and name", () => {
      component["deleteProcessDefinition"]({ key: "key-a", name: "Process A" });

      const dialogData = dialogOpenSpy.mock.calls[0][1].data;
      expect(dialogData._melding.key).toBe(
        "msg.procesdefinitie.verwijderen.bevestigen",
      );
      expect(dialogData._melding.args).toEqual({ naam: "Process A" });
    });

    it("should show snackbar and reload when dialog is confirmed", () => {
      dialogOpenSpy.mockReturnValue({ afterClosed: () => of(true) } as never);
      const loadSpy = jest.spyOn(
        component as ProcessDefinitionsComponent & {
          loadBpmnProcessDefinitions: () => void;
        },
        "loadBpmnProcessDefinitions",
      );

      component["deleteProcessDefinition"]({ key: "key-a", name: "Process A" });

      expect(utilService.openSnackbar).toHaveBeenCalledWith(
        "msg.procesdefinitie.verwijderen.uitgevoerd",
        { naam: "Process A" },
      );
      expect(loadSpy).toHaveBeenCalled();
    });

    it("should not show snackbar when dialog is cancelled", () => {
      component["deleteProcessDefinition"]({ key: "key-a", name: "Process A" });
      expect(utilService.openSnackbar).not.toHaveBeenCalled();
    });
  });
});
