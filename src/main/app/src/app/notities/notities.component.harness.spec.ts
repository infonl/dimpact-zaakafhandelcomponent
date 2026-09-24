/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  provideHttpClient,
  withInterceptorsFromDi,
} from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { Component } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { screen } from "@testing-library/angular";
import { userEvent } from "@testing-library/user-event";
import { of } from "rxjs";
import { testQueryClient } from "../../../setupJest";
import { IdentityService } from "../identity/identity.service";
import { GeneratedType } from "../shared/utils/generated-types";
import { NotitiesComponent } from "./notities.component";
import { NotitieService } from "./notities.service";

@Component({
  template: `<zac-notities
    zaakUuid="fakeZaakUuid"
    [notitieRechten]="notitieRechten"
  ></zac-notities>`,
  standalone: true,
  imports: [NotitiesComponent],
})
class TestHostComponent {
  notitieRechten?: GeneratedType<"RestNotitieRechten">;
}

const currentUser: GeneratedType<"RestLoggedInUser"> = {
  id: "currentUser",
  naam: "test",
};

describe("NotitiesComponent harness", () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let notitieService: NotitieService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        TestHostComponent,
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

    fixture = TestBed.createComponent(TestHostComponent);
    fixture.detectChanges();
  });

  it("should load the notities of the zaak it is given as a static attribute", () => {
    expect(notitieService.listNotities).toHaveBeenCalledWith("fakeZaakUuid");
  });

  it.each`
    str      | wijzigen | expected
    ${""}    | ${true}  | ${true}
    ${"not"} | ${false} | ${false}
  `(
    "should $str show textarea when wijzigen is $wijzigen",
    async ({ wijzigen, expected }) => {
      const user = userEvent.setup();
      fixture.componentInstance.notitieRechten = { lezen: false, wijzigen };
      fixture.detectChanges();
      await fixture.whenStable();

      await user.click(screen.getByRole("button", { name: "Notities" }));
      fixture.detectChanges();
      await fixture.whenStable();

      expect(
        screen.getByRole("button", { name: "actie.minimaliseren" }),
      ).toBeInTheDocument();
      expect(screen.queryByRole("textbox") !== null).toBe(expected);
    },
  );
});
