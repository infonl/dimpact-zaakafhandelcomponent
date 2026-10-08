/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TestBed } from "@angular/core/testing";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { render, screen } from "@testing-library/angular";
import { DocumentIconComponent } from "./document-icon.component";

describe(DocumentIconComponent.name, () => {
  const setup = async (bestandsnaam?: string) => {
    const { fixture } = await render(DocumentIconComponent, {
      inputs: { bestandsnaam },
      imports: [TranslateModule.forRoot()],
    });

    const translateService = TestBed.inject(TranslateService);
    translateService.setTranslation("nl", {
      bestandstype: "{{type}}-bestand",
      "bestandstype.onbekend": "Onbekend bestand",
    });
    translateService.use("nl");
    fixture.detectChanges();

    return { fixture };
  };

  it.each([
    ["fakeBestand.pdf", "picture_as_pdf", "PDF-bestand", "darkred"],
    ["fakeBestand.docx", "description", "DOCX-bestand", "blue"],
    ["fakeBestand.xlsx", "table", "XLSX-bestand", "green"],
    ["fakeBestand.pptx", "slideshow", "PPTX-bestand", "red"],
    ["fakeBestand.eml", "mail", "EML-bestand", "goldenrod"],
  ])(
    "shows %s as the %s icon, titled %s and coloured %s",
    async (bestandsnaam, icon, title, color) => {
      await setup(bestandsnaam);

      const documentIcon = screen.getByTitle(title);
      expect(documentIcon).toHaveTextContent(icon);
      expect(documentIcon).toHaveStyle({ color });
    },
  );

  it.each([
    ["fakeBestand.png", "image", "PNG-bestand"],
    ["fakeBestand.txt", "text_snippet", "TXT-bestand"],
    ["fakeBestand.mp4", "video_file", "MP4-bestand"],
  ])(
    "shows %s as the uncoloured %s icon, titled %s",
    async (bestandsnaam, icon, title) => {
      await setup(bestandsnaam);

      const documentIcon = screen.getByTitle(title);
      expect(documentIcon).toHaveTextContent(icon);
      expect(documentIcon).not.toHaveAttribute(
        "style",
        expect.stringContaining("color"),
      );
    },
  );

  it("picks the icon by the last extension of the file name", async () => {
    await setup("fakeBestand.docx.pdf");

    expect(screen.getByTitle("PDF-bestand")).toHaveTextContent(
      "picture_as_pdf",
    );
  });

  it.each([
    ["an unknown extension", "fakeBestand.xyz"],
    ["no extension", "fakeBestand"],
    ["no file name", undefined],
  ])(
    "shows the unknown document icon for a file name with %s",
    async (_, bestandsnaam) => {
      await setup(bestandsnaam);

      expect(screen.getByTitle("Onbekend bestand")).toHaveTextContent(
        "unknown_document",
      );
    },
  );

  it("shows the icon of the new file name once the file name changes", async () => {
    const { fixture } = await setup("fakeBestand.pdf");

    fixture.componentRef.setInput("bestandsnaam", "fakeBestand.xlsx");
    fixture.detectChanges();

    const documentIcon = screen.getByTitle("XLSX-bestand");
    expect(documentIcon).toHaveTextContent("table");
    expect(documentIcon).toHaveStyle({ color: "green" });
  });
});
