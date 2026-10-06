/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TestBed } from "@angular/core/testing";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { I18nLabelPipe } from "./i18n-label.pipe";

describe(I18nLabelPipe.name, () => {
  let pipe: I18nLabelPipe;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot()],
      providers: [I18nLabelPipe],
    });
    const translateService = TestBed.inject(TranslateService);
    translateService.setTranslation("nl", {
      "objecttype.adres": "Adres",
      "zaak-geometrie": "Zaakgeometrie",
      behandelaar: "Behandelaar",
    });
    translateService.use("nl");
    pipe = TestBed.inject(I18nLabelPipe);
  });

  it.each([
    ["objecttype.ADRES", "Adres"],
    ["zaakGeometrie", "Zaakgeometrie"],
    ["Behandelaar", "Behandelaar"],
  ])(
    "should translate the label %p that the backend sends as an i18n key in another casing",
    (label, translation) => {
      expect(pipe.transform(label)).toBe(translation);
    },
  );

  it.each(["Zaakcoördinator", "Belanghebbende", "objecttype.Boom (overige)"])(
    "should show the free-text label %p unchanged when no i18n key matches it",
    (label) => {
      expect(pipe.transform(label)).toBe(label);
    },
  );

  it.each([null, undefined, ""])(
    "should show nothing for the missing label %p",
    (label) => {
      expect(pipe.transform(label)).toBe("");
    },
  );
});
