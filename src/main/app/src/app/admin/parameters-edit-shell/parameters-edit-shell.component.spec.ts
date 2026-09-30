/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, EventEmitter, input, Output } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { ActivatedRoute, provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { screen } from "@testing-library/angular";
import { BehaviorSubject } from "rxjs";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { UtilService } from "../../core/service/util.service";
import { ProcessModelMethodSelection } from "../model/parameters/process-model-method";
import { ParametersEditBpmnComponent } from "../parameters-edit-bpmn/parameters-edit-bpmn.component";
import { ParametersEditCmmnComponent } from "../parameters-edit-cmmn/parameters-edit-cmmn.component";
import { ParameterSelectProcessModelMethodComponent } from "../parameters-select-process-model-method/parameters-select-process-model-method.component";
import { ParametersEditShellComponent } from "./parameters-edit-shell.component";

@Component({
  selector: "zac-parameters-select-process-model-method",
  template: '<section aria-label="fakeModellingMethodSelector"></section>',
  standalone: true,
})
class StubSelectMethodComponent {
  @Output() switchModellingMethod =
    new EventEmitter<ProcessModelMethodSelection>();
}

@Component({
  selector: "zac-parameters-edit-cmmn",
  template:
    '<section aria-label="fakeCmmnEditor">{{ selectedIndexStart() }}</section>',
  standalone: true,
})
class StubCmmnComponent {
  readonly selectedIndexStart = input(0);
  @Output() switchModellingMethod =
    new EventEmitter<ProcessModelMethodSelection>();
}

@Component({
  selector: "zac-parameters-edit-bpmn",
  template:
    '<section aria-label="fakeBpmnEditor">{{ selectedIndexStart() }}</section>',
  standalone: true,
})
class StubBpmnComponent {
  readonly selectedIndexStart = input(0);
  @Output() switchModellingMethod =
    new EventEmitter<ProcessModelMethodSelection>();
}

describe(ParametersEditShellComponent.name, () => {
  let fixture: ComponentFixture<ParametersEditShellComponent>;
  let component: ParametersEditShellComponent;
  let utilServiceMock: Pick<UtilService, "setTitle">;
  let routeData$: BehaviorSubject<{ parameters: Record<string, unknown> }>;

  function createComponent(parameters: Record<string, unknown>) {
    routeData$.next({ parameters });
    fixture = TestBed.createComponent(ParametersEditShellComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  function modellingMethodSelector() {
    return screen.getByRole("region", { name: "fakeModellingMethodSelector" });
  }

  function cmmnEditor() {
    return screen.getByRole("region", { name: "fakeCmmnEditor" });
  }

  function bpmnEditor() {
    return screen.getByRole("region", { name: "fakeBpmnEditor" });
  }

  beforeEach(async () => {
    utilServiceMock = { setTitle: jest.fn() };
    routeData$ = new BehaviorSubject<{ parameters: Record<string, unknown> }>({
      parameters: {},
    });

    await TestBed.configureTestingModule({
      imports: [
        ParametersEditShellComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [
        provideRouter([]),
        {
          provide: UtilService,
          useValue: utilServiceMock satisfies Pick<UtilService, "setTitle">,
        },
        {
          provide: ConfiguratieService,
          useValue: {} satisfies Partial<ConfiguratieService>,
        },
        {
          provide: ActivatedRoute,
          useValue: {
            data: routeData$.asObservable(),
          } satisfies Pick<ActivatedRoute, "data">,
        },
      ],
    })
      .overrideComponent(ParametersEditShellComponent, {
        remove: {
          imports: [
            ParameterSelectProcessModelMethodComponent,
            ParametersEditCmmnComponent,
            ParametersEditBpmnComponent,
          ],
        },
        add: {
          imports: [
            StubSelectMethodComponent,
            StubCmmnComponent,
            StubBpmnComponent,
          ],
        },
      })
      .compileComponents();
  });

  it("should show the modelling method selector when no process type is configured", () => {
    createComponent({ isBpmn: false, isSavedZaakafhandelParameters: false });

    expect(modellingMethodSelector()).toBeInTheDocument();
  });

  it("should show the BPMN editor when isBpmn is true", () => {
    createComponent({ isBpmn: true });

    expect(bpmnEditor()).toBeInTheDocument();
  });

  it("should show the CMMN editor when isSavedZaakafhandelParameters is true", () => {
    createComponent({ isBpmn: false, isSavedZaakafhandelParameters: true });

    expect(cmmnEditor()).toBeInTheDocument();
  });

  it("starts the BPMN editor at its second step when isBpmn is true", () => {
    createComponent({ isBpmn: true });

    expect(bpmnEditor()).toHaveTextContent("1");
  });

  it("starts the CMMN editor at its second step when isSavedZaakafhandelParameters is true", () => {
    createComponent({ isBpmn: false, isSavedZaakafhandelParameters: true });

    expect(cmmnEditor()).toHaveTextContent("1");
  });

  describe("switchModellingMethod", () => {
    beforeEach(() => createComponent({}));

    it("should switch to BPMN editor", () => {
      component["switchModellingMethod"]({ type: "BPMN" });
      fixture.detectChanges();

      expect(bpmnEditor()).toBeInTheDocument();
    });

    it("starts the editor it switches to at its first step when no start step is given", () => {
      component["switchModellingMethod"]({ type: "BPMN" });
      fixture.detectChanges();

      expect(bpmnEditor()).toHaveTextContent("0");
    });

    it("starts the editor it switches to at the given start step", () => {
      component["switchModellingMethod"]({
        type: "BPMN",
        selectedIndexStart: 1,
      });
      fixture.detectChanges();

      expect(bpmnEditor()).toHaveTextContent("1");
    });

    it("should switch to CMMN editor", () => {
      component["switchModellingMethod"]({ type: "CMMN" });
      fixture.detectChanges();

      expect(cmmnEditor()).toBeInTheDocument();
    });

    it("should switch back to the modelling method selector", () => {
      component["switchModellingMethod"]({ type: "BPMN" });
      fixture.detectChanges();

      component["switchModellingMethod"]({ type: null });
      fixture.detectChanges();

      expect(modellingMethodSelector()).toBeInTheDocument();
    });
  });
});
