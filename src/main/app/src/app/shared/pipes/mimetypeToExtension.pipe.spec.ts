/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { FileFormat } from "src/app/informatie-objecten/model/file-format";
import { MimetypeToExtensionPipe } from "./mimetypeToExtension.pipe";

describe("Mimetype to extension pipe", () => {
  it("should create the correct key values for all expected strings", () => {
    const pipe = new MimetypeToExtensionPipe();

    for (const key of Object.values(FileFormat)) {
      expect(pipe.transform(key)).toBe(
        pipe.fileFormatExtesions[key as FileFormat],
      );
    }
  });

  it("should safely parse the provided value if the mimtype does not exist in list", () => {
    const pipe = new MimetypeToExtensionPipe();

    expect(pipe.transform("not a key")).toBe("not a key");
  });
});
