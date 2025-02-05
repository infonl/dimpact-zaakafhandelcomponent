/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import FormData from "form-data";

type FormCompatible = string | Blob | { toString: () => string };

type FormDataMappingFunction<T> = (
  keyValuePair: readonly [string, T],
) => readonly [string, string | Blob] | readonly [string, Blob, string];

type FormDataMapper<T> = Partial<{
  [K in keyof T]: T[K] extends FormCompatible[]
    ? FormDataMappingFunction<T[K][number]> | true
    : T[K] extends FormCompatible
      ? FormDataMappingFunction<T[K]> | true
      : T[K] extends Array<infer A>
        ? FormDataMappingFunction<A>
        : FormDataMappingFunction<T[K]>;
}>;

function parseFormValue(v: unknown) {
  if (v === undefined || v === null) return false;
  if (typeof v === "string" || v instanceof Blob) return v;
  return v.toString();
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
      if (Array.isArray(value)) {
        value.forEach(add);
      } else {
        add(value);
      }
      function add(value: unknown) {
        if (typeof mappingFunction === "function") {
          const [newKey, stringOrBlob, maybeFileName] = mappingFunction([
            key,
            value,
          ]);
          if (stringOrBlob instanceof Blob) {
            formData.append(newKey, stringOrBlob, maybeFileName);
          } else if (stringOrBlob) {
            formData.append(newKey, stringOrBlob);
          }
        } else {
          const parsed = parseFormValue(value);
          parsed && formData.append(key, parsed);
        }
      }
    },
  );
  return formData;
}
