/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { VertrouwelijkaanduidingToTranslationKeyPipe } from "./vertrouwelijkaanduiding-to-translation-key.pipe";

describe("Vertrouwelijkaanduiding-to-translation-pipe", () => {
  it("should create the correct key values for all expected strings", () => {
    const results = [
      "vertrouwelijkheidaanduiding.OPENBAAR",
      "vertrouwelijkheidaanduiding.BEPERKT_OPENBAAR",
      "vertrouwelijkheidaanduiding.INTERN",
      "vertrouwelijkheidaanduiding.ZAAKVERTROUWELIJK",
      "vertrouwelijkheidaanduiding.VERTROUWELIJK",
      "vertrouwelijkheidaanduiding.CONFIDENTIEEL",
      "vertrouwelijkheidaanduiding.GEHEIM",
      "vertrouwelijkheidaanduiding.ZEER_GEHEIM",
    ];
    const pipe = new VertrouwelijkaanduidingToTranslationKeyPipe();

    for (const key of pipe.expectedKeys) {
      expect(pipe.transform(key)).toBe(
        `${results[pipe.expectedKeys.indexOf(key)]}`,
      );
    }
  });

  it("should throw error when any other strings are provided", () => {
    const pipe = new VertrouwelijkaanduidingToTranslationKeyPipe();

    expect(() => pipe.transform("not a key")).toThrow(
      "Unexpected vertrouwelijkheidaanduiding: not a key",
    );
  });
});
