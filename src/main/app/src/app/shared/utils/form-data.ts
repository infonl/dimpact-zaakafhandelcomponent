type FormDataMappingFunction<T> = (
  keyValuePair: readonly [string, T],
) => readonly [string, string | Blob] | readonly [string, Blob, string];

type FormDataMapper<T> = Partial<{
  [K in keyof T]: T[K] extends string | Blob | { toString: () => string }
    ? FormDataMappingFunction<T[K]> | true
    : FormDataMappingFunction<T[K]>;
}>;

function parseFormValue(v: unknown) {
  if (!v) return false;
  if (typeof v === "string" || v instanceof Blob) return v;
  if (v.toString && typeof v.toString === "function") return v.toString();
  return false;
}

export function createFormData<T extends {}>(
  obj: T,
  mapper: FormDataMapper<T>,
): FormData {
  const formData = new FormData();
  Object.entries(mapper).forEach(
    ([key, mappingFunction]: [
      string,
      FormDataMappingFunction<unknown> | true,
    ]) => {
      const value = obj[key];
      if (typeof mappingFunction === "function") {
        const [newKey, stringOrBlob, maybeFileName] = mappingFunction([
          key,
          value,
        ]);
        if (stringOrBlob instanceof Blob) {
          formData.append(newKey, stringOrBlob, maybeFileName);
        } else {
          formData.append(newKey, stringOrBlob);
        }
      } else {
        const parsed = parseFormValue(value);
        parsed && formData.append(key, parsed);
      }
    },
  );
  return formData;
}
