/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { isI18nKey, toI18nKey } from "./i18n-key";

describe(toI18nKey.name, () => {
  it("should write a camelCase key in kebab-case", () => {
    expect(toI18nKey("zaakIdentificatie")).toBe("zaak-identificatie");
  });

  it("should write every segment of a dotted key in kebab-case", () => {
    expect(toI18nKey("healthCheck.zaaktype.geenRolOverigeAanwezig")).toBe(
      "health-check.zaaktype.geen-rol-overige-aanwezig",
    );
  });

  it("should write an UPPER_SNAKE enum value in lowercase kebab-case", () => {
    expect(toI18nKey("vertrouwelijkheidaanduiding.ZEER_GEHEIM")).toBe(
      "vertrouwelijkheidaanduiding.zeer-geheim",
    );
  });

  it("should write a snake_case segment in kebab-case", () => {
    expect(toI18nKey("informatieobject.status.in_bewerking")).toBe(
      "informatieobject.status.in-bewerking",
    );
  });

  it("should write a PascalCase segment in lowercase kebab-case", () => {
    expect(toI18nKey("AdresseerbaarObject.Ligplaats")).toBe(
      "adresseerbaar-object.ligplaats",
    );
  });

  it("should turn a value with spaces and punctuation into a slug, so a status from an external register can be looked up", () => {
    expect(
      toI18nKey("bagObject.pand.status.Pand in gebruik (niet ingemeten)"),
    ).toBe("bag-object.pand.status.pand-in-gebruik-niet-ingemeten");
  });

  it("should drop the dashes around a placeholder segment", () => {
    expect(toI18nKey("betrokkeneRoltype.-kies-")).toBe(
      "betrokkene-roltype.kies",
    );
  });

  it.each([
    "omschrijving",
    "msg.document.verwijderen.inbox.niet-verwijderd",
    "objecttype.adres",
    "health-check.synchroniseer.ztc.uitleg.1",
    "",
  ])("should leave %p untouched, as it is already an i18n key", (key) => {
    expect(toI18nKey(key)).toBe(key);
  });
});

describe(isI18nKey.name, () => {
  it.each([
    "zaak",
    "zaak-identificatie",
    "health-check.synchroniseer.ztc.uitleg.1",
    "bag-object.pand.status.pand-in-gebruik-niet-ingemeten",
  ])(
    "should accept %p, which is lowercase kebab-case in every segment",
    (key) => {
      expect(isI18nKey(key)).toBe(true);
    },
  );

  it.each([
    "zaakIdentificatie",
    "objecttype.ADRES",
    "informatieobject.status.in_bewerking",
    "AdresseerbaarObject.Ligplaats",
    "Verblijfsobject gevormd",
    "betrokkene-roltype.-kies-",
    "zaak--identificatie",
    "zaak.",
    "",
  ])("should reject %p", (key) => {
    expect(isI18nKey(key)).toBe(false);
  });
});
