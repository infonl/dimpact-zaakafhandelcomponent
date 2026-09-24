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
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatDrawer } from "@angular/material/sidenav";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { render, screen, within } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { of } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../setupJest";
import { UtilService } from "../../core/service/util.service";
import { FoutAfhandelingService } from "../../fout-afhandeling/fout-afhandeling.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { LINKABLE_ZAKEN_PAGINATION_SIZE } from "../../zoeken/zoeken.service";
import { InformatieObjectLinkComponent } from "./informatie-object-link.component";

const LINK_URL = "/rest/informatieobjecten/informatieobject/verplaats";

const detachedDocument = fromPartial<GeneratedType<"RestDetachedDocument">>({
  documentUUID: "fakeDocumentUuid",
  documentID: "DOC-001",
  titel: "Test Document",
  informatieobjectTypeUUID: "fakeInformatieobjectTypeUuid",
});

const inboxDocument = fromPartial<GeneratedType<"RestInboxDocument">>({
  enkelvoudiginformatieobjectUUID: "fakeInboxDocumentUuid",
  enkelvoudiginformatieobjectID: "INBOX-001",
  titel: "Inbox Document",
  informatieobjectTypeUUID: "fakeInformatieobjectTypeUuid",
});

const enkelvoudigInformatieobject = fromPartial<
  GeneratedType<"RestEnkelvoudigInformatieobject">
>({
  uuid: "fakeEnkelvoudigInformatieobjectUuid",
  identificatie: "EIO-001",
  titel: "Zaak Document",
  informatieobjectTypeUUID: "fakeInformatieobjectTypeUuid",
});

const koppelbareZaken = [
  fromPartial<GeneratedType<"RestZaakKoppelenZoekObject">>({
    identificatie: "ZAAK-001",
    zaaktypeOmschrijving: "Type A",
    statustypeOmschrijving: "In behandeling",
    omschrijving: "Zaak omschrijving 1",
    isKoppelbaar: true,
  }),
  fromPartial<GeneratedType<"RestZaakKoppelenZoekObject">>({
    identificatie: "ZAAK-002",
    zaaktypeOmschrijving: "Type B",
    statustypeOmschrijving: "Afgerond",
    omschrijving: "Zaak omschrijving 2",
    isKoppelbaar: false,
  }),
  fromPartial<GeneratedType<"RestZaakKoppelenZoekObject">>({
    identificatie: "SOURCE-ZAAK",
    zaaktypeOmschrijving: "Type C",
    statustypeOmschrijving: "Open",
    omschrijving: "Zaak omschrijving 3",
    isKoppelbaar: true,
  }),
];

describe(InformatieObjectLinkComponent.name, () => {
  let fixture: ComponentFixture<InformatieObjectLinkComponent>;
  let httpTestingController: HttpTestingController;
  let sideNav: MatDrawer;
  let informationObjectLinked: jest.Mock;
  let openSnackbar: jest.SpyInstance;
  let foutAfhandelen: jest.SpyInstance;

  const user = userEvent.setup({ delay: null });

  jest.setTimeout(20_000);

  async function setup(
    infoObject: GeneratedType<
      | "RestDetachedDocument"
      | "RestInboxDocument"
      | "RestEnkelvoudigInformatieobject"
    > | null = detachedDocument,
    actionLabel:
      | "actie.document.koppelen"
      | "actie.document.verplaatsen" = "actie.document.koppelen",
  ) {
    sideNav = fromPartial<MatDrawer>({
      close: jest.fn().mockResolvedValue(undefined),
    });
    informationObjectLinked = jest.fn();

    const inputs = {
      sideNav,
      source: "SOURCE-ZAAK",
      actionLabel,
    };

    const { fixture: renderedFixture, rerender } = await render(
      InformatieObjectLinkComponent,
      {
        inputs: { ...inputs, infoObject: null },
        imports: [NoopAnimationsModule, TranslateModule.forRoot()],
        providers: [
          provideRouter([]),
          provideHttpClient(withInterceptorsFromDi()),
          provideHttpClientTesting(),
          provideQueryClient(testQueryClient),
        ],
      },
    );

    fixture = renderedFixture;
    fixture.componentInstance.informationObjectLinked.subscribe(
      informationObjectLinked,
    );
    httpTestingController = TestBed.inject(HttpTestingController);
    openSnackbar = jest
      .spyOn(TestBed.inject(UtilService), "openSnackbar")
      .mockImplementation(() => undefined);
    foutAfhandelen = jest
      .spyOn(TestBed.inject(FoutAfhandelingService), "foutAfhandelen")
      .mockReturnValue(of());
    jest
      .spyOn(TestBed.inject(TranslateService), "instant")
      .mockImplementation((key, parameters) =>
        parameters?.["documentID"]
          ? `${key} ${parameters["documentID"]}`
          : String(key),
      );

    await rerender({ inputs: { ...inputs, infoObject } });
    fixture.detectChanges();
  }

  function searchField() {
    return screen.getByLabelText(/identificatie/i);
  }

  function searchButton() {
    return screen.getByRole("button", { name: "actie.zoeken" });
  }

  async function search(zaakIdentificatie: string) {
    await user.type(searchField(), zaakIdentificatie);
    await user.click(searchButton());
    await sleep();
  }

  async function showKoppelbareZaken(totaal = koppelbareZaken.length) {
    await search("ZAAK");
    httpTestingController
      .expectOne("/rest/zoeken/zaken")
      .flush({ totaal, resultaten: koppelbareZaken, filters: {} });
    await sleep();
    // the table creates the row views in one pass and binds their cells in the next
    fixture.detectChanges();
    fixture.detectChanges();
  }

  function koppelButtonInRowOf(
    zaakIdentificatie: string,
    actionLabel = "actie.document.koppelen",
  ) {
    const row = screen.getByRole("row", {
      name: new RegExp(zaakIdentificatie),
    });
    return within(row).getByRole("button", { name: actionLabel });
  }

  function toolbar(actionLabel = "actie.document.koppelen") {
    return screen.getByRole("heading", { name: new RegExp(actionLabel) });
  }

  it("names the document to link in the introduction", async () => {
    await setup();

    expect(
      screen.getByText("informatieobject.koppelen.uitleg DOC-001"),
    ).toBeVisible();
  });

  it("names an inbox document by its enkelvoudiginformatieobject in the introduction", async () => {
    await setup(inboxDocument);

    expect(
      screen.getByText("informatieobject.koppelen.uitleg INBOX-001"),
    ).toBeVisible();
  });

  it("names an enkelvoudig informatieobject by its identificatie in the introduction", async () => {
    await setup(enkelvoudigInformatieobject);

    expect(
      screen.getByText("informatieobject.koppelen.uitleg EIO-001"),
    ).toBeVisible();
  });

  it("offers no search while there is no document to link", async () => {
    await setup(null);

    expect(toolbar()).toBeVisible();
    expect(
      screen.queryByRole("button", { name: "actie.zoeken" }),
    ).not.toBeInTheDocument();
  });

  it("shows the koppelen action with a link icon in its toolbar", async () => {
    await setup();

    expect(within(toolbar()).getByText("link")).toBeInTheDocument();
  });

  it("shows the verplaatsen action with a move icon in its toolbar", async () => {
    await setup(detachedDocument, "actie.document.verplaatsen");

    expect(
      within(toolbar("actie.document.verplaatsen")).getByText("move_item"),
    ).toBeInTheDocument();
  });

  it("closes the side nav from its toolbar", async () => {
    await setup();

    await user.click(within(toolbar()).getByRole("button"));

    expect(sideNav.close).toHaveBeenCalled();
  });

  describe("when a different document is given", () => {
    it("names the new document in the introduction", async () => {
      await setup();

      fixture.componentRef.setInput("infoObject", inboxDocument);
      fixture.detectChanges();

      expect(
        screen.getByText("informatieobject.koppelen.uitleg INBOX-001"),
      ).toBeVisible();
    });

    it("clears the search and its results", async () => {
      await setup();
      await showKoppelbareZaken();

      fixture.componentRef.setInput("infoObject", inboxDocument);
      fixture.detectChanges();

      expect(searchField()).toHaveValue("");
      expect(screen.queryByRole("row", { name: /ZAAK-001/ })).toBeNull();
      expect(screen.getByText("msg.geen.gegevens.gevonden")).toBeVisible();
    });

    it("searches the zaken for the informatieobjecttype of the new document", async () => {
      await setup();

      fixture.componentRef.setInput(
        "infoObject",
        fromPartial<GeneratedType<"RestInboxDocument">>({
          ...inboxDocument,
          informatieobjectTypeUUID: "fakeOtherInformatieobjectTypeUuid",
        }),
      );
      fixture.detectChanges();
      await search("ZAAK-001");

      const request = httpTestingController.expectOne("/rest/zoeken/zaken");
      expect(request.request.body).toMatchObject({
        informationObjectTypeUuid: "fakeOtherInformatieobjectTypeUuid",
      });
      request.flush({ totaal: 0, resultaten: [], filters: {} });
    });

    it("hides the search when the document is taken away", async () => {
      await setup();

      fixture.componentRef.setInput("infoObject", null);
      fixture.detectChanges();

      expect(
        screen.queryByRole("button", { name: "actie.zoeken" }),
      ).not.toBeInTheDocument();
    });
  });

  it("keeps the search disabled until at least two characters are typed", async () => {
    await setup();

    expect(searchButton()).toBeDisabled();

    await user.type(searchField(), "Z");
    expect(searchButton()).toBeDisabled();

    await user.type(searchField(), "AAK-001");
    expect(searchButton()).toBeEnabled();
  });

  it("searches the zaken the document can be linked to", async () => {
    await setup();

    await search("ZAAK-001");

    const request = httpTestingController.expectOne("/rest/zoeken/zaken");
    expect(request.request.body).toEqual({
      zaakIdentificator: "ZAAK-001",
      informationObjectTypeUuid: "fakeInformatieobjectTypeUuid",
      page: 0,
      rows: LINKABLE_ZAKEN_PAGINATION_SIZE,
    });
    request.flush({ totaal: 0, resultaten: [], filters: {} });
  });

  it("does not offer to search again while a search is running", async () => {
    await setup();

    await search("ZAAK-001");

    expect(searchButton()).toBeDisabled();
    httpTestingController
      .expectOne("/rest/zoeken/zaken")
      .flush({ totaal: 0, resultaten: [], filters: {} });
  });

  it("does not search for a document without an informatieobjecttype", async () => {
    await setup(
      fromPartial<GeneratedType<"RestDetachedDocument">>({
        ...detachedDocument,
        informatieobjectTypeUUID: undefined,
      }),
    );

    await search("ZAAK-001");

    httpTestingController.expectNone("/rest/zoeken/zaken");
    expect(screen.queryByText("msg.loading")).toBeNull();
  });

  it("shows a row for every zaak that was found", async () => {
    await setup();

    await showKoppelbareZaken();

    expect(
      within(screen.getByRole("row", { name: /ZAAK-001/ })).getByText("Type A"),
    ).toBeVisible();
    expect(screen.getByRole("row", { name: /ZAAK-002/ })).toBeVisible();
  });

  it("does not offer to link a zaak that is not koppelbaar or is the source", async () => {
    await setup();

    await showKoppelbareZaken();

    expect(koppelButtonInRowOf("ZAAK-001")).toBeEnabled();
    expect(koppelButtonInRowOf("ZAAK-002")).toBeDisabled();
    expect(koppelButtonInRowOf("SOURCE-ZAAK")).toBeDisabled();
  });

  it("warns that only the first results are shown when there are more", async () => {
    await setup();

    await showKoppelbareZaken(30);

    expect(screen.getByText(/Alleen de eerste 10 resultaten/)).toBeVisible();
  });

  it("links the document to the zaak of the row it was clicked on", async () => {
    await setup();
    await showKoppelbareZaken();

    await user.click(koppelButtonInRowOf("ZAAK-001"));
    await sleep();

    const request = httpTestingController.expectOne(LINK_URL);
    expect(request.request.method).toBe("POST");
    expect(request.request.body).toEqual({
      documentUUID: "fakeDocumentUuid",
      bron: "SOURCE-ZAAK",
      nieuweZaakID: "ZAAK-001",
    });
    request.flush(null);
  });

  it("links an inbox document by its enkelvoudiginformatieobject", async () => {
    await setup(inboxDocument);
    await showKoppelbareZaken();

    await user.click(koppelButtonInRowOf("ZAAK-001"));
    await sleep();

    const request = httpTestingController.expectOne(LINK_URL);
    expect(request.request.body).toMatchObject({
      documentUUID: "fakeInboxDocumentUuid",
    });
    request.flush(null);
  });

  it("reports the link, closes the side nav and announces it once linked", async () => {
    await setup();
    await showKoppelbareZaken();

    await user.click(koppelButtonInRowOf("ZAAK-001"));
    await sleep();
    httpTestingController.expectOne(LINK_URL).flush(null);
    await sleep();

    expect(openSnackbar).toHaveBeenCalledWith(
      "msg.document.koppelen.uitgevoerd",
      {
        document: "Test Document",
        case: "ZAAK-001",
      },
    );
    expect(sideNav.close).toHaveBeenCalled();
    expect(informationObjectLinked).toHaveBeenCalled();
  });

  it("links an enkelvoudig informatieobject by its uuid", async () => {
    await setup(enkelvoudigInformatieobject);
    await showKoppelbareZaken();

    await user.click(koppelButtonInRowOf("ZAAK-001"));
    await sleep();

    const request = httpTestingController.expectOne(LINK_URL);
    expect(request.request.body).toMatchObject({
      documentUUID: "fakeEnkelvoudigInformatieobjectUuid",
    });
    request.flush(null);
  });

  it("reports the move once the document is moved", async () => {
    await setup(detachedDocument, "actie.document.verplaatsen");
    await showKoppelbareZaken();

    await user.click(
      koppelButtonInRowOf("ZAAK-001", "actie.document.verplaatsen"),
    );
    await sleep();
    httpTestingController.expectOne(LINK_URL).flush(null);
    await sleep();

    expect(openSnackbar).toHaveBeenCalledWith(
      "msg.document.verplaatsen.uitgevoerd",
      {
        document: "Test Document",
        case: "ZAAK-001",
      },
    );
    expect(informationObjectLinked).toHaveBeenCalled();
  });

  it("routes a failed link through the error handler", async () => {
    await setup();
    await showKoppelbareZaken();

    await user.click(koppelButtonInRowOf("ZAAK-001"));
    await sleep();
    httpTestingController
      .expectOne(LINK_URL)
      .flush(null, { status: 500, statusText: "Server Error" });
    await sleep();

    expect(foutAfhandelen).toHaveBeenCalled();
    expect(openSnackbar).not.toHaveBeenCalled();
  });

  it("does not offer to link again while a link is in progress", async () => {
    await setup();
    await showKoppelbareZaken();

    await user.click(koppelButtonInRowOf("ZAAK-001"));
    await sleep();
    fixture.detectChanges();

    expect(koppelButtonInRowOf("ZAAK-001")).toBeDisabled();
    httpTestingController.expectOne(LINK_URL).flush(null);
  });

  it("clears the search and its results", async () => {
    await setup();
    await showKoppelbareZaken();

    await user.click(screen.getByRole("button", { name: "actie.wissen" }));
    fixture.detectChanges();

    expect(searchField()).toHaveValue("");
    expect(screen.queryByRole("row", { name: /ZAAK-001/ })).toBeNull();
    expect(screen.getByText("msg.geen.gegevens.gevonden")).toBeVisible();
  });

  it("closes the side nav when the linking is cancelled", async () => {
    await setup();

    await user.click(screen.getByRole("button", { name: "actie.annuleren" }));

    expect(sideNav.close).toHaveBeenCalled();
  });
});
