/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { appendFileToFormData } from "./file-upload";

describe(appendFileToFormData.name, () => {
  function getAppendedFile(formData: FormData) {
    return formData.get("file") as File;
  }

  it("should append a non-eml file under the 'file' key with its original media type", () => {
    const formData = new FormData();
    const file = new Blob(["pdf-content"], { type: "application/pdf" });

    appendFileToFormData(formData, file, "report.pdf");

    const appendedFile = getAppendedFile(formData);
    expect(appendedFile.name).toBe("report.pdf");
    expect(appendedFile.type).toBe("application/pdf");
  });

  it("should force eml files to application/octet-stream", () => {
    const formData = new FormData();
    const file = new Blob(["email-content"], { type: "message/rfc822" });

    appendFileToFormData(formData, file, "mail.eml");

    const appendedFile = getAppendedFile(formData);
    expect(appendedFile.name).toBe("mail.eml");
    expect(appendedFile.type).toBe("application/octet-stream");
  });

  it("should match the .eml extension case-insensitively", () => {
    const formData = new FormData();
    const file = new Blob(["email-content"], { type: "message/rfc822" });

    appendFileToFormData(formData, file, "MAIL.EML");

    expect(getAppendedFile(formData).type).toBe("application/octet-stream");
  });

  it("should preserve the file content", () => {
    const formData = new FormData();
    const file = new Blob(["hello"], { type: "text/plain" });

    appendFileToFormData(formData, file, "note.txt");

    const appendedFile = getAppendedFile(formData);
    expect(appendedFile).toBeInstanceOf(Blob);
    expect(appendedFile.size).toBe(file.size);
  });
});
