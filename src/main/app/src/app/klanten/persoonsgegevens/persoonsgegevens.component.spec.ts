/*
 * SPDX-FileCopyrightText:  2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Input } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { of } from "rxjs";
import { PipesModule } from "src/app/shared/pipes/pipes.module";
import { GeneratedType } from "../../shared/utils/generated-types";
import { KlantenService } from "../klanten.service";
import { PersoonsgegevensComponent } from "./persoonsgegevens.component";

const mockTranslateService = {
  get(key: any): any {
    return of(key);
  },
  onTranslationChange: of({}),
  onLangChange: of({}),
  onDefaultLangChange: of({}),
};

const testPerson: GeneratedType<"RestPersoon"> = {
  bsn: "999993033",
  indicaties: [],
};

describe("PersoonsgegevensComponent", () => {
  let component: PersoonsgegevensComponent;
  let fixture: ComponentFixture<PersoonsgegevensComponent>;
  let klantenServiceMock: any;

  beforeEach(async () => {
    klantenServiceMock = {
      readPersoon: jest.fn().mockReturnValue(of(testPerson)),
    };

    await TestBed.configureTestingModule({
      declarations: [PersoonsgegevensComponent],
      imports: [TranslateModule.forRoot(), PipesModule],
      providers: [
        { provide: KlantenService, useValue: klantenServiceMock },
        { provide: TranslateService, useValue: mockTranslateService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PersoonsgegevensComponent);
    component = fixture.componentInstance;
    component.bsn = new Input(testPerson.bsn);
    fixture.detectChanges();
  });

  it("should call service method just once", () => {
    expect(klantenServiceMock.readPersoon).toHaveBeenCalledTimes(1);
  });
});
