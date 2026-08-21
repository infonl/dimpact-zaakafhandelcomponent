/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { fromPartial } from "src/test-helpers";
import { promptForSaveLocation, writeFile } from "./save-file";

describe(promptForSaveLocation.name, () => {
  afterEach(() => {
    delete window.showSaveFilePicker;
  });

  it("should report the file the user chose", async () => {
    const fileHandle = fromPartial<FileSystemFileHandle>({ name: "fake.zip" });
    window.showSaveFilePicker = jest.fn().mockResolvedValue(fileHandle);

    await expect(
      promptForSaveLocation({ suggestedName: "fake.zip" }),
    ).resolves.toBe(fileHandle);
    expect(window.showSaveFilePicker).toHaveBeenCalledWith({
      suggestedName: "fake.zip",
    });
  });

  it("should report no file in a browser without the File System Access API", async () => {
    await expect(
      promptForSaveLocation({ suggestedName: "fake.zip" }),
    ).resolves.toBeNull();
  });

  it("should reject when the user closes the dialog", async () => {
    const abortError = new DOMException("fakeAbortMessage", "AbortError");
    window.showSaveFilePicker = jest.fn().mockRejectedValue(abortError);

    await expect(
      promptForSaveLocation({ suggestedName: "fake.zip" }),
    ).rejects.toBe(abortError);
  });
});

describe(writeFile.name, () => {
  it("should write the content to the file and close it", async () => {
    const writableStream = { write: jest.fn(), close: jest.fn() };
    const fileHandle = fromPartial<FileSystemFileHandle>({
      createWritable: jest.fn().mockResolvedValue(writableStream),
    });
    const content = new Blob(["fakeContent"]);

    await writeFile(fileHandle, content);

    expect(writableStream.write).toHaveBeenCalledWith(content);
    expect(writableStream.close).toHaveBeenCalled();
  });
});
