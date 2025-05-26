/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ComponentHarness, HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { of } from "rxjs";
import { IdentityService } from "../identity/identity.service";
import { MaterialModule } from "../shared/material/material.module";
import { PipesModule } from "../shared/pipes/pipes.module";
import { GeneratedType } from "../shared/utils/generated-types";
import { NotitiesComponent } from "./notities.component";
import { NotitieService } from "./notities.service";

const currentUser: GeneratedType<"RestLoggedInUser"> = {
  id: "currentUser",
  naam: "test",
};

const mockIdentityService = {
  readLoggedInUser() {
    return of(currentUser);
  },
};
const mockNotitieService = {
  listNotities() {
    return of([]);
  },
  updateNotitie(notitie: GeneratedType<"RestNote">) {
    return of(notitie);
  },
};

const mockTranslateService = {
  get(key: unknown) {
    return of(key);
  },
  onTranslationChange: of({}),
  onLangChange: of({}),
  onDefaultLangChange: of({}),
};

class NotitiesHarness extends ComponentHarness {
  static hostSelector = ".notitie-container";

  async isTextareaVisible(): Promise<boolean> {
    const textareas = await this.locatorForAll("textarea")();
    return textareas.length > 0;
  }
}

describe("NotitiesComponent Harness", () => {
  let fixture: ComponentFixture<NotitiesComponent>;
  let component: NotitiesComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [NotitiesComponent],
      imports: [
        TranslateModule,
        MaterialModule,
        PipesModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: IdentityService, useValue: mockIdentityService },
        { provide: NotitieService, useValue: mockNotitieService },
        { provide: TranslateService, useValue: mockTranslateService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(NotitiesComponent);
    component = fixture.componentInstance;
  });

  it.each`
    wijzigen | expected
    ${true}  | ${true}
    ${false} | ${false}
  `(
    "should ${expected ? '' : 'not '}show textarea when wijzigen is $wijzigen",
    async ({ wijzigen, expected }) => {
      component.notitieRechten = { lezen: false, wijzigen };
      fixture.detectChanges();

      const loader: HarnessLoader = TestbedHarnessEnvironment.loader(fixture);
      const harness = await loader.getHarness(NotitiesHarness);

      expect(await harness.isTextareaVisible()).toBe(expected);
    },
  );
});
