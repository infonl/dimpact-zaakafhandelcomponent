/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  provideHttpClient,
  withInterceptorsFromDi,
} from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { Component } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { screen } from "@testing-library/angular";
import { userEvent } from "@testing-library/user-event";
import { sleep, testQueryClient } from "../../../setupJest";
import { IdentityService } from "../identity/identity.service";
import { GeneratedType } from "../shared/utils/generated-types";
import { NotitiesComponent } from "./notities.component";

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
  let httpTestingController: HttpTestingController;

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

    httpTestingController = TestBed.inject(HttpTestingController);

    fixture = TestBed.createComponent(TestHostComponent);
    fixture.detectChanges();
    await sleep();
    httpTestingController
      .expectOne("/rest/notities/zaken/fakeZaakUuid")
      .flush([{ id: 1, zaakUUID: "fakeZaakUuid", tekst: "fakeTekst" }]);
    await sleep();
    fixture.detectChanges();
  });

  it("should show the notities of the zaak it is given as a static attribute", async () => {
    await userEvent
      .setup()
      .click(screen.getByRole("button", { name: "Notities" }));
    fixture.detectChanges();

    expect(screen.getByText("fakeTekst")).toBeInTheDocument();
  });

  it.each`
    str      | wijzigen | expected
    ${""}    | ${true}  | ${true}
    ${"not"} | ${false} | ${false}
  `(
    "should $str show textarea when wijzigen is $wijzigen",
    async ({ wijzigen, expected }) => {
      const user = userEvent.setup();
      fixture.componentInstance.notitieRechten = {
        canLezen: false,
        canWijzigen: wijzigen,
      };
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
