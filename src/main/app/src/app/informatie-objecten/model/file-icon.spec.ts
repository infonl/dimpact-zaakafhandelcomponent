/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
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
          icon: "unknown_document",
          color: undefined,
        });
      },
    );

    it.each([["foo.unknowntype"], ["foo.nonexistent"]])(
      "should return an unknown type when the filetype is %p",
      (fileName) => {
        expect(FileIcon.getIconByBestandsnaam(fileName)).toEqual({
          type: "unknown",
          icon: "unknown_document",
          color: undefined,
        });
      },
    );

    it.each([
      ["foo.pdf", { color: "darkred", icon: "picture_as_pdf", type: "pdf" }],
      ["foo.rtf", { icon: "description", type: "rtf" }],
    ])('for the file "%s" it should return %p', (fileName, expected) => {
      expect(FileIcon.getIconByBestandsnaam(fileName)).toEqual(expected);
    });
  });
});
