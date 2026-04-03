/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ComponentHarness, HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
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
import { of } from "rxjs";
import { testQueryClient } from "../../../setupJest";
import { IdentityService } from "../identity/identity.service";
import { GeneratedType } from "../shared/utils/generated-types";
import { NotitiesComponent } from "./notities.component";
import { NotitieService } from "./notities.service";

@Component({
  template: `<zac-notities zaakUuid="test-uuid"></zac-notities>`,
  standalone: true,
  imports: [NotitiesComponent],
})
class TestHostComponent {}

const currentUser: GeneratedType<"RestLoggedInUser"> = {
  id: "currentUser",
  naam: "test",
};

class NotitiesHarness extends ComponentHarness {
  static hostSelector = "zac-notities";

  async clickNotitiesButton(): Promise<void> {
    const button = await this.locatorFor('button[aria-label="Notities"]')();
    await button.click();
  }

  async isMatCardVisible(): Promise<boolean> {
    try {
      await this.locatorFor("mat-card")();
      return true;
    } catch {
      return false;
    }
  }

  async isTextareaVisible(): Promise<boolean> {
    try {
      await this.locatorFor("textarea")();
      return true;
    } catch {
      return false;
    }
  }
}

describe("NotitiesComponent harness", () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let loader: HarnessLoader;

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

    const notitieService = TestBed.inject(NotitieService);
    jest.spyOn(notitieService, "listNotities").mockReturnValue(of([]));
    jest
      .spyOn(notitieService, "updateNotitie")
      .mockImplementation((notitie) => of(notitie));

    fixture = TestBed.createComponent(TestHostComponent);
    fixture.detectChanges();

    loader = TestbedHarnessEnvironment.loader(fixture);
  });

  it.each`
    str      | wijzigen | expected
    ${""}    | ${true}  | ${true}
    ${"not"} | ${false} | ${false}
  `(
    "should $str show textarea when wijzigen is $wijzigen",
    async ({ wijzigen, expected }) => {
      const notitiesComponentInstance =
        fixture.debugElement.children[0].componentInstance;
      notitiesComponentInstance.notitieRechten = { lezen: false, wijzigen };

      fixture.detectChanges();
      await fixture.whenStable();

      const harness = await loader.getHarness(NotitiesHarness);
      await harness.clickNotitiesButton();

      fixture.detectChanges();
      await fixture.whenStable();

      expect(await harness.isMatCardVisible()).toBe(true);
      expect(await harness.isTextareaVisible()).toBe(expected);
    },
  );
});
