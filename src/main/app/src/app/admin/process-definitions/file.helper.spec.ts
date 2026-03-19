/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { readFileContent } from "./file.helper";

describe(readFileContent.name, () => {
  let mockFileReader: {
    readAsText: jest.Mock;
    onload: (() => void) | null;
    onerror: ((event: unknown) => void) | null;
    result: string | null;
  };

  const originalFileReader = global.FileReader;

  beforeEach(() => {
    mockFileReader = {
      readAsText: jest.fn(),
      onload: null,
      onerror: null,
      result: null,
    };
    global.FileReader = jest.fn(
      () => mockFileReader,
    ) as unknown as typeof FileReader;
  });

  afterEach(() => {
    global.FileReader = originalFileReader;
  });

  it("should resolve with the file text content", async () => {
    const file = new File(["hello world"], "test.json");
    mockFileReader.result = "hello world";

    const promise = readFileContent(file);
    mockFileReader.onload?.();

    await expect(promise).resolves.toBe("hello world");
    expect(mockFileReader.readAsText).toHaveBeenCalledWith(file);
  });

  it("should reject when FileReader encounters an error", async () => {
    const file = new File(["content"], "test.json");
    const error = new ProgressEvent("error");

    const promise = readFileContent(file);
    mockFileReader.onerror?.(error);

    await expect(promise).rejects.toBe(error);
  });
});
