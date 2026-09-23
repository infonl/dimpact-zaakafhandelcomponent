/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { provideMomentDateAdapter } from "@angular/material-moment-adapter";
import { MatDrawer } from "@angular/material/sidenav";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { render, screen, within } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { EMPTY } from "rxjs";
import { createQueryOptions, fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../setupJest";
import { UtilService } from "../../core/service/util.service";
import { FoutAfhandelingService } from "../../fout-afhandeling/fout-afhandeling.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZoekenService } from "../../zoeken/zoeken.service";
import { ZaakLinkComponent } from "./zaak-link.component";

const KOPPEL_URL = "/rest/zaken/zaak/koppel";
const ZAAKTYPES_URL = "/rest/zaken/gekoppelde-zaken/zaaktypen";

const makeFakeZaak = (
  fields: Partial<GeneratedType<"RestZaak">> = {},
): GeneratedType<"RestZaak"> =>
  fromPartial<GeneratedType<"RestZaak">>({
    uuid: "fake-zaak-uuid",
    identificatie: "ZAAK-2026-001",
    zaaktype: fromPartial<GeneratedType<"RestZaaktype">>({
      omschrijving: "fakeZaaktypeOmschrijving",
    }),
    ...fields,
  });

const makeFakeSearchResult = (
  fields: Partial<GeneratedType<"RestZaakKoppelenZoekObject">> = {},
): GeneratedType<"RestZaakKoppelenZoekObject"> =>
  fromPartial<GeneratedType<"RestZaakKoppelenZoekObject">>({
    id: "fake-result-uuid",
    identificatie: "ZAAK-2026-002",
    isKoppelbaar: true,
    ...fields,
  });

const NOT_LINKABLE_REASONS: NonNullable<
  GeneratedType<"RestZaakKoppelenZoekObject">["nietKoppelbaarReden"]
>[] = [
  "FOUND_ZAAK_AFGEHANDELD",
  "FOUND_ZAAK_OPEN",
  "FOUND_ZAAK_IS_DEELZAAK_SO_NO_HOOFDZAAK",
  "FOUND_ZAAK_ALREADY_DEELZAAK",
  "FOUND_ZAAK_HAS_DEELZAKEN",
  "ZAAKTYPE_DOES_NOT_ALLOW_DEELZAAK",
  "NO_KOPPELEN_RIGHT",
  "NO_LEZEN_RIGHT",
];

describe(ZaakLinkComponent.name, () => {
  let fixture: ComponentFixture<ZaakLinkComponent>;
  let httpTestingController: HttpTestingController;

  const user = userEvent.setup({ delay: null });

  const setup = async (zaakFields: Partial<GeneratedType<"RestZaak">> = {}) => {
    const zaak = makeFakeZaak(zaakFields);
    const sideNav = fromPartial<MatDrawer>({
      close: jest.fn().mockResolvedValue("close"),
    });
    const zaakLinked = jest.fn();

    const rendered = await render(ZaakLinkComponent, {
      inputs: { zaak, sideNav },
      on: { zaakLinked },
      imports: [NoopAnimationsModule, TranslateModule.forRoot()],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideQueryClient(testQueryClient),
        provideRouter([]),
        provideMomentDateAdapter(),
      ],
    });

    fixture = rendered.fixture;
    httpTestingController = TestBed.inject(HttpTestingController);

    const utilService = TestBed.inject(UtilService);
    jest.spyOn(utilService, "setLoading").mockReturnValue(undefined);
    jest.spyOn(utilService, "openSnackbar").mockReturnValue(undefined);
    const foutAfhandelingService = TestBed.inject(FoutAfhandelingService);
    jest.spyOn(foutAfhandelingService, "foutAfhandelen").mockReturnValue(EMPTY);

    httpTestingController.expectOne(ZAAKTYPES_URL).flush([]);

    return {
      zaak,
      sideNav,
      zaakLinked,
      utilService,
      foutAfhandelingService,
    };
  };

  const relationTypeSelect = () => screen.getByLabelText("Zaak.koppelen.label");

  const chooseRelationType = async (relationType: string) => {
    await user.click(relationTypeSelect());
    await user.click(
      screen.getByRole("option", {
        name: `zaak.koppelen.link.type.${relationType}`,
      }),
    );
  };

  const findLinkableZaken = (
    resultaten: GeneratedType<"RestZaakKoppelenZoekObject">[],
    totaal = resultaten.length,
  ) =>
    jest
      .spyOn(ZoekenService.prototype, "findLinkableZaken")
      .mockReturnValue(
        createQueryOptions(
          fromPartial<
            GeneratedType<"RestZoekResultaatRestZaakKoppelenZoekObject">
          >({ resultaten, totaal }),
        ) as never,
      );

  const enterSearchCriterion = async () => {
    await user.type(screen.getByLabelText("Zaak.identificatie"), "ZAAK-2026");
  };

  const clickSearch = async () => {
    await user.click(screen.getByRole("button", { name: "actie.zoeken" }));
    await sleep();
    // the results table creates the row views in one pass and binds their cells in the next
    fixture.detectChanges();
    fixture.detectChanges();
  };

  const dateRangeField = (label: string) =>
    screen.getByRole("group", { name: label });

  const fillDateRange = async (label: string, van: string, tot: string) => {
    await user.type(
      within(dateRangeField(label)).getByPlaceholderText("zoeken.filter.van"),
      van,
    );
    await user.type(
      within(dateRangeField(label)).getByPlaceholderText(
        "zoeken.filter.tot_en_met",
      ),
      tot,
    );
    await user.tab();
  };

  const linkButtonOfRow = (identificatie: string) =>
    within(
      screen.getByRole("row", { name: new RegExp(identificatie) }),
    ).getByRole("button", { name: "actie.zaak.koppelen" });

  const translateZaaktypeForbidsDeelzaak = () => {
    const translateService = TestBed.inject(TranslateService);
    translateService.setTranslation("nl", {
      "zaak.koppelen.niet-koppelbaar.ZAAKTYPE_DOES_NOT_ALLOW_DEELZAAK":
        "{{hoofdzaakZaaktype}} staat geen deelzaken van {{deelzaakZaaktype}} toe",
    });
    translateService.use("nl");
  };

  it("offers every relation type a zaak can be linked with", async () => {
    await setup();

    await user.click(relationTypeSelect());

    expect(
      screen.getAllByRole("option").map((option) => option.textContent?.trim()),
    ).toEqual([
      "zaak.koppelen.link.type.DEELZAAK",
      "zaak.koppelen.link.type.HOOFDZAAK",
      "zaak.koppelen.link.type.GERELATEERD",
    ]);
  });

  it("cannot be searched before a relation type is chosen", async () => {
    await setup();

    expect(screen.getByRole("button", { name: "actie.zoeken" })).toBeDisabled();
  });

  it.each([
    ["HOOFDZAAK", "zaak.koppelen.hint.hoofdzaak-aan-deelzaak"],
    ["DEELZAAK", "zaak.koppelen.hint.deelzaak-aan-hoofdzaak"],
    ["GERELATEERD", "zaak.koppelen.hint.gerelateerd"],
  ])("explains what linking a %s means", async (relationType, hint) => {
    await setup();

    await chooseRelationType(relationType);

    expect(screen.getByText(hint)).toBeVisible();
  });

  it.each([
    ["HOOFDZAAK", "zaak.koppelen.geblokkeerd.al-deelzaak-van-andere-zaak"],
    ["DEELZAAK", "zaak.koppelen.geblokkeerd.is-zelf-deelzaak"],
  ])(
    "replaces the search form with an explanation when the zaak is itself a deelzaak and %s is chosen",
    async (relationType, blockedReason) => {
      await setup({ isDeelzaak: true });

      await chooseRelationType(relationType);

      expect(screen.getByText(blockedReason)).toBeVisible();
      expect(
        screen.queryByRole("button", { name: "actie.zoeken" }),
      ).not.toBeInTheDocument();
    },
  );

  it("blocks the search as soon as the zaak gains deelzaken while the panel is open", async () => {
    await setup({ isHoofdzaak: false });

    await chooseRelationType("HOOFDZAAK");
    await fixture.componentRef.setInput(
      "zaak",
      makeFakeZaak({ isHoofdzaak: true }),
    );
    fixture.detectChanges();

    expect(
      screen.getByText("zaak.koppelen.geblokkeerd.heeft-al-deelzaken"),
    ).toBeVisible();
  });

  it("replaces the search form with an explanation when the zaak already has deelzaken and HOOFDZAAK is chosen", async () => {
    await setup({ isHoofdzaak: true });

    await chooseRelationType("HOOFDZAAK");

    expect(
      screen.getByText("zaak.koppelen.geblokkeerd.heeft-al-deelzaken"),
    ).toBeVisible();
    expect(
      screen.queryByRole("button", { name: "actie.zoeken" }),
    ).not.toBeInTheDocument();
  });

  it("still searches for a GERELATEERD link when the zaak is a deelzaak that has deelzaken of its own", async () => {
    const { zaak } = await setup({ isDeelzaak: true, isHoofdzaak: true });
    const search = findLinkableZaken([
      makeFakeSearchResult({ identificatie: "ZAAK-2026-002" }),
    ]);

    await chooseRelationType("GERELATEERD");
    await enterSearchCriterion();
    await clickSearch();

    expect(search).toHaveBeenCalledWith(
      expect.objectContaining({
        zaakUuid: zaak.uuid,
        relationType: "GERELATEERD",
      }),
    );
    expect(
      screen.getByRole("row", { name: /ZAAK-2026-002/ }),
    ).toBeInTheDocument();
    expect(
      screen.queryByText("zaak.koppelen.geblokkeerd.is-zelf-deelzaak"),
    ).not.toBeInTheDocument();
  });

  it("searches for linkable zaken with the entered criteria", async () => {
    const { zaak } = await setup();
    const search = findLinkableZaken([makeFakeSearchResult()]);

    await chooseRelationType("DEELZAAK");
    await user.type(screen.getByLabelText("Zaak.identificatie"), "ZAAK-2026");
    await user.type(
      screen.getByLabelText("ZoekVeld.ZAAK_OMSCHRIJVING"),
      "ZAAKOMSCHR",
    );
    await fillDateRange("Startdatum", "01-02-2026", "01-03-2026");
    await fillDateRange("Einddatum", "01-04-2026", "01-05-2026");
    await clickSearch();

    expect(search).toHaveBeenCalledWith({
      zaakUuid: zaak.uuid,
      zoekZaakIdentifier: "ZAAK-2026",
      zoekZaakOmschrijving: "ZAAKOMSCHR",
      zoekZaakTypeOmschrijving: undefined,
      relationType: "DEELZAAK",
      startdatum: {
        van: new Date(2026, 1, 1).toISOString(),
        tot: new Date(2026, 2, 1).toISOString(),
      },
      einddatum: {
        van: new Date(2026, 3, 1).toISOString(),
        tot: new Date(2026, 4, 1).toISOString(),
      },
    });
  });

  it("cannot be searched until at least one criterion is entered", async () => {
    await setup();

    await chooseRelationType("DEELZAAK");

    expect(screen.getByRole("button", { name: "actie.zoeken" })).toBeDisabled();

    await enterSearchCriterion();
    fixture.detectChanges();

    expect(screen.getByRole("button", { name: "actie.zoeken" })).toBeEnabled();
  });

  it("counts a date range as a criterion of its own", async () => {
    await setup();

    await chooseRelationType("DEELZAAK");
    await fillDateRange("Startdatum", "01-01-2026", "31-01-2026");
    fixture.detectChanges();

    expect(screen.getByRole("button", { name: "actie.zoeken" })).toBeEnabled();
  });

  it("shows no results table before a search was done", async () => {
    await setup();

    await chooseRelationType("DEELZAAK");

    expect(screen.queryByRole("table")).toBeNull();
    expect(screen.queryByText("msg.geen.gegevens.gevonden")).toBeNull();
  });

  it("reports that no zaken were found when the search comes back empty", async () => {
    await setup();
    findLinkableZaken([]);

    await chooseRelationType("DEELZAAK");
    await enterSearchCriterion();
    await clickSearch();

    expect(screen.getByText("msg.geen.gegevens.gevonden")).toBeVisible();
    expect(screen.queryByRole("table")).toBeNull();
  });

  it("does not search again when the search fields are cleared", async () => {
    await setup();
    const findLinkableZakenSpy = findLinkableZaken([makeFakeSearchResult()]);

    await chooseRelationType("DEELZAAK");
    await enterSearchCriterion();
    await clickSearch();
    findLinkableZakenSpy.mockClear();

    await user.click(screen.getByRole("button", { name: "actie.wissen" }));
    await sleep();

    expect(findLinkableZakenSpy).not.toHaveBeenCalled();
  });

  it("shows the zaken that were found", async () => {
    await setup();
    findLinkableZaken([makeFakeSearchResult()]);

    await chooseRelationType("DEELZAAK");
    await enterSearchCriterion();
    await clickSearch();

    expect(screen.getByRole("row", { name: /ZAAK-2026-002/ })).toBeVisible();
  });

  it("stops showing the loading message when the search fails", async () => {
    await setup();
    jest.spyOn(ZoekenService.prototype, "findLinkableZaken").mockReturnValue(
      fromPartial({
        queryKey: ["koppelbare zaken die falen"],
        queryFn: () => Promise.reject(new Error("server error")),
        retry: false,
      }),
    );

    await chooseRelationType("DEELZAAK");
    await enterSearchCriterion();
    await clickSearch();

    expect(screen.queryByText("msg.loading")).toBeNull();
  });

  it("points out that only the first ten of many results are shown", async () => {
    await setup();
    findLinkableZaken(
      Array.from({ length: 11 }, (_, index) =>
        makeFakeSearchResult({ id: `result-${index}` }),
      ),
      11,
    );

    await chooseRelationType("DEELZAAK");
    await enterSearchCriterion();
    await clickSearch();

    expect(
      screen.getByText("msg.zaak.koppelen.meer-dan-10-gevonden"),
    ).toBeVisible();
  });

  it("does not point that out when ten or fewer zaken were found", async () => {
    await setup();
    findLinkableZaken([makeFakeSearchResult()]);

    await chooseRelationType("DEELZAAK");
    await enterSearchCriterion();
    await clickSearch();

    expect(
      screen.queryByText("msg.zaak.koppelen.meer-dan-10-gevonden"),
    ).toBeNull();
  });

  it("drops the results when another relation type is chosen", async () => {
    await setup();
    findLinkableZaken([makeFakeSearchResult()]);

    await chooseRelationType("DEELZAAK");
    await enterSearchCriterion();
    await clickSearch();
    await chooseRelationType("HOOFDZAAK");
    fixture.detectChanges();

    expect(screen.queryByRole("row", { name: /ZAAK-2026-002/ })).toBeNull();
  });

  it("drops the results and the criteria when they are cleared", async () => {
    await setup();
    findLinkableZaken([makeFakeSearchResult()]);

    await chooseRelationType("DEELZAAK");
    await enterSearchCriterion();
    await clickSearch();
    await user.click(screen.getByRole("button", { name: "actie.wissen" }));
    fixture.detectChanges();

    expect(screen.queryByRole("row", { name: /ZAAK-2026-002/ })).toBeNull();
    expect(screen.getByRole("button", { name: "actie.zoeken" })).toBeDisabled();
  });

  it("cannot link a zaak that is not koppelbaar", async () => {
    await setup();
    findLinkableZaken([
      makeFakeSearchResult({
        identificatie: "ZAAK-2026-003",
        isKoppelbaar: false,
      }),
    ]);

    await chooseRelationType("DEELZAAK");
    await enterSearchCriterion();
    await clickSearch();

    expect(linkButtonOfRow("ZAAK-2026-003")).toHaveAttribute(
      "aria-disabled",
      "true",
    );
  });

  it.each(NOT_LINKABLE_REASONS)(
    "explains that a found zaak cannot be linked because of %s",
    async (reason) => {
      await setup();
      findLinkableZaken([
        makeFakeSearchResult({
          identificatie: "ZAAK-2026-003",
          isKoppelbaar: false,
          nietKoppelbaarReden: reason,
        }),
      ]);

      await chooseRelationType("DEELZAAK");
      await enterSearchCriterion();
      await clickSearch();

      expect(linkButtonOfRow("ZAAK-2026-003")).toHaveAccessibleDescription(
        `zaak.koppelen.niet-koppelbaar.${reason}`,
      );
    },
  );

  it("names the current zaak as the hoofdzaak when the found zaak would become the deelzaak", async () => {
    await setup({
      zaaktype: fromPartial<GeneratedType<"RestZaaktype">>({
        omschrijving: "Hoofdzaaktype",
      }),
    });
    translateZaaktypeForbidsDeelzaak();
    findLinkableZaken([
      makeFakeSearchResult({
        identificatie: "ZAAK-2026-003",
        isKoppelbaar: false,
        nietKoppelbaarReden: "ZAAKTYPE_DOES_NOT_ALLOW_DEELZAAK",
        zaaktypeOmschrijving: "Deelzaaktype",
      }),
    ]);

    await chooseRelationType("DEELZAAK");
    await enterSearchCriterion();
    await clickSearch();

    expect(linkButtonOfRow("ZAAK-2026-003")).toHaveAccessibleDescription(
      "Hoofdzaaktype staat geen deelzaken van Deelzaaktype toe",
    );
  });

  it("names the found zaak as the hoofdzaak when the current zaak would become the deelzaak", async () => {
    await setup({
      zaaktype: fromPartial<GeneratedType<"RestZaaktype">>({
        omschrijving: "Deelzaaktype",
      }),
    });
    translateZaaktypeForbidsDeelzaak();
    findLinkableZaken([
      makeFakeSearchResult({
        identificatie: "ZAAK-2026-003",
        isKoppelbaar: false,
        nietKoppelbaarReden: "ZAAKTYPE_DOES_NOT_ALLOW_DEELZAAK",
        zaaktypeOmschrijving: "Hoofdzaaktype",
      }),
    ]);

    await chooseRelationType("HOOFDZAAK");
    await enterSearchCriterion();
    await clickSearch();

    expect(linkButtonOfRow("ZAAK-2026-003")).toHaveAccessibleDescription(
      "Hoofdzaaktype staat geen deelzaken van Deelzaaktype toe",
    );
  });

  it("links the zaak of the row the button was clicked on", async () => {
    const { zaak, zaakLinked, utilService, sideNav } = await setup();
    findLinkableZaken([makeFakeSearchResult()]);

    await chooseRelationType("DEELZAAK");
    await enterSearchCriterion();
    await clickSearch();
    await user.click(linkButtonOfRow("ZAAK-2026-002"));
    await sleep();

    const request = httpTestingController.expectOne(KOPPEL_URL);
    expect(request.request.method).toBe("PATCH");
    expect(request.request.body).toEqual({
      zaakUuid: zaak.uuid,
      teKoppelenZaakUuid: "fake-result-uuid",
      relatieType: "DEELZAAK",
    });
    request.flush(null);
    await sleep();

    expect(utilService.openSnackbar).toHaveBeenCalledWith(
      "msg.zaak.gekoppeld",
      {
        case: "ZAAK-2026-002",
      },
    );
    expect(zaakLinked).toHaveBeenCalled();
    expect(sideNav.close).toHaveBeenCalled();
  });

  it("reports an error and keeps the panel open when linking fails", async () => {
    const { zaakLinked, foutAfhandelingService } = await setup();
    findLinkableZaken([makeFakeSearchResult()]);

    await chooseRelationType("DEELZAAK");
    await enterSearchCriterion();
    await clickSearch();
    await user.click(linkButtonOfRow("ZAAK-2026-002"));
    await sleep();
    httpTestingController
      .expectOne(KOPPEL_URL)
      .flush("boom", { status: 500, statusText: "Server Error" });
    await sleep();

    expect(foutAfhandelingService.foutAfhandelen).toHaveBeenCalled();
    expect(zaakLinked).not.toHaveBeenCalled();
  });

  it("disables only the link button of the row that is being linked", async () => {
    await setup();
    findLinkableZaken([
      makeFakeSearchResult({
        id: "clicked-uuid",
        identificatie: "ZAAK-2026-002",
      }),
      makeFakeSearchResult({
        id: "other-uuid",
        identificatie: "ZAAK-2026-003",
      }),
    ]);

    await chooseRelationType("DEELZAAK");
    await enterSearchCriterion();
    await clickSearch();
    await user.click(linkButtonOfRow("ZAAK-2026-002"));
    await sleep();
    fixture.detectChanges();

    const request = httpTestingController.expectOne(KOPPEL_URL);
    expect(linkButtonOfRow("ZAAK-2026-002")).toHaveAttribute(
      "aria-disabled",
      "true",
    );
    expect(linkButtonOfRow("ZAAK-2026-003")).not.toHaveAttribute(
      "aria-disabled",
      "true",
    );

    request.flush(null);
    await sleep();
  });

  it("does not link a zaak without an id", async () => {
    await setup();
    findLinkableZaken([makeFakeSearchResult({ id: undefined })]);

    await chooseRelationType("DEELZAAK");
    await enterSearchCriterion();
    await clickSearch();
    await user.click(linkButtonOfRow("ZAAK-2026-002"));
    await sleep();

    httpTestingController.expectNone(KOPPEL_URL);
  });

  it("closes the panel and clears the criteria when cancelled", async () => {
    const { sideNav } = await setup();

    await chooseRelationType("DEELZAAK");
    await user.click(screen.getByRole("button", { name: "actie.annuleren" }));
    fixture.detectChanges();

    expect(sideNav.close).toHaveBeenCalled();
    expect(screen.getByRole("button", { name: "actie.zoeken" })).toBeDisabled();
  });

  it("closes the panel when the close button is used", async () => {
    const { sideNav } = await setup();

    await user.click(
      screen.getByRole("button", { name: "actie.paneel.sluiten" }),
    );

    expect(sideNav.close).toHaveBeenCalled();
  });
});
