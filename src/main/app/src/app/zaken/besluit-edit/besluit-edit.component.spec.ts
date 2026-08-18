/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inputBinding, outputBinding } from "@angular/core";
import { provideHttpClient } from "@angular/common/http";
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
import { render, screen, within } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import moment from "moment";
import { EMPTY } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../setupJest";
import { UtilService } from "../../core/service/util.service";
import { FoutAfhandelingService } from "../../fout-afhandeling/fout-afhandeling.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { BesluitEditComponent } from "./besluit-edit.component";

const DOCUMENTS_URL = "/rest/informatieobjecten/informatieobjectenList";
const UPDATE_URL = "/rest/zaken/besluit";

const mockDocuments = [
  fromPartial<GeneratedType<"RestEnkelvoudigInformatieobject">>({
    uuid: "document-uuid-1",
    titel: "Document 1",
    bestandsnaam: "document-1.pdf",
  }),
  fromPartial<GeneratedType<"RestEnkelvoudigInformatieobject">>({
    uuid: "document-uuid-2",
    titel: "Document 2",
    bestandsnaam: "document-2.pdf",
  }),
];

const publicationBesluittype = fromPartial<GeneratedType<"RestBesluitType">>({
  id: "besluittype-id-2",
  naam: "Besluittype 2",
  publication: { enabled: true, responseTermDays: 6 },
});

const makeBesluit = (fields: Partial<GeneratedType<"RestBesluit">> = {}) =>
  fromPartial<GeneratedType<"RestBesluit">>({
    uuid: "besluit-uuid-1",
    besluittype: fromPartial<GeneratedType<"RestBesluitType">>({
      id: "besluittype-id-1",
      naam: "Besluittype 1",
      publication: { enabled: false },
    }),
    ingangsdatum: "2026-01-01",
    vervaldatum: "2026-12-31",
    toelichting: "Bestaande toelichting",
    informatieobjecten: [mockDocuments[0]],
    ...fields,
  });

describe(BesluitEditComponent.name, () => {
  let fixture: ComponentFixture<BesluitEditComponent>;
  let httpTestingController: HttpTestingController;

  const setup = async (besluit = makeBesluit()) => {
    const besluitGewijzigd = jest.fn();
    const sideNav = fromPartial<MatDrawer>({
      close: jest.fn().mockResolvedValue("close"),
    });

    const rendered = await render(BesluitEditComponent, {
      bindings: [
        inputBinding("besluit", () => besluit),
        inputBinding("zaak", () =>
          fromPartial<GeneratedType<"RestZaak">>({ uuid: "zaak-uuid-1" }),
        ),
        inputBinding("sideNav", () => sideNav),
        outputBinding<boolean>("besluitGewijzigd", besluitGewijzigd),
      ],
      imports: [TranslateModule.forRoot(), NoopAnimationsModule],
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideMomentDateAdapter({
          parse: { dateInput: "yyyy-MM-DD" },
          display: {
            dateInput: "yyyy-MM-DD",
            monthYearLabel: "MMMM YYYY",
            dateA11yLabel: "LL",
            monthYearA11yLabel: "MMMM YYYY",
          },
        }),
        provideQueryClient(testQueryClient),
      ],
    });

    fixture = rendered.fixture;
    httpTestingController = TestBed.inject(HttpTestingController);

    const utilService = TestBed.inject(UtilService);
    jest.spyOn(utilService, "openSnackbar");
    const foutAfhandelingService = TestBed.inject(FoutAfhandelingService);
    jest.spyOn(foutAfhandelingService, "foutAfhandelen").mockReturnValue(EMPTY);

    const documentsRequest = httpTestingController.expectOne(DOCUMENTS_URL);
    documentsRequest.flush(mockDocuments);
    await sleep();
    fixture.detectChanges();
    await sleep();
    // the documents table creates its row views in one pass and binds the cells in the next
    fixture.detectChanges();
    fixture.detectChanges();

    return {
      besluitGewijzigd,
      sideNav,
      documentsRequest,
      utilService,
      foutAfhandelingService,
    };
  };

  const submitButton = () =>
    screen.getByRole("button", { name: "actie.wijzigen" });

  const fillDate = async (label: string, value: string) => {
    const field = screen.getByLabelText(label);
    await userEvent.clear(field);
    await userEvent.type(field, value);
    await userEvent.tab();
  };

  const fillText = async (label: string, value: string) => {
    const field = screen.getByLabelText(label);
    await userEvent.clear(field);
    await userEvent.click(field);
    await userEvent.paste(value);
    await userEvent.tab();
  };

  const documentRow = (titel: string) =>
    screen.getByRole("row", { name: new RegExp(titel) });

  it("fills the form with the values of the besluit", async () => {
    await setup();

    expect(screen.getByLabelText("Besluit")).toHaveValue("Besluittype 1");
    expect(screen.getByLabelText("Ingangsdatum")).toHaveValue("2026-01-01");
    expect(screen.getByLabelText("Vervaldatum")).toHaveValue("2026-12-31");
    expect(screen.getByLabelText("BesluitToelichting")).toHaveValue(
      "Bestaande toelichting",
    );
  });

  it("does not allow the besluittype to be changed", async () => {
    await setup();

    expect(screen.getByLabelText("Besluit")).toBeDisabled();
  });

  it("shows the documents of the besluittype for this zaak", async () => {
    const { documentsRequest } = await setup();

    expect(documentsRequest.request.body).toEqual({
      zaakUUID: "zaak-uuid-1",
      besluittypeUUID: "besluittype-id-1",
    });
    expect(documentRow("Document 1")).toBeVisible();
    expect(documentRow("Document 2")).toBeVisible();
  });

  it("pre-selects the documents that are already linked to the besluit", async () => {
    await setup();

    expect(
      within(documentRow("Document 1")).getByRole("checkbox"),
    ).toBeChecked();
    expect(
      within(documentRow("Document 2")).getByRole("checkbox"),
    ).not.toBeChecked();
  });

  it("cannot be submitted without a reden", async () => {
    await setup();

    await fillText("BesluitToelichting", "Andere toelichting");

    expect(submitButton()).toBeDisabled();
  });

  it("can be submitted once a reden is given", async () => {
    await setup();

    await fillText("Wijziging.reden", "Wijziging reden");

    expect(submitButton()).toBeEnabled();
  });

  it("cannot be submitted without an ingangsdatum", async () => {
    await setup();
    await fillText("Wijziging.reden", "Wijziging reden");

    await userEvent.clear(screen.getByLabelText("Ingangsdatum"));

    expect(submitButton()).toBeDisabled();
  });

  it("warns when the vervaldatum is before the ingangsdatum", async () => {
    await setup();

    await fillDate("Vervaldatum", "2025-12-31");

    expect(
      screen.getByText(
        "msg.error.date.invalid.datum.vervaldatum-voor-ingangsdatum",
      ),
    ).toBeVisible();
  });

  it("warns about the prefilled vervaldatum when the ingangsdatum is moved past it", async () => {
    await setup();

    await fillDate("Ingangsdatum", "2027-06-10");

    expect(
      screen.getByText(
        "msg.error.date.invalid.datum.vervaldatum-voor-ingangsdatum",
      ),
    ).toBeVisible();
  });

  it("accepts a toelichting of 1000 characters and takes no more", async () => {
    await setup();

    await fillText("BesluitToelichting", "a".repeat(1001));

    expect(screen.getByLabelText("BesluitToelichting")).toHaveValue(
      "a".repeat(1000),
    );
  });

  it("accepts a reden of 80 characters and takes no more", async () => {
    await setup();

    await fillText("Wijziging.reden", "a".repeat(81));

    expect(screen.getByLabelText("Wijziging.reden")).toHaveValue(
      "a".repeat(80),
    );
  });

  it("hides the publication dates for a besluittype without publication", async () => {
    await setup();

    expect(screen.queryByLabelText("Publicatiedatum")).toBeNull();
    expect(screen.queryByLabelText("Uiterlijkereactiedatum")).toBeNull();
  });

  it("shows the publication dates for a besluittype that requires publication", async () => {
    await setup(makeBesluit({ besluittype: publicationBesluittype }));

    expect(screen.getByLabelText("Publicatiedatum")).toBeVisible();
    expect(screen.getByLabelText("Uiterlijkereactiedatum")).toBeVisible();
  });

  it("derives the uiterlijke reactiedatum from the publicatiedatum", async () => {
    await setup(makeBesluit({ besluittype: publicationBesluittype }));

    await fillDate("Publicatiedatum", "2026-03-01");

    expect(screen.getByLabelText("Uiterlijkereactiedatum")).toHaveValue(
      "2026-03-07",
    );
  });

  it("warns when the uiterlijke reactiedatum is before the publicatiedatum", async () => {
    await setup(makeBesluit({ besluittype: publicationBesluittype }));
    await fillDate("Publicatiedatum", "2026-03-01");

    await fillDate("Uiterlijkereactiedatum", "2026-03-01");

    expect(
      screen.getByText(
        "msg.error.date.invalid.datum.reactiedatum-voor-publicatiedatum",
      ),
    ).toBeVisible();
  });

  it("warns when the uiterlijke reactiedatum is before the vervaldatum it was loaded with", async () => {
    await setup(makeBesluit({ besluittype: publicationBesluittype }));

    await fillDate("Uiterlijkereactiedatum", "2026-01-01");

    expect(
      screen.getByText(
        "msg.error.date.invalid.datum.reactiedatum-voor-publicatiedatum",
      ),
    ).toBeVisible();
  });

  it("empties the uiterlijke reactiedatum when the publicatiedatum is cleared", async () => {
    await setup(makeBesluit({ besluittype: publicationBesluittype }));
    await fillDate("Publicatiedatum", "2026-03-01");

    await userEvent.clear(screen.getByLabelText("Publicatiedatum"));

    expect(screen.getByLabelText("Uiterlijkereactiedatum")).toHaveValue("");
  });

  it("updates the besluit and reports the change", async () => {
    const { besluitGewijzigd, utilService } = await setup();
    await fillText("Wijziging.reden", "Wijziging reden");

    await userEvent.click(submitButton());
    await sleep();

    const request = httpTestingController.expectOne({
      method: "PUT",
      url: UPDATE_URL,
    });
    expect(request.request.body).toEqual(
      expect.objectContaining({
        besluitUuid: "besluit-uuid-1",
        reden: "Wijziging reden",
        informatieobjecten: ["document-uuid-1"],
        ingangsdatum: moment("2026-01-01").toISOString(),
        vervaldatum: moment("2026-12-31").toISOString(),
      }),
    );
    request.flush(null);
    await sleep(100);

    expect(utilService.openSnackbar).toHaveBeenCalledWith(
      "msg.besluit.gewijzigd",
    );
    expect(besluitGewijzigd).toHaveBeenCalledWith(true);
  });

  it("includes the publication dates when the besluittype requires publication", async () => {
    await setup(
      makeBesluit({
        besluittype: publicationBesluittype,
        vervaldatum: "2026-02-01",
        publicationDate: "2026-02-01",
        lastResponseDate: "2026-02-07",
      }),
    );
    await fillText("Wijziging.reden", "Wijziging reden");

    await userEvent.click(submitButton());
    await sleep();

    const request = httpTestingController.expectOne({
      method: "PUT",
      url: UPDATE_URL,
    });
    expect(request.request.body).toEqual(
      expect.objectContaining({
        publicationDate: moment("2026-02-01").toISOString(),
        lastResponseDate: moment("2026-02-07").toISOString(),
      }),
    );
    request.flush(null);
    await sleep(100);
  });

  it("shows an error and keeps the panel open when updating fails", async () => {
    const { besluitGewijzigd, sideNav, foutAfhandelingService } = await setup();
    await fillText("Wijziging.reden", "Wijziging reden");

    await userEvent.click(submitButton());
    await sleep();
    httpTestingController
      .expectOne({ method: "PUT", url: UPDATE_URL })
      .flush(null, { status: 500, statusText: "Internal Server Error" });
    await sleep(100);

    expect(foutAfhandelingService.foutAfhandelen).toHaveBeenCalled();
    expect(besluitGewijzigd).not.toHaveBeenCalled();
    expect(sideNav.close).not.toHaveBeenCalled();
  });

  it("closes the side panel when the cancel button is used", async () => {
    const { sideNav } = await setup();

    await userEvent.click(
      screen.getByRole("button", { name: "actie.annuleren" }),
    );

    expect(sideNav.close).toHaveBeenCalled();
  });
});
