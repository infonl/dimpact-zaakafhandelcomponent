/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { of } from "rxjs";
import { ReferentieTabelService } from "../admin/referentie-tabel.service";
import { FoutAfhandelingComponent } from "./fout-afhandeling.component";
import { FoutAfhandelingService } from "./fout-afhandeling.service";

describe(FoutAfhandelingComponent.name, () => {
  let fixture: ComponentFixture<FoutAfhandelingComponent>;
  let component: FoutAfhandelingComponent;
  let foutAfhandelingService: FoutAfhandelingService;
  let referentieTabelService: ReferentieTabelService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        FoutAfhandelingComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [provideHttpClient(), provideRouter([])],
    }).compileComponents();

    foutAfhandelingService = TestBed.inject(FoutAfhandelingService);
    referentieTabelService = TestBed.inject(ReferentieTabelService);
  });

  const setup = (
    foutmelding: string,
    bericht: string,
    serverErrorTexts: string[],
  ) => {
    foutAfhandelingService.foutmelding = foutmelding;
    foutAfhandelingService.bericht = bericht;
    jest
      .spyOn(referentieTabelService, "listServerErrorTexts")
      .mockReturnValue(of(serverErrorTexts) as never);

    fixture = TestBed.createComponent(FoutAfhandelingComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  };

  it("displays foutmelding in heading when set", () => {
    setup("Er is een fout opgetreden", "", []);

    const heading = (fixture.nativeElement as HTMLElement).querySelector("h2");
    expect(heading?.textContent).toContain("Er is een fout opgetreden");
  });

  it("falls back to default translation key in heading when foutmelding is empty", () => {
    setup("", "", []);

    const heading = (fixture.nativeElement as HTMLElement).querySelector("h2");
    expect(heading?.textContent).toContain("error-card.title.default");
  });

  it("shows bericht paragraph when bericht is non-empty", () => {
    setup("", "Neem contact op met de beheerder", []);

    const nativeElement = fixture.nativeElement as HTMLElement;
    const paragraphs = nativeElement.querySelectorAll("p");
    const berichtTexts = Array.from(paragraphs).map((p) => p.textContent ?? "");
    expect(
      berichtTexts.some((t) => t.includes("Neem contact op met de beheerder")),
    ).toBe(true);
  });

  it("does not show bericht paragraph when bericht is empty", () => {
    setup("Fout", "", []);

    const nativeElement = fixture.nativeElement as HTMLElement;
    const paragraphs = nativeElement.querySelectorAll("p");
    expect(paragraphs.length).toBe(0);
  });

  it("shows server error text paragraphs when serverErrorTexts emits values", () => {
    setup("", "", ["Server onbeschikbaar", "Probeer het later"]);

    const nativeElement = fixture.nativeElement as HTMLElement;
    const paragraphs = nativeElement.querySelectorAll("p");
    const texts = Array.from(paragraphs).map(
      (p) => p.textContent?.trim() ?? "",
    );
    expect(texts).toContain("Server onbeschikbaar");
    expect(texts).toContain("Probeer het later");
  });

  it("does not show server error text paragraphs when serverErrorTexts emits empty array", () => {
    setup("", "", []);

    const nativeElement = fixture.nativeElement as HTMLElement;
    const paragraphs = nativeElement.querySelectorAll("p");
    expect(paragraphs.length).toBe(0);
  });

  it("reads foutmelding and bericht from FoutAfhandelingService on init", () => {
    setup("Technische fout", "Details van de fout", []);

    expect(component["foutmelding"]).toBe("Technische fout");
    expect(component["bericht"]).toBe("Details van de fout");
  });

  it("calls listServerErrorTexts on ReferentieTabelService", () => {
    setup("", "", []);

    expect(referentieTabelService.listServerErrorTexts).toHaveBeenCalled();
  });
});
