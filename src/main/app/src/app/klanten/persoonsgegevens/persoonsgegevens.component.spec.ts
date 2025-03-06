/*
 * SPDX-FileCopyrightText:  2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ComponentFixture, TestBed } from "@angular/core/testing";
import { PersoonsgegevensComponent } from "./persoonsgegevens.component";
import { KlantenService } from "../klanten.service";
import { of } from "rxjs";
import { GeneratedType } from "../../shared/utils/generated-types";
import { Input } from "@angular/core";
import { TranslateService, TranslateModule } from "@ngx-translate/core";
import { PipesModule } from "src/app/shared/pipes/pipes.module";

const mockTranslateService = {
  get(key: any): any {
    return of(key);
  },
  onTranslationChange: of({}),
  onLangChange: of({}),
  onDefaultLangChange: of({}),
};

const persoon: GeneratedType<"RestPersoon"> = {
  bsn: "987654321",
  indicaties: [],
};

describe("PersoonsgegevensComponent", () => {
  let component: PersoonsgegevensComponent;
  let fixture: ComponentFixture<PersoonsgegevensComponent>;
  let klantenServiceMock: any;

  beforeEach(async () => {
    klantenServiceMock = {
      readPersoon: jest.fn().mockReturnValue(of({})),
    };

    await TestBed.configureTestingModule({
      declarations: [PersoonsgegevensComponent],
      imports: [TranslateModule.forRoot(), PipesModule],
      providers: [
        { provide: KlantenService, useValue: klantenServiceMock },
        { provide: TranslateService, useValue: mockTranslateService },
      ],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(PersoonsgegevensComponent);
    component = fixture.componentInstance;
    component.bsn = new Input("123456789");
    fixture.detectChanges();
  });

  it("should have created component", () => {
    expect(component).toBeTruthy();
  });

  it("should call service method just once", () => {
    expect(klantenServiceMock.readPersoon).toHaveBeenCalledTimes(1);
  });

  it("should emit delete event", () => {
    const deleteSpy = jest.spyOn(component.delete, "emit");

    component.delete.emit(persoon);
    expect(deleteSpy).toHaveBeenCalledWith(persoon);
  });

  it("should emit edit event", () => {
    const editSpy = jest.spyOn(component.edit, "emit");

    component.edit.emit(persoon);
    expect(editSpy).toHaveBeenCalledWith(persoon);
  });
});
