/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { VertrouwelijkaanduidingToTranslationKeyPipe } from "./vertrouwelijkaanduiding-to-translation-key.pipe";

describe("Vertrouwelijkaanduiding-to-translation-pipe", () => {
  it("should create the correct key values for all expected strings", () => {
    const results = [
      "vertrouwelijkheidaanduiding.openbaar",
      "vertrouwelijkheidaanduiding.beperkt-openbaar",
      "vertrouwelijkheidaanduiding.intern",
      "vertrouwelijkheidaanduiding.zaakvertrouwelijk",
      "vertrouwelijkheidaanduiding.vertrouwelijk",
      "vertrouwelijkheidaanduiding.confidentieel",
      "vertrouwelijkheidaanduiding.geheim",
      "vertrouwelijkheidaanduiding.zeer-geheim",
    ];
    const pipe = new VertrouwelijkaanduidingToTranslationKeyPipe();

    for (const key of VertrouwelijkaanduidingToTranslationKeyPipe.expectedKeys) {
      expect(pipe.transform(key)).toBe(
        `${results[VertrouwelijkaanduidingToTranslationKeyPipe.expectedKeys.indexOf(key)]}`,
      );
    }
  });

  it("should throw error when any other strings are provided", () => {
    const pipe = new VertrouwelijkaanduidingToTranslationKeyPipe();

    // @ts-expect-error force error state
    expect(() => pipe.transform("not a key")).toThrow(
      "Unexpected vertrouwelijkheidaanduiding: not a key",
    );
  });
});
