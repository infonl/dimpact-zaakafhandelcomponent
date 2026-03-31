/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { extractBpmnProcessKey, readFileContent } from "./file.helper";

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

describe(extractBpmnProcessKey.name, () => {
  it("should return the process id from a valid BPMN XML string", () => {
    const bpmn = `<?xml version="1.0" encoding="UTF-8"?>
      <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL">
        <process id="my-process-key" name="My Process"></process>
      </definitions>`;
    expect(extractBpmnProcessKey(bpmn)).toBe("my-process-key");
  });

  it("should return the process id from a real-world Flowable BPMN file with default namespace", () => {
    const bpmn = `<?xml version="1.0" encoding="UTF-8"?>
      <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                   xmlns:flowable="http://flowable.org/bpmn"
                   targetNamespace="http://flowable.org/test">
        <process id="pd-demo-sprint-67" name="Demo" isExecutable="true">
        </process>
      </definitions>`;
    expect(extractBpmnProcessKey(bpmn)).toBe("pd-demo-sprint-67");
  });

  it("should return null when no process element is present", () => {
    const bpmn = `<?xml version="1.0" encoding="UTF-8"?>
      <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"></definitions>`;
    expect(extractBpmnProcessKey(bpmn)).toBeNull();
  });

  it("should return null when the process element has no id attribute", () => {
    const bpmn = `<?xml version="1.0" encoding="UTF-8"?>
      <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL">
        <process name="No Id"></process>
      </definitions>`;
    expect(extractBpmnProcessKey(bpmn)).toBeNull();
  });
});
