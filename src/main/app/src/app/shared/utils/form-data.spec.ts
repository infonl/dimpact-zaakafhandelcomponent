/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { createFormData } from "./form-data";

describe("createFormData", () => {
  it("should preserve key and value for strings if no mapping is specified", () => {
    const formData = createFormData({ key: "value" }, { key: true });
    const entries = Array.from(formData);
    expect(entries).toHaveLength(1);
    const [key, value] = entries[0];
    expect(key).toBe("key");
    expect(value).toBe("value");
  });

  it("should preserve key and value for Blobs if no mapping is specified", () => {
    const file = new File([], "dummy");
    const formData = createFormData({ key: file }, { key: true });
    const entries = Array.from(formData);
    expect(entries).toHaveLength(1);
    const [key, value] = entries[0];
    expect(key).toBe("key");
    expect(value).toBe(file);
    expect((<File>value).name).toBe("dummy");
  });

  it("should preserve key and call toString on value if that method exists and no mapping is specified", () => {
    const formData = createFormData({ key: 5 }, { key: true });
    const entries = Array.from(formData);
    expect(entries).toHaveLength(1);
    const [key, value] = entries[0];
    expect(key).toBe("key");
    expect(value).toBe("5");
  });

  it("should skip keys that are not specified in the mapper", () => {
    const formData = createFormData(
      { key: "value", skip: "me-please" },
      { key: true },
    );
    const entries = Array.from(formData);
    expect(entries).toHaveLength(1);
    const [key] = entries[0];
    expect(key).toBe("key");
  });

  it("should call the mapping function if specified", () => {
    const formData = createFormData(
      { key: "value" },
      { key: () => ["new-key", "new-value"] },
    );
    const entries = Array.from(formData);
    expect(entries).toHaveLength(1);
    const [key, value] = entries[0];
    expect(key).toBe("new-key");
    expect(value).toBe("new-value");
  });

  it("should skip keys that are null, undefined or an empty string", () => {
    const formData = createFormData(
      { emptyString: "", null: null, undefined: undefined },
      // @ts-expect-error
      { emptyString: true, null: true, undefined: true },
    );
    const entries = Array.from(formData);
    expect(entries).toHaveLength(0);
  });

  it("should overwrite filename if specified in the mapper", () => {
    const file = new File([], "replace-me");
    const formData = createFormData(
      { file },
      { file: ([k, v]) => [k, v, "new-file-name"] },
    );
    const entries = Array.from(formData);
    expect(entries).toHaveLength(1);
    const [key, value] = entries[0];
    expect(key).toBe("file");
    expect(value).toBeInstanceOf(File);
    expect((<File>value).name).toBe("new-file-name");
  });

  it("should handle arrays without mapping functions", () => {
    const formData = createFormData({ key: [1, 2] }, { key: true });
    const entries = Array.from(formData);
    expect(entries).toHaveLength(2);
    const [[key1, value1], [key2, value2]] = entries;
    expect(key1).toBe("key");
    expect(value1).toBe("1");
    expect(key2).toBe("key");
    expect(value2).toBe("2");
  });

  it("should handle arrays with mapping functions", () => {
    const file1 = new File([], "replace-me-1");
    const file2 = new File([], "replace-me-2");
    const formData = createFormData(
      { file: [file1, file2] },
      { file: ([k, v]) => [k, v, "new-file-name"] },
    );
    const entries = Array.from(formData);
    expect(entries).toHaveLength(2);
    const [[key1, value1], [key2, value2]] = entries;
    expect(key1).toBe("file");
    expect(value1).toBeInstanceOf(File);
    expect((<File>value1).name).toBe("new-file-name");
    expect(key2).toBe("file");
    expect(value2).toBeInstanceOf(File);
    expect((<File>value2).name).toBe("new-file-name");
  });
});
