import { Component } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { of } from "rxjs";

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { ComponentHarness } from "@angular/cdk/testing";

import { NotitiesComponent } from "./notities.component";
import { NotitieService } from "./notities.service";
import { IdentityService } from "../identity/identity.service";
import { MaterialModule } from "../shared/material/material.module";
import { PipesModule } from "../shared/pipes/pipes.module";
import { GeneratedType } from "../shared/utils/generated-types";

@Component({
  template: `<zac-notities></zac-notities>`,
})
class TestHostComponent {}

const currentUser: GeneratedType<"RestLoggedInUser"> = {
  id: "currentUser",
  naam: "test",
};

const mockIdentityService = {
  readLoggedInUser: () => of(currentUser),
};

const mockNotitieService = {
  listNotities: () => of([]),
  updateNotitie: (notitie: GeneratedType<"RestNote">) => of(notitie),
};

const mockTranslateService = {
  get: (key: unknown) => of(key),
  onTranslationChange: of({}),
  onLangChange: of({}),
  onDefaultLangChange: of({}),
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

describe("NotitiesComponent Harness with Host Wrapper", () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let loader: HarnessLoader;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [TestHostComponent, NotitiesComponent],
      imports: [
        TranslateModule.forRoot(),
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
