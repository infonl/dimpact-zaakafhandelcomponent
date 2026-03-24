/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, EventEmitter, Input, Output } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { ActivatedRoute, provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
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
  template: "",
  standalone: true,
})
class StubSelectMethodComponent {
  @Output() switchModellingMethod =
    new EventEmitter<ProcessModelMethodSelection>();
}

@Component({
  selector: "zac-parameters-edit-cmmn",
  template: "",
  standalone: true,
})
class StubCmmnComponent {
  @Input() selectedIndexStart = 0;
  @Output() switchModellingMethod =
    new EventEmitter<ProcessModelMethodSelection>();
}

@Component({
  selector: "zac-parameters-edit-bpmn",
  template: "",
  standalone: true,
})
class StubBpmnComponent {
  @Input() selectedIndexStart = 0;
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

    expect(
      fixture.nativeElement.querySelector(
        "zac-parameters-select-process-model-method",
      ),
    ).toBeTruthy();
  });

  it("should show the BPMN editor when isBpmn is true", () => {
    createComponent({ isBpmn: true });

    expect(
      fixture.nativeElement.querySelector("zac-parameters-edit-bpmn"),
    ).toBeTruthy();
  });

  it("should show the CMMN editor when isSavedZaakafhandelParameters is true", () => {
    createComponent({ isBpmn: false, isSavedZaakafhandelParameters: true });

    expect(
      fixture.nativeElement.querySelector("zac-parameters-edit-cmmn"),
    ).toBeTruthy();
  });

  describe("switchModellingMethod", () => {
    beforeEach(() => createComponent({}));

    it("should switch to BPMN editor", () => {
      component["switchModellingMethod"]({ type: "BPMN" });
      fixture.detectChanges();

      expect(
        fixture.nativeElement.querySelector("zac-parameters-edit-bpmn"),
      ).toBeTruthy();
    });

    it("should switch to CMMN editor", () => {
      component["switchModellingMethod"]({ type: "CMMN" });
      fixture.detectChanges();

      expect(
        fixture.nativeElement.querySelector("zac-parameters-edit-cmmn"),
      ).toBeTruthy();
    });

    it("should switch back to the modelling method selector", () => {
      component["switchModellingMethod"]({ type: "BPMN" });
      fixture.detectChanges();

      component["switchModellingMethod"]({ type: null });
      fixture.detectChanges();

      expect(
        fixture.nativeElement.querySelector(
          "zac-parameters-select-process-model-method",
        ),
      ).toBeTruthy();
    });
  });
});
