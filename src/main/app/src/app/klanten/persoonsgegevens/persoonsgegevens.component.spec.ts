/*
 * SPDX-FileCopyrightText:  2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { TestBed } from "@angular/core/testing";
import { MatExpansionPanelHarness } from "@angular/material/expansion/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { of, throwError } from "rxjs";
import { PipesModule } from "src/app/shared/pipes/pipes.module";
import { testQueryClient } from "../../../../setupJest";
import { MaterialModule } from "../../shared/material/material.module";
import { GeneratedType } from "../../shared/utils/generated-types";
import { KlantenService } from "../klanten.service";
import { PersoonsgegevensComponent } from "./persoonsgegevens.component";

const mockTranslateService = {
  get(key: unknown) {
    return of(key);
  },
  getCurrentLang() {
    return "nl";
  },
  getFallbackLang() {
    return "nl";
  },
  onTranslationChange: of({}),
  onLangChange: of({}),
  onFallbackLangChange: of({}),
};

const testPerson: GeneratedType<"RestPersoon"> = {
  personId: "f31b38f2-d336-431f-a045-2ce4240c6c7e",
  indicaties: [],
};

describe("PersoonsgegevensComponent", () => {
  let klantenServiceMock: Partial<KlantenService>;
  let loader: HarnessLoader;

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
        provideQueryClient(testQueryClient),
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(PersoonsgegevensComponent);
    const ref = fixture.componentRef;
    ref.setInput("personId", "f31b38f2-d336-431f-a045-2ce4240c6c7e");
    ref.setInput("zaaktypeUuid", "test-zaaktype-uuid");
    ref.setInput("action", "test");
    fixture.detectChanges();
    loader = TestbedHarnessEnvironment.loader(fixture);
  });

  it("should call service method just once", () => {
    expect(klantenServiceMock.readPersoon).toHaveBeenCalledTimes(1);
  });

  it("should disable expansion panel on error", async () => {
    klantenServiceMock.readPersoon = jest
      .fn()
      .mockReturnValue(throwError(() => new Error("Person not found")));

    const fixture = TestBed.createComponent(PersoonsgegevensComponent);
    const ref = fixture.componentRef;
    ref.setInput("personId", "invalid-id");
    ref.setInput("zaaktypeUuid", "test-zaaktype-uuid");
    ref.setInput("action", "test");
    fixture.detectChanges();

    const testLoader = TestbedHarnessEnvironment.loader(fixture);
    await fixture.whenStable();

    const panel = await testLoader.getHarness(MatExpansionPanelHarness);
    expect(await panel.isDisabled()).toBe(true);
  });

  it("should show warning icon on error", async () => {
    klantenServiceMock.readPersoon = jest
      .fn()
      .mockReturnValue(throwError(() => new Error("Person not found")));

    const fixture = TestBed.createComponent(PersoonsgegevensComponent);
    const ref = fixture.componentRef;
    ref.setInput("personId", "invalid-id");
    ref.setInput("zaaktypeUuid", "test-zaaktype-uuid");
    ref.setInput("action", "test");

    fixture.detectChanges();
    expect(await fixture.nativeElement.querySelector("mat-icon")).toBeTruthy();
  });
});
