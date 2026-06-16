/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

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
