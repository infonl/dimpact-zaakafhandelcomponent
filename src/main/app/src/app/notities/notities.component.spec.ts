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

const mockTranslateService = {
  get(key: unknown) {
    return of(key);
  },
  onTranslationChange: of({}),
  onLangChange: of({}),
  onDefaultLangChange: of({}),
};

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
        { provide: TranslateService, useValue: mockTranslateService },
        provideHttpClient(withInterceptorsFromDi()),
      ],
    }).compileComponents();

    const notitieService = TestBed.inject(NotitieService);
    jest.spyOn(notitieService, "listNotities").mockReturnValue(of([]));
    jest
      .spyOn(notitieService, "updateNotitie")
      .mockImplementation((notitie) => {
        return of(notitie);
      });
    fixture = TestBed.createComponent(NotitiesComponent);
    component = fixture.componentInstance;
    component.notitieRechten = { lezen: true, wijzigen: true };
    fixture.detectChanges();
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });

  it("should set new text and current username on notitie edit", () => {
    const notitie: GeneratedType<"RestNote"> = {
      zaakUUID: "some-uuid",
      tekst: "some text",
      gebruikersnaamMedewerker: "some other user",
    };
    const someOtherText = "some other text";
    component.updateNotitie(notitie, someOtherText);
    expect(notitie.gebruikersnaamMedewerker).toEqual(currentUser.id);
    expect(notitie.tekst).toEqual(someOtherText);
  });
});
