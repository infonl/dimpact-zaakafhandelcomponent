/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { FileIcon } from "./file-icon";

describe(FileIcon.name, () => {
  describe(FileIcon.getIconByBestandsnaam.name, () => {
    it.each([[{}], [undefined], [null], [new Error("test")], [5]])(
      "should return an unknown type when the input is %p",
      (fileName) => {
        expect(FileIcon.getIconByBestandsnaam(fileName)).toEqual({
          type: "unknown",
          icon: "fa-file-circle-question",
          color: undefined,
        });
      },
    );

    it.each([["foo.unknowntype"], ["foo.nonexistent"]])(
      "should return an unknown type when the filetype is %p",
      (fileName) => {
        expect(FileIcon.getIconByBestandsnaam(fileName)).toEqual({
          type: "unknown",
          icon: "fa-file-circle-question",
          color: undefined,
        });
      },
    );

    it.each([
      ["foo.pdf", { color: "darkred", icon: "fa-file-pdf", type: "pdf" }],
      ["foo.rtf", { icon: "fa-file-word", type: "rtf" }],
    ])('for the file "%s" it should return %p', (fileName, expected) => {
      expect(FileIcon.getIconByBestandsnaam(fileName)).toEqual(expected);
    });
  });
});
