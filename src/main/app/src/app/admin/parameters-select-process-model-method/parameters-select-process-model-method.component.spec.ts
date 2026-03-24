/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NO_ERRORS_SCHEMA } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { ReactiveFormsModule } from "@angular/forms";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { ActivatedRoute } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { BehaviorSubject } from "rxjs";
import { GeneratedType } from "src/app/shared/utils/generated-types";
import { ProcessModelMethodSelection } from "../model/parameters/process-model-method";
import { ParameterSelectProcessModelMethodComponent } from "./parameters-select-process-model-method.component";

const mockZaaktype: GeneratedType<"RestZaaktype"> = {
  uuid: "uuid-1",
  identificatie: "TEST-001",
  doel: "",
  omschrijving: "Test zaaktype omschrijving",
};

describe(ParameterSelectProcessModelMethodComponent.name, () => {
  let fixture: ComponentFixture<ParameterSelectProcessModelMethodComponent>;
  let component: ParameterSelectProcessModelMethodComponent;
  let routeData$: BehaviorSubject<{ parameters: Record<string, unknown> }>;

  beforeEach(async () => {
    routeData$ = new BehaviorSubject<{
      parameters: Record<string, unknown>;
    }>({
      parameters: {
        zaakafhandelParameters: {
          zaaktypeUuid: "uuid-1",
          zaaktypeOmschrijving: "Test zaaktype omschrijving",
          bpmnProcessDefinitionKey: "",
          productaanvraagtype: null,
          groepNaam: "",
          zaaktype: mockZaaktype,
          betrokkeneKoppelingen: {},
          brpDoelbindingen: {},
          zaakbeeindigParameters: [],
        },
      },
    });

    await TestBed.configureTestingModule({
      declarations: [ParameterSelectProcessModelMethodComponent],
      imports: [
        ReactiveFormsModule,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: { data: routeData$.asObservable() },
        },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();

    fixture = TestBed.createComponent(
      ParameterSelectProcessModelMethodComponent,
    );
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it("should display the zaaktype omschrijving from route data", () => {
    expect(fixture.nativeElement.querySelector("h2").textContent.trim()).toBe(
      "Test zaaktype omschrijving",
    );
  });

  it("should emit CMMN selection when CMMN option is chosen", () => {
    const emitted: ProcessModelMethodSelection[] = [];
    component.switchModellingMethod.subscribe((v) => emitted.push(v));

    component["cmmnBpmnFormGroup"].controls.options.setValue({
      value: "CMMN",
      label: "CMMN",
    });

    expect(emitted).toEqual([{ type: "CMMN" }]);
  });

  it("should emit BPMN selection when BPMN option is chosen", () => {
    const emitted: ProcessModelMethodSelection[] = [];
    component.switchModellingMethod.subscribe((v) => emitted.push(v));

    component["cmmnBpmnFormGroup"].controls.options.setValue({
      value: "BPMN",
      label: "BPMN",
    });

    expect(emitted).toEqual([{ type: "BPMN" }]);
  });

  it("should emit null type when option is cleared", () => {
    const emitted: ProcessModelMethodSelection[] = [];
    component.switchModellingMethod.subscribe((v) => emitted.push(v));

    component["cmmnBpmnFormGroup"].controls.options.setValue(null);

    expect(emitted).toEqual([{ type: null }]);
  });
});
