/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { appendFileToFormData, toDocumentFormData } from "./file-upload";

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

describe(toDocumentFormData.name, () => {
  it("appends the file under 'file', named after bestandsnaam, with .eml forced to application/octet-stream", () => {
    const emlFile = new File(["Subject: Test EML"], "test-email.eml", {
      type: "message/rfc822",
    });

    const formData = toDocumentFormData({
      bestand: emlFile,
      bestandsnaam: emlFile.name,
    });

    const appendedFile = formData.get("file") as File;
    expect(appendedFile.name).toBe("test-email.eml");
    expect(appendedFile.type).toBe("application/octet-stream");
    expect(appendedFile.size).toBe(emlFile.size);
    expect(formData.get("bestandsnaam")).toBe("test-email.eml");
    expect(formData.has("bestand")).toBe(false);
  });

  it("formats the document dates the way the backend parses them", () => {
    const formData = toDocumentFormData({
      creatiedatum: "2025-09-24T11:59:23.111Z",
      ontvangstdatum: "2025-09-25T00:00:00.000Z",
      verzenddatum: "2025-09-26T00:00:00.000Z",
    });

    for (const key of ["creatiedatum", "ontvangstdatum", "verzenddatum"]) {
      expect(formData.get(key)).toMatch(
        /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}[+-]\d{2}:\d{2}$/,
      );
    }
    expect(formData.get("creatiedatum")).toMatch(/^2025-09-24/);
  });

  it("sends objects as JSON and everything else as text, leaving out fields without a value", () => {
    const formData = toDocumentFormData({
      taal: { id: "nl", naam: "Nederlands" },
      titel: "Title",
      status: "DEFINITIEF",
      versie: 2,
      beschrijving: null,
      toelichting: undefined,
    });

    expect(JSON.parse(formData.get("taal") as string)).toEqual({
      id: "nl",
      naam: "Nederlands",
    });
    expect(formData.get("titel")).toBe("Title");
    expect(formData.get("status")).toBe("DEFINITIEF");
    expect(formData.get("versie")).toBe("2");
    expect(formData.has("beschrijving")).toBe(false);
    expect(formData.has("toelichting")).toBe(false);
  });
});
