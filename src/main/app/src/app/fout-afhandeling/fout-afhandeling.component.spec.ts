/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ComponentFixture, TestBed } from "@angular/core/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { of } from "rxjs";
import { ReferentieTabelService } from "../admin/referentie-tabel.service";
import { FoutAfhandelingService } from "./fout-afhandeling.service";
import { FoutAfhandelingComponent } from "./fout-afhandeling.component";

describe(FoutAfhandelingComponent.name, () => {
  let fixture: ComponentFixture<FoutAfhandelingComponent>;
  let component: FoutAfhandelingComponent;
  let foutAfhandelingServiceMock: Pick<FoutAfhandelingService, "foutmelding" | "bericht">;
  let referentieTabelServiceMock: Pick<ReferentieTabelService, "listServerErrorTexts">;

  const setup = (
    foutmelding: string,
    bericht: string,
    serverErrorTexts: string[],
  ) => {
    foutAfhandelingServiceMock = { foutmelding, bericht };
    referentieTabelServiceMock = {
      listServerErrorTexts: jest.fn().mockReturnValue(of(serverErrorTexts)),
    };

    TestBed.configureTestingModule({
      imports: [FoutAfhandelingComponent, NoopAnimationsModule, TranslateModule.forRoot()],
      providers: [
        { provide: FoutAfhandelingService, useValue: foutAfhandelingServiceMock },
        { provide: ReferentieTabelService, useValue: referentieTabelServiceMock },
      ],
    });

    fixture = TestBed.createComponent(FoutAfhandelingComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  };

  it("displays foutmelding in heading when set", () => {
    setup("Er is een fout opgetreden", "", []);

    const heading = fixture.nativeElement.querySelector("h2");
    expect(heading.textContent).toContain("Er is een fout opgetreden");
  });

  it("falls back to default translation key in heading when foutmelding is empty", () => {
    setup("", "", []);

    // TranslateModule.forRoot() returns the key itself as translation value
    const heading = fixture.nativeElement.querySelector("h2");
    expect(heading.textContent).toContain("error-card.title.default");
  });

  it("shows bericht paragraph when bericht is non-empty", () => {
    setup("", "Neem contact op met de beheerder", []);

    const paragraphs: NodeListOf<HTMLParagraphElement> = fixture.nativeElement.querySelectorAll("p");
    const berichtTexts = Array.from(paragraphs).map((p) => p.textContent ?? "");
    expect(berichtTexts.some((t) => t.includes("Neem contact op met de beheerder"))).toBe(true);
  });

  it("does not show bericht paragraph when bericht is empty", () => {
    setup("Fout", "", []);

    const paragraphs: NodeListOf<HTMLParagraphElement> = fixture.nativeElement.querySelectorAll("p");
    expect(paragraphs.length).toBe(0);
  });

  it("shows server error text paragraphs when serverErrorTexts emits values", () => {
    setup("", "", ["Server onbeschikbaar", "Probeer het later"]);

    const paragraphs: NodeListOf<HTMLParagraphElement> = fixture.nativeElement.querySelectorAll("p");
    const texts = Array.from(paragraphs).map((p) => p.textContent?.trim() ?? "");
    expect(texts).toContain("Server onbeschikbaar");
    expect(texts).toContain("Probeer het later");
  });

  it("does not show server error text paragraphs when serverErrorTexts emits empty array", () => {
    setup("", "", []);

    const paragraphs: NodeListOf<HTMLParagraphElement> = fixture.nativeElement.querySelectorAll("p");
    expect(paragraphs.length).toBe(0);
  });

  it("reads foutmelding and bericht from FoutAfhandelingService on init", () => {
    setup("Technische fout", "Details van de fout", []);

    expect(component["foutmelding"]).toBe("Technische fout");
    expect(component["bericht"]).toBe("Details van de fout");
  });

  it("calls listServerErrorTexts on ReferentieTabelService", () => {
    setup("", "", []);

    expect(referentieTabelServiceMock.listServerErrorTexts).toHaveBeenCalled();
  });
});
