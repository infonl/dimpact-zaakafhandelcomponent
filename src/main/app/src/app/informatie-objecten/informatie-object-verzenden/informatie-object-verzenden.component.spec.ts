/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  provideHttpClient,
  withInterceptorsFromDi,
} from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { provideZonelessChangeDetection } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { provideMomentDateAdapter } from "@angular/material-moment-adapter";
import { MatDrawer } from "@angular/material/sidenav";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { fireEvent, render, screen, within } from "@testing-library/angular";
import userEvent, { UserEvent } from "@testing-library/user-event";
import { EMPTY } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../setupJest";
import { UtilService } from "../../core/service/util.service";
import { FoutAfhandelingService } from "../../fout-afhandeling/fout-afhandeling.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { InformatieObjectVerzendenComponent } from "./informatie-object-verzenden.component";

const VERZENDEN_URL = "/rest/informatieobjecten/informatieobjecten/verzenden";

const documents = [
  fromPartial<GeneratedType<"RestEnkelvoudigInformatieobject">>({
    uuid: "fakeDocumentUuid1",
    titel: "Document 1",
    bestandsnaam: "document-1.pdf",
  }),
  fromPartial<GeneratedType<"RestEnkelvoudigInformatieobject">>({
    uuid: "fakeDocumentUuid2",
    titel: "Document 2",
    bestandsnaam: "document-2.pdf",
  }),
];

const zaak = fromPartial<GeneratedType<"RestZaak">>({ uuid: "fakeZaakUuid1" });

describe(InformatieObjectVerzendenComponent.name, () => {
  let fixture: ComponentFixture<InformatieObjectVerzendenComponent>;
  let rerenderWithZaak: (zaak: GeneratedType<"RestZaak">) => Promise<void>;
  let httpTestingController: HttpTestingController;
  let sideNav: MatDrawer;
  let documentSent: jest.Mock;
  let openSnackbar: jest.SpyInstance;
  let foutAfhandelen: jest.SpyInstance;

  let user: UserEvent;

  jest.setTimeout(20_000);

  async function setup() {
    user = userEvent.setup({ delay: null });
    sideNav = fromPartial<MatDrawer>({
      close: jest.fn().mockResolvedValue(undefined),
    });
    documentSent = jest.fn();

    const {
      fixture: renderedFixture,
      rerender,
      detectChanges,
    } = await render(InformatieObjectVerzendenComponent, {
      inputs: { zaak, sideNav },
      on: { documentSent },
      imports: [NoopAnimationsModule, TranslateModule.forRoot()],
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideMomentDateAdapter(),
        provideQueryClient(testQueryClient),
      ],
    });

    fixture = renderedFixture;
    rerenderWithZaak = async (newZaak) => {
      await rerender({ inputs: { zaak: newZaak, sideNav } });
      detectChanges();
    };
    httpTestingController = TestBed.inject(HttpTestingController);
    openSnackbar = jest
      .spyOn(TestBed.inject(UtilService), "openSnackbar")
      .mockImplementation(() => undefined);
    foutAfhandelen = jest
      .spyOn(TestBed.inject(FoutAfhandelingService), "foutAfhandelen")
      .mockReturnValue(EMPTY);
  }

  function documentsRequest(zaakUuid: string) {
    return httpTestingController.expectOne(
      `/rest/informatieobjecten/informatieobjecten/zaak/${zaakUuid}/teVerzenden`,
    );
  }

  async function showDocuments(zaakUuid = "fakeZaakUuid1") {
    await sleep();
    documentsRequest(zaakUuid).flush(documents);
    await sleep();
    fixture.componentRef.changeDetectorRef.markForCheck();
    await fixture.whenStable();
  }

  function field(label: string) {
    return screen.getByLabelText(new RegExp(label, "i"));
  }

  function submitButton() {
    return screen.getByRole("button", { name: "actie.verzenden" });
  }

  async function selectDocument(titel: string) {
    const row = screen.getByRole("row", { name: new RegExp(titel) });
    await user.click(within(row).getByRole("checkbox"));
  }

  it("announces what the drawer is for", async () => {
    await setup();
    await showDocuments();

    expect(screen.getByText("actie.document.verzenden")).toBeVisible();
  });

  it("closes the drawer from the toolbar", async () => {
    await setup();
    await showDocuments();

    await user.click(screen.getByRole("button", { name: "actie.sluiten" }));

    expect(sideNav.close).toHaveBeenCalled();
  });

  it("lists the documents that can be sent for the zaak", async () => {
    await setup();
    await showDocuments();

    expect(screen.getByRole("row", { name: /Document 1/ })).toBeVisible();
    expect(screen.getByRole("row", { name: /Document 2/ })).toBeVisible();
  });

  it("lists the documents of the zaak it is shown for after it changes", async () => {
    await setup();
    await showDocuments();

    await rerenderWithZaak(
      fromPartial<GeneratedType<"RestZaak">>({ uuid: "fakeZaakUuid2" }),
    );
    await sleep();

    expect(documentsRequest("fakeZaakUuid2").request.method).toBe("GET");
  });

  it("keeps the sending disabled until a document is chosen", async () => {
    await setup();
    await showDocuments();

    expect(submitButton()).toBeDisabled();

    await selectDocument("Document 1");

    expect(submitButton()).toBeEnabled();
  });

  it("keeps the sending disabled without a verzenddatum", async () => {
    await setup();
    await showDocuments();

    await selectDocument("Document 1");
    fireEvent.input(field("verzenddatum"), { target: { value: "" } });
    fireEvent.blur(field("verzenddatum"));
    await fixture.whenStable();

    expect(submitButton()).toBeDisabled();
  });

  it("sends the chosen document with the toelichting for the zaak", async () => {
    await setup();
    await showDocuments();

    await selectDocument("Document 1");
    await user.type(field("toelichting"), "Ter kennisgeving");
    await user.click(submitButton());
    await sleep();

    const request = httpTestingController.expectOne(VERZENDEN_URL);
    expect(request.request.method).toBe("POST");
    expect(request.request.body).toMatchObject({
      zaakUuid: "fakeZaakUuid1",
      informatieobjecten: ["fakeDocumentUuid1"],
      toelichting: "Ter kennisgeving",
    });

    request.flush(null);
    await sleep();
  });

  it("reports the document that was sent", async () => {
    await setup();
    await showDocuments();

    await selectDocument("Document 1");
    await user.click(submitButton());
    await sleep();
    httpTestingController.expectOne(VERZENDEN_URL).flush(null);
    await sleep();

    expect(openSnackbar).toHaveBeenCalledWith(
      "msg.document.verzenden.uitgevoerd",
    );
    expect(documentSent).toHaveBeenCalled();
  });

  it("reports the documents that were sent when there is more than one", async () => {
    await setup();
    await showDocuments();

    await selectDocument("Document 1");
    await selectDocument("Document 2");
    await user.click(submitButton());
    await sleep();
    httpTestingController.expectOne(VERZENDEN_URL).flush(null);
    await sleep();

    expect(openSnackbar).toHaveBeenCalledWith(
      "msg.documenten.verzenden.uitgevoerd",
    );
  });

  it("keeps the drawer open when the sending fails", async () => {
    await setup();
    await showDocuments();

    await selectDocument("Document 1");
    await user.click(submitButton());
    await sleep();
    httpTestingController
      .expectOne(VERZENDEN_URL)
      .flush(null, { status: 500, statusText: "Internal Server Error" });
    await sleep();

    expect(foutAfhandelen).toHaveBeenCalled();
    expect(documentSent).not.toHaveBeenCalled();
    expect(sideNav.close).not.toHaveBeenCalled();
  });

  it("closes the drawer when the sending is cancelled", async () => {
    await setup();
    await showDocuments();

    await user.click(screen.getByRole("button", { name: "actie.annuleren" }));

    expect(sideNav.close).toHaveBeenCalled();
  });
});
