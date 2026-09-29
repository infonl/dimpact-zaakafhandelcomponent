/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { render, screen } from "@testing-library/angular";
import { fromPartial } from "src/test-helpers";
import { InformatieObjectenService } from "../../informatie-objecten/informatie-objecten.service";
import { FileFormat } from "../../informatie-objecten/model/file-format";
import { GeneratedType } from "../utils/generated-types";
import { DocumentViewerComponent } from "./document-viewer.component";

const makeDocument = (
  fields: Partial<GeneratedType<"RestEnkelvoudigInformatieobject">> = {},
) =>
  fromPartial<GeneratedType<"RestEnkelvoudigInformatieobject">>({
    uuid: "fakeDocumentUuid",
    versie: 2,
    titel: "fakeTitel",
    beschrijving: "fakeBeschrijving",
    bestandsnaam: "fakeBestandsnaam",
    formaat: FileFormat.PDF,
    ...fields,
  });

describe(DocumentViewerComponent.name, () => {
  const setup = async (
    document: GeneratedType<"RestEnkelvoudigInformatieobject">,
  ) => {
    const getPreviewUrl = jest.fn(
      (uuid: string, versie?: number | null) =>
        `https://fake.preview/${uuid}/${versie}`,
    );

    const { fixture } = await render(DocumentViewerComponent, {
      inputs: { document },
      providers: [
        {
          provide: InformatieObjectenService,
          useValue: fromPartial<InformatieObjectenService>({ getPreviewUrl }),
        },
      ],
    });

    return { fixture, getPreviewUrl };
  };

  it("embeds a PDF preview, titled with the document title", async () => {
    await setup(makeDocument({ formaat: FileFormat.PDF }));

    const pdfPreview = screen.getByTitle("fakeTitel");
    expect(pdfPreview).toHaveAttribute("type", "application/pdf");
    expect(pdfPreview).toHaveAttribute(
      "data",
      "https://fake.preview/fakeDocumentUuid/2",
    );
    expect(screen.queryByRole("img")).not.toBeInTheDocument();
  });

  it.each([FileFormat.JPEG, FileFormat.PNG, FileFormat.GIF])(
    "shows a %s preview as an image described by the document description and titled with the file name",
    async (formaat) => {
      await setup(makeDocument({ formaat }));

      const imagePreview = screen.getByRole("img", {
        name: "fakeBeschrijving",
      });
      expect(imagePreview).toHaveAttribute(
        "src",
        "https://fake.preview/fakeDocumentUuid/2",
      );
      expect(imagePreview).toHaveAttribute("title", "fakeBestandsnaam");
      expect(screen.queryByTitle("fakeTitel")).not.toBeInTheDocument();
    },
  );

  it.each([FileFormat.BMP, FileFormat.DOCX, FileFormat.TEXT])(
    "shows no preview for %s",
    async (formaat) => {
      const { getPreviewUrl } = await setup(makeDocument({ formaat }));

      expect(screen.queryByRole("img")).not.toBeInTheDocument();
      expect(screen.queryByTitle("fakeTitel")).not.toBeInTheDocument();
      expect(getPreviewUrl).not.toHaveBeenCalled();
    },
  );

  it("asks for the preview of the given document version once", async () => {
    const { getPreviewUrl } = await setup(makeDocument());

    expect(getPreviewUrl).toHaveBeenCalledTimes(1);
    expect(getPreviewUrl).toHaveBeenCalledWith("fakeDocumentUuid", 2);
  });

  it("shows the preview of the new document once the document changes", async () => {
    const { fixture, getPreviewUrl } = await setup(makeDocument());

    fixture.componentRef.setInput(
      "document",
      makeDocument({
        uuid: "fakeOtherDocumentUuid",
        versie: 3,
        formaat: FileFormat.PNG,
      }),
    );
    fixture.detectChanges();

    expect(getPreviewUrl).toHaveBeenLastCalledWith("fakeOtherDocumentUuid", 3);
    expect(
      screen.getByRole("img", { name: "fakeBeschrijving" }),
    ).toHaveAttribute("src", "https://fake.preview/fakeOtherDocumentUuid/3");
    expect(screen.queryByTitle("fakeTitel")).not.toBeInTheDocument();
  });

  it("removes the preview once the document changes into one without a preview", async () => {
    const { fixture } = await setup(makeDocument({ formaat: FileFormat.PDF }));

    fixture.componentRef.setInput(
      "document",
      makeDocument({ formaat: FileFormat.DOCX }),
    );
    fixture.detectChanges();

    expect(screen.queryByTitle("fakeTitel")).not.toBeInTheDocument();
  });

  it("shows a preview once the document changes from one without a preview into one with", async () => {
    const { fixture, getPreviewUrl } = await setup(
      makeDocument({ formaat: FileFormat.DOCX }),
    );

    fixture.componentRef.setInput(
      "document",
      makeDocument({ formaat: FileFormat.PDF }),
    );
    fixture.detectChanges();

    expect(getPreviewUrl).toHaveBeenCalledTimes(1);
    expect(screen.getByTitle("fakeTitel")).toHaveAttribute(
      "data",
      "https://fake.preview/fakeDocumentUuid/2",
    );
  });
});
