/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { CommonModule } from "@angular/common";
import { NO_ERRORS_SCHEMA } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatSidenavModule } from "@angular/material/sidenav";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { ActivatedRoute } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { BehaviorSubject } from "rxjs";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { UtilService } from "../../core/service/util.service";
import { ProcessModelMethodSelection } from "../model/parameters/process-model-method";
import { ParametersEditShellComponent } from "./parameters-edit-shell.component";

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
      declarations: [ParametersEditShellComponent],
      imports: [
        CommonModule,
        MatSidenavModule,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [
        { provide: UtilService, useValue: utilServiceMock },
        {
          provide: ConfiguratieService,
          useValue: {} satisfies Partial<ConfiguratieService>,
        },
        {
          provide: ActivatedRoute,
          useValue: { data: routeData$.asObservable() },
        },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();
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
      const definition: ProcessModelMethodSelection = { type: "BPMN" };
      component.switchModellingMethod(definition);
      fixture.detectChanges();

      expect(
        fixture.nativeElement.querySelector("zac-parameters-edit-bpmn"),
      ).toBeTruthy();
    });

    it("should switch to CMMN editor", () => {
      const definition: ProcessModelMethodSelection = { type: "CMMN" };
      component.switchModellingMethod(definition);
      fixture.detectChanges();

      expect(
        fixture.nativeElement.querySelector("zac-parameters-edit-cmmn"),
      ).toBeTruthy();
    });

    it("should switch back to the modelling method selector", () => {
      component.switchModellingMethod({ type: "BPMN" });
      fixture.detectChanges();

      component.switchModellingMethod({ type: null });
      fixture.detectChanges();

      expect(
        fixture.nativeElement.querySelector(
          "zac-parameters-select-process-model-method",
        ),
      ).toBeTruthy();
    });
  });
});
