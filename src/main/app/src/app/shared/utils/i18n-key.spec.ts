/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { isCamelCaseSegment, toI18nKey } from "./i18n-key";

describe(toI18nKey.name, () => {
  it("should write a camelCase key in kebab-case", () => {
    expect(toI18nKey("zaakIdentificatie")).toBe("zaak-identificatie");
  });

  it("should write only the last segment of a dotted key in kebab-case", () => {
    expect(toI18nKey("healthCheck.zaaktype.geenRolOverigeAanwezig")).toBe(
      "healthCheck.zaaktype.geen-rol-overige-aanwezig",
    );
  });

  it("should give every capital of a run of capitals its own word", () => {
    expect(toI18nKey("validators.bsnOrVes")).toBe("validators.bsn-or-ves");
  });

  it.each([
    "omschrijving",
    "msg.document.verwijderen.inbox.niet-verwijderd",
    "objecttype.ADRES",
    "vertrouwelijkheidaanduiding.ZEER_GEHEIM",
    "betrokkeneRoltype.-kies-",
    "",
  ])(
    "should leave %p untouched, as its last segment is not camelCase",
    (key) => {
      expect(toI18nKey(key)).toBe(key);
    },
  );
});

describe(isCamelCaseSegment.name, () => {
  it.each(["zaakIdentificatie", "matDatepickerMax", "bsnOrVes", "a1B2"])(
    "should recognise %p as camelCase",
    (segment) => {
      expect(isCamelCaseSegment(segment)).toBe(true);
    },
  );

  it.each([
    "zaak",
    "zaak-identificatie",
    "ADRES",
    "BEPERKT_OPENBAAR",
    "ZaakIdentificatie",
    "Ingetrokken door overheid",
  ])("should not recognise %p as camelCase", (segment) => {
    expect(isCamelCaseSegment(segment)).toBe(false);
  });
});
