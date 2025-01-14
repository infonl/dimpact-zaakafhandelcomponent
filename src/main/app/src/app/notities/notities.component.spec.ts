/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpClientModule } from "@angular/common/http";
import { ComponentFixture, TestBed } from "@angular/core/testing";

import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { Observable, of } from "rxjs";
import { IdentityService } from "../identity/identity.service";
import { MaterialModule } from "../shared/material/material.module";
import { PipesModule } from "../shared/pipes/pipes.module";
import { Notitie } from "./model/notitie";
import { NotitiesComponent } from "./notities.component";
import { NotitieService } from "./notities.service";
import {GeneratedType} from "../shared/utils/generated-types";

const currentUser: GeneratedType<'RestLoggedInUser'> = {
  id: 'currentUser',
  naam: 'test'
}

const mockIdentityService = {
  readLoggedInUser() {
    return of(currentUser);
  },
};
const mockNotitieService = {
  listNotities(type: string, uuid: string): Observable<Notitie[]> {
    return of([]);
  },
  updateNotitie(notitie: Notitie) {
    return of(notitie);
  },
};

const mockTranslateService = {
  get(key: any): any {
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
      imports: [
        HttpClientModule,
        TranslateModule,
        MaterialModule,
        PipesModule,
        NoopAnimationsModule,
      ],
      declarations: [NotitiesComponent],
      providers: [
        { provide: IdentityService, useValue: mockIdentityService },
        { provide: NotitieService, useValue: mockNotitieService },
        { provide: TranslateService, useValue: mockTranslateService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(NotitiesComponent);
    component = fixture.componentInstance;
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
});
