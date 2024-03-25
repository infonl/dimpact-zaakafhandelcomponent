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
});
