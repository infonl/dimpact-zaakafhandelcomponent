/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  provideHttpClient,
  withInterceptorsFromDi,
} from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { of } from "rxjs";
import { testQueryClient } from "../../../setupJest";
import { IdentityService } from "../identity/identity.service";
import { GeneratedType } from "../shared/utils/generated-types";
import { NotitiesComponent } from "./notities.component";
import { NotitieService } from "./notities.service";

const currentUser: GeneratedType<"RestLoggedInUser"> = {
  id: "currentUser",
  naam: "test",
};

describe(NotitiesComponent.name, () => {
  let component: NotitiesComponent;
  let fixture: ComponentFixture<NotitiesComponent>;
  let notitieService: NotitieService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        NotitiesComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideQueryClient(testQueryClient),
      ],
    }).compileComponents();

    const identityService = TestBed.inject(IdentityService);
    testQueryClient.setQueryData(
      identityService.readLoggedInUser().queryKey,
      currentUser,
    );

    notitieService = TestBed.inject(NotitieService);
    jest.spyOn(notitieService, "listNotities").mockReturnValue(of([]));
    jest
      .spyOn(notitieService, "updateNotitie")
      .mockImplementation((notitie) => of(notitie));

    fixture = TestBed.createComponent(NotitiesComponent);
    component = fixture.componentInstance;
    component.zaakUuid = "test-zaak-uuid";
    component.notitieRechten = { lezen: true, wijzigen: true };
    fixture.detectChanges();
  });

  it("should load notities on init", () => {
    expect(notitieService.listNotities).toHaveBeenCalledWith("test-zaak-uuid");
  });

  it("should set new text and current username on notitie edit", () => {
    const notitie: GeneratedType<"RestNote"> = {
      zaakUUID: "some-uuid",
      tekst: "some text",
      gebruikersnaamMedewerker: "some other user",
    };
    component["updateNotitie"](notitie, "some other text");
    expect(notitie.gebruikersnaamMedewerker).toEqual(currentUser.id);
    expect(notitie.tekst).toEqual("some other text");
  });

  it("should not call updateNotitie service when tekst is empty", () => {
    const notitie: GeneratedType<"RestNote"> = {
      zaakUUID: "some-uuid",
      tekst: "some text",
      gebruikersnaamMedewerker: "some user",
    };
    component["updateNotitie"](notitie, "");
    expect(notitieService.updateNotitie).not.toHaveBeenCalled();
  });
});
