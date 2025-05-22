/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  provideHttpClient,
  withInterceptorsFromDi,
} from "@angular/common/http";
import { ComponentFixture, TestBed } from "@angular/core/testing";

import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { Observable, of } from "rxjs";
import { IdentityService } from "../identity/identity.service";
import { MaterialModule } from "../shared/material/material.module";
import { PipesModule } from "../shared/pipes/pipes.module";
import { GeneratedType } from "../shared/utils/generated-types";
import { Notitie } from "./model/notitie";
import { NotitiesComponent } from "./notities.component";
import { NotitieService } from "./notities.service";
import { ComponentHarness, HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";

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
  listNotities(): Observable<Notitie[]> {
    return of([]);
  },
  updateNotitie(notitie: Notitie) {
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

    console.log("Textareas found:: ", textareas.length);
    return textareas.length > 0;
  }
}

describe("NotitiesComponent", () => {
  let component: NotitiesComponent;
  let fixture: ComponentFixture<NotitiesComponent>;
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
        provideHttpClient(withInterceptorsFromDi()),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(NotitiesComponent);
    component = fixture.componentInstance;
    component.notitieRechten = { lezen: true, wijzigen: true };
    fixture.detectChanges();
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });

  it("should set new text and current username on notitie edit", () => {
    const notitie = new Notitie();
    notitie.tekst = "some text";
    notitie.gebruikersnaamMedewerker = "some other user";
    notitie.id = 1;
    const someOtherText = "some other text";
    component.updateNotitie(notitie, someOtherText);
    expect(notitie.gebruikersnaamMedewerker).toEqual(currentUser.id);
    expect(notitie.tekst).toEqual(someOtherText);
  });

  it("Harness: should show textarea when wijzigen is true", async () => {
    component.notitieRechten = { lezen: false, wijzigen: true };
    fixture.detectChanges();

    const loader: HarnessLoader = TestbedHarnessEnvironment.loader(fixture);
    const harness = await loader.getHarness(NotitiesHarness);

    const isVisible = await harness.isTextareaVisible();
    expect(isVisible).toBeTruthy();
  });

  it("Harness: should not show textarea when wijzigen is false", async () => {
    component.notitieRechten = { lezen: false, wijzigen: false };
    fixture.detectChanges();

    const loader: HarnessLoader = TestbedHarnessEnvironment.loader(fixture);
    const harness = await loader.getHarness(NotitiesHarness);

    const isVisible = await harness.isTextareaVisible();
    expect(isVisible).toBeFalsy();
  });
});
