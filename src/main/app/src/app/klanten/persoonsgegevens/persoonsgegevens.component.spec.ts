/*
 * SPDX-FileCopyrightText:  2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TestBed } from "@angular/core/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { of } from "rxjs";
import { PipesModule } from "src/app/shared/pipes/pipes.module";
import { MaterialModule } from "../../shared/material/material.module";
import { GeneratedType } from "../../shared/utils/generated-types";
import { KlantenService } from "../klanten.service";
import { PersoonsgegevensComponent } from "./persoonsgegevens.component";

const mockTranslateService = {
  get(key: unknown) {
    return of(key);
  },
  onTranslationChange: of({}),
  onLangChange: of({}),
  onFallbackLangChange: of({}),
};

const testPerson: GeneratedType<"RestPersoon"> = {
  bsn: "999993033",
  indicaties: [],
};

describe("PersoonsgegevensComponent", () => {
  let klantenServiceMock: Partial<KlantenService>;

  beforeEach(async () => {
    klantenServiceMock = {
      readPersoon: jest.fn().mockReturnValue(of(testPerson)),
    };

    await TestBed.configureTestingModule({
      declarations: [PersoonsgegevensComponent],
      imports: [
        TranslateModule.forRoot(),
        PipesModule,
        MaterialModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: KlantenService, useValue: klantenServiceMock },
        { provide: TranslateService, useValue: mockTranslateService },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(PersoonsgegevensComponent);
    const ref = fixture.componentRef;
    ref.setInput("bsn", "20");
    ref.setInput("zaakIdentificatie", "test");
    ref.setInput("action", "test");
    fixture.detectChanges();
  });

  it("should call service method just once", () => {
    expect(klantenServiceMock.readPersoon).toHaveBeenCalledTimes(1);
  });
});
