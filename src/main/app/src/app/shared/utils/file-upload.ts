/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import moment from "moment";

const DATE_FIELDS = ["creatiedatum", "ontvangstdatum", "verzenddatum"];

export function toDocumentFormData(
  document: Record<string, unknown>,
): FormData {
  const formData = new FormData();
  const filename = String(document.bestandsnaam ?? "");
  for (const [key, value] of Object.entries(document)) {
    if (value === undefined || value === null) continue;
    if (value instanceof Blob) {
      appendFileToFormData(formData, value, filename);
    } else if (DATE_FIELDS.includes(key)) {
      formData.append(key, moment(String(value)).format("YYYY-MM-DDThh:mmZ"));
    } else if (typeof value === "object") {
      formData.append(key, JSON.stringify(value));
    } else {
      formData.append(key, String(value));
    }
  }
  return formData;
}

/**
 * Appends an upload file to a `FormData` under the "file" key, forcing .eml files to
 * "application/octet-stream": browsers send .eml as "message/rfc822", which RESTEasy
 * won't bind to a byte[]. The real media type still travels in the "formaat"/"type" field.
 */
export function appendFileToFormData(
  formData: FormData,
  file: Blob,
  filename: string,
): void {
  if (filename.toLowerCase().endsWith(".eml")) {
    formData.append(
      "file",
      new Blob([file], { type: "application/octet-stream" }),
      filename,
    );
  } else {
    formData.append("file", file, filename);
  }
}
