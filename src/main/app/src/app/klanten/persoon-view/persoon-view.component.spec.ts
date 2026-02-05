/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ComponentFixture, TestBed } from "@angular/core/testing";
import { ActivatedRoute } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { of } from "rxjs";
import { UtilService } from "../../core/service/util.service";
import { DatumPipe } from "../../shared/pipes/datum.pipe";
import { GeneratedType } from "../../shared/utils/generated-types";
import { PersoonViewComponent } from "./persoon-view.component";

describe(PersoonViewComponent.name, () => {
  let component: PersoonViewComponent;
  let fixture: ComponentFixture<PersoonViewComponent>;

  const mockPersoon: GeneratedType<"RestPersoon"> = {
    naam: "Jan de Vries",
    bsn: "123456789",
    geboortedatum: "1990-01-15",
    indicaties: [],
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [PersoonViewComponent, DatumPipe],
      imports: [TranslateModule.forRoot()],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            data: of({ persoon: mockPersoon }),
          },
        },
        {
          provide: UtilService,
          useValue: { setTitle: jest.fn() },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PersoonViewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it("should receive persoon data from route resolver", () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const nameElement = compiled.querySelector('zac-static-text[label="naam"]');
    expect(nameElement).toBeTruthy();
  });

  it("should render persoon name", () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const nameElement = compiled.querySelector('zac-static-text[label="naam"]');
    expect(nameElement).toBeTruthy();
  });
});
