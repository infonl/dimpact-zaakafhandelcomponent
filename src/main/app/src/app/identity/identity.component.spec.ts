/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { provideHttpClient } from "@angular/common/http";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatCardHarness } from "@angular/material/card/testing";
import { MatListItemHarness } from "@angular/material/list/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { testQueryClient } from "../../../setupJest";
import { GeneratedType } from "../shared/utils/generated-types";
import { IdentityService } from "./identity.service";
import { IdentityComponent } from "./identity.component";

const makeLoggedInUser = (
  fields: Partial<GeneratedType<"RestLoggedInUser">> = {},
): GeneratedType<"RestLoggedInUser"> =>
  ({
    id: "user-1",
    naam: "Test User",
    groupIds: [],
    functionalRoles: [],
    applicationRoles: {},
    ...fields,
  }) as Partial<
    GeneratedType<"RestLoggedInUser">
  > as unknown as GeneratedType<"RestLoggedInUser">;

describe(IdentityComponent.name, () => {
  let fixture: ComponentFixture<IdentityComponent>;
  let loader: HarnessLoader;
  let identityService: IdentityService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        IdentityComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [
        IdentityService,
        provideHttpClient(),
        provideQueryClient(testQueryClient),
      ],
    }).compileComponents();

    identityService = TestBed.inject(IdentityService);
  });

  const setup = (user: GeneratedType<"RestLoggedInUser">) => {
    testQueryClient.setQueryData(
      identityService.readLoggedInUser().queryKey,
      user,
    );
    fixture = TestBed.createComponent(IdentityComponent);
    fixture.detectChanges();
    loader = TestbedHarnessEnvironment.loader(fixture);
  };

  it("renders the Groepen card with one list item per group", async () => {
    setup(makeLoggedInUser({ groupIds: ["groep-a", "groep-b"] }));

    const cards = await loader.getAllHarnesses(MatCardHarness);
    const groepenCard = cards[0];
    expect(await groepenCard.getTitleText()).toBe("Groepen");

    const items = await loader.getAllHarnesses(
      MatListItemHarness.with({ ancestor: "mat-card:first-of-type" }),
    );
    expect(items).toHaveLength(2);
  });

  it("renders the Rollen card with one list item per role", async () => {
    setup(makeLoggedInUser({ functionalRoles: ["rol-1", "rol-2", "rol-3"] }));

    const cards = await loader.getAllHarnesses(MatCardHarness);
    const rollenCard = cards[1];
    expect(await rollenCard.getTitleText()).toBe("Rollen");
  });

  it("renders an empty Groepen card when groupIds is null", async () => {
    setup(makeLoggedInUser({ groupIds: null }));

    const cards = await loader.getAllHarnesses(MatCardHarness);
    expect(await cards[0].getTitleText()).toBe("Groepen");
    // No list items should appear for the empty groups list
    const allItems = await loader.getAllHarnesses(MatListItemHarness);
    expect(allItems).toHaveLength(0);
  });

  it("renders an empty Rollen card when functionalRoles is null", async () => {
    setup(makeLoggedInUser({ functionalRoles: null }));

    const cards = await loader.getAllHarnesses(MatCardHarness);
    expect(await cards[1].getTitleText()).toBe("Rollen");
    const allItems = await loader.getAllHarnesses(MatListItemHarness);
    expect(allItems).toHaveLength(0);
  });

  it("renders one card per zaaktype under Rollen per zaaktype", async () => {
    setup(
      makeLoggedInUser({
        applicationRoles: {
          "zaaktype-A": {} as Record<string, never>,
          "zaaktype-B": {} as Record<string, never>,
        },
      }),
    );

    const cards = await loader.getAllHarnesses(MatCardHarness);
    // First two are Groepen and Rollen; remaining are per zaaktype
    const zaakTypeCards = cards.slice(2);
    expect(zaakTypeCards).toHaveLength(2);
    expect(await zaakTypeCards[0].getTitleText()).toBe("zaaktype-A");
    expect(await zaakTypeCards[1].getTitleText()).toBe("zaaktype-B");
  });

  it("renders no zaaktype cards when applicationRoles is empty", async () => {
    setup(makeLoggedInUser({ applicationRoles: {} }));

    const cards = await loader.getAllHarnesses(MatCardHarness);
    expect(cards).toHaveLength(2); // only Groepen and Rollen
  });

  it("renders no zaaktype cards when applicationRoles is null", async () => {
    setup(makeLoggedInUser({ applicationRoles: null }));

    const cards = await loader.getAllHarnesses(MatCardHarness);
    expect(cards).toHaveLength(2); // only Groepen and Rollen
  });

  it("renders the h1 heading for Rollen per zaaktype", async () => {
    setup(makeLoggedInUser());

    const h1 = (fixture.nativeElement as Element).querySelector(
      "h1",
    ) as HTMLElement;
    expect(h1.textContent).toContain("Rollen per zaaktype");
  });

  it("renders group code elements inside the Groepen list", async () => {
    setup(makeLoggedInUser({ groupIds: ["my-group"] }));

    const nativeElement = fixture.nativeElement as Element;
    const codeElements = nativeElement.querySelectorAll("code");
    const texts = Array.from(codeElements).map((el) =>
      (el as HTMLElement).textContent?.trim(),
    );
    expect(texts).toContain("my-group");
  });

  it("renders role code elements inside the Rollen list", async () => {
    setup(makeLoggedInUser({ functionalRoles: ["ROLE_ADMIN"] }));

    const nativeElement = fixture.nativeElement as Element;
    const codeElements = nativeElement.querySelectorAll("code");
    const texts = Array.from(codeElements).map((el) =>
      (el as HTMLElement).textContent?.trim(),
    );
    expect(texts).toContain("ROLE_ADMIN");
  });
});
