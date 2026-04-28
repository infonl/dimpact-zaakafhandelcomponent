/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { provideHttpClient } from "@angular/common/http";
import { LOCALE_ID } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatCardHarness } from "@angular/material/card/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { fromPartial } from "src/test-helpers";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZaakVerkortComponent } from "./zaak-verkort.component";

const makeZaak = (fields: Partial<GeneratedType<"RestZaak">> = {}) =>
  fromPartial<GeneratedType<"RestZaak">>({
    identificatie: "ZAAK-001",
    zaaktype: fromPartial<GeneratedType<"RestZaaktype">>({ omschrijving: "Testtype" }),
    status: fromPartial<GeneratedType<"RestZaakStatus">>({ naam: "Open" }),
    startdatum: "2026-01-01",
    einddatumGepland: null,
    einddatum: null,
    toelichting: "Testtoelichting",
    ...fields,
  });

const setup = (zaak = makeZaak()) => {
  TestBed.configureTestingModule({
    imports: [ZaakVerkortComponent, NoopAnimationsModule, TranslateModule.forRoot()],
    providers: [provideHttpClient(), provideRouter([]), { provide: LOCALE_ID, useValue: "nl" }],
  });
  const fixture: ComponentFixture<ZaakVerkortComponent> =
    TestBed.createComponent(ZaakVerkortComponent);
  fixture.componentRef.setInput("zaak", zaak);
  fixture.detectChanges();
  return { fixture, component: fixture.componentInstance };
};

describe(ZaakVerkortComponent.name, () => {
  it("renders the zaak identificatie in the card title", async () => {
    const { fixture } = setup(makeZaak({ identificatie: "ZAAK-2026-001" }));
    const loader = TestbedHarnessEnvironment.loader(fixture);
    const card = await loader.getHarness(MatCardHarness);
    expect(await card.getTitleText()).toContain("ZAAK-2026-001");
  });

  it("renders zaaktype omschrijving as subtitle", async () => {
    const { fixture } = setup(
      makeZaak({ zaaktype: fromPartial<GeneratedType<"RestZaaktype">>({ omschrijving: "Bijzonder type" }) }),
    );
    const loader = TestbedHarnessEnvironment.loader(fixture);
    const card = await loader.getHarness(MatCardHarness);
    expect(await card.getSubtitleText()).toBe("Bijzonder type");
  });

  it("renders link to zaak detail page", () => {
    const { fixture } = setup(makeZaak({ identificatie: "ZAAK-999" }));
    const anchor: HTMLAnchorElement = fixture.nativeElement.querySelector("a[id='zaakDetail_button']");
    expect(anchor.getAttribute("href")).toBe("/zaken/ZAAK-999");
  });

  it("renders status naam via empty pipe", () => {
    const { fixture } = setup(
      makeZaak({ status: fromPartial<GeneratedType<"RestZaakStatus">>({ naam: "In behandeling" }) }),
    );
    expect(fixture.nativeElement.textContent).toContain("In behandeling");
  });

  it("renders startdatum via datum pipe", () => {
    const { fixture } = setup(makeZaak({ startdatum: "2026-03-15" }));
    expect(fixture.nativeElement.textContent).toContain("15\u201103\u20112026");
  });

  it("renders toelichting value", () => {
    const { fixture } = setup(makeZaak({ toelichting: "Mijn toelichting" }));
    expect(fixture.nativeElement.textContent).toContain("Mijn toelichting");
  });

  it("sets einddatumGeplandIcon when einddatum is exceeded", () => {
    const { component } = setup(makeZaak({ einddatumGepland: "2020-01-01", einddatum: "" }));
    expect(component["einddatumGeplandIcon"]).not.toBeNull();
  });
});
