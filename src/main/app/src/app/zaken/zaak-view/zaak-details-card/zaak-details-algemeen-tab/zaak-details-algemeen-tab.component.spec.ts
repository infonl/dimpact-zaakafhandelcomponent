/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { LOCALE_ID } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatIconHarness } from "@angular/material/icon/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { within } from "@testing-library/angular";
import moment from "moment";
import { fromPartial } from "src/test-helpers";
import { StaticTextComponent } from "../../../../shared/static-text/static-text.component";
import { GeneratedType } from "../../../../shared/utils/generated-types";
import { ZaakDetailsAlgemeenTabComponent } from "./zaak-details-algemeen-tab.component";

describe(ZaakDetailsAlgemeenTabComponent.name, () => {
  let fixture: ComponentFixture<ZaakDetailsAlgemeenTabComponent>;
  let loader: HarnessLoader;

  const zaak = fromPartial<GeneratedType<"RestZaak">>({
    uuid: "1234",
    zaaktype: fromPartial<GeneratedType<"RestZaaktype">>({
      omschrijving: "mock description",
    }),
    indicaties: [],
    rechten: {
      behandelen: true,
    },
    groep: {},
    vertrouwelijkheidaanduiding: "OPENBAAR",
  });

  const renderZaak = (zaakToRender: GeneratedType<"RestZaak">) => {
    fixture.componentRef.setInput("zaak", zaakToRender);
    fixture.detectChanges();
  };

  const detailFields = () =>
    fixture.debugElement
      .queryAll((debugElement) => debugElement.name === "zac-static-text")
      .map(
        (debugElement) => debugElement.componentInstance as StaticTextComponent,
      );

  const detailFieldLabels = () => detailFields().map(({ label }) => label);

  const findDetailField = (label: string) =>
    detailFields().find((staticText) => staticText.label === label);

  const screen = () => within(fixture.nativeElement as HTMLElement);

  // the grid placeholders are decorative aria-hidden fillers with no role,
  // accessible name or text, so no Testing Library query can reach them
  const gridPlaceholderCount = () =>
    // eslint-disable-next-line no-restricted-syntax, testing-library/no-node-access
    (fixture.nativeElement as HTMLElement).querySelectorAll(
      ".zaak-grid .grid-placeholder",
    ).length;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        ZaakDetailsAlgemeenTabComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [
        // matches the locale the app provides, so dates format as they do in production
        { provide: LOCALE_ID, useValue: "nl-NL" },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ZaakDetailsAlgemeenTabComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
  });

  describe("dateFieldIconMap icon logic", () => {
    const yesterdayDate = moment().subtract(1, "days").format("YYYY-MM-DD");
    const today = moment().format("YYYY-MM-DD");
    const tomorrowDate = moment().add(1, "days").format("YYYY-MM-DD");

    it.each([
      [
        {
          einddatum: null,
          einddatumGepland: null,
          uiterlijkeEinddatumAfdoening: yesterdayDate,
        },
        1,
      ],
      [
        {
          einddatum: null,
          einddatumGepland: yesterdayDate,
          uiterlijkeEinddatumAfdoening: yesterdayDate,
        },
        2,
      ],
      [
        {
          einddatum: null,
          einddatumGepland: null,
          uiterlijkeEinddatumAfdoening: null,
        },
        0,
      ],
      [
        {
          einddatum: null,
          einddatumGepland: tomorrowDate,
          uiterlijkeEinddatumAfdoening: tomorrowDate,
        },
        0,
      ],
      [
        {
          einddatum: today,
          einddatumGepland: tomorrowDate,
          uiterlijkeEinddatumAfdoening: tomorrowDate,
        },
        0,
      ],
    ])(
      "shows the correct warning icons for overdue data",
      async (zaakData, expectedIcons) => {
        renderZaak({ ...zaak, ...zaakData });

        const icons = await loader.getAllHarnesses(
          MatIconHarness.with({ name: "report_problem" }),
        );

        expect(icons.length).toBe(expectedIcons);
      },
    );
  });

  describe("inactive group indicator", () => {
    it("should show 'inactief' label when groep is inactive", () => {
      renderZaak({
        ...zaak,
        groep: { id: "g1", naam: "Groep A", active: false },
      });

      expect(screen().getByText("(inactief)")).toBeInTheDocument();
    });

    it("should not show 'inactief' label when groep is active", () => {
      renderZaak({
        ...zaak,
        groep: { id: "g1", naam: "Groep A", active: true },
      });

      expect(screen().queryByText("(inactief)")).toBeNull();
    });

    it("should not crash when groep is null", () => {
      renderZaak({ ...zaak, groep: null });

      expect(screen().queryByText("(inactief)")).toBeNull();
    });
  });

  describe("opschorting en verlenging remark", () => {
    const hasRemark = (translationKey: string) =>
      screen().queryAllByText(translationKey, { exact: false }).length > 0;

    const renderOpschorting = (duurDagen: number) => {
      fixture.componentRef.setInput("zaak", zaak);
      fixture.componentRef.setInput(
        "zaakOpschorting",
        fromPartial<GeneratedType<"RESTZaakOpschorting">>({ duurDagen }),
      );
      fixture.detectChanges();
    };

    it("uses the plural translation key when the zaak is opgeschort for several days", () => {
      renderOpschorting(5);

      expect(hasRemark("duurDagenOpschorting")).toBe(true);
    });

    it("uses the singular translation key when the zaak is opgeschort for one day", () => {
      renderOpschorting(1);

      expect(hasRemark("duurDagenOpschorting.enkelvoud")).toBe(true);
    });

    it("shows the verlenging duur when the zaak has been verlengd", () => {
      renderZaak({ ...zaak, duurVerlenging: "3" });

      expect(hasRemark("duurVerlenging")).toBe(true);
    });

    it("shows no remark when the zaak is neither opgeschort nor verlengd", () => {
      renderZaak(zaak);

      expect(hasRemark("duurDagenOpschorting")).toBe(false);
      expect(hasRemark("duurVerlenging")).toBe(false);
    });
  });

  describe("zaak details edit button", () => {
    const editButton = () => screen().queryByRole("button");

    it("emits editCaseDetails when the user may edit the zaak", () => {
      const editCaseDetails = jest.fn();
      renderZaak({ ...zaak, rechten: { ...zaak.rechten, wijzigen: true } });
      fixture.componentInstance.editCaseDetails.subscribe(editCaseDetails);

      editButton()?.click();

      expect(editCaseDetails).toHaveBeenCalled();
    });

    it("renders no edit button when the user may neither wijzigen nor toekennen", () => {
      renderZaak({
        ...zaak,
        rechten: { ...zaak.rechten, wijzigen: false, toekennen: false },
      });

      expect(editButton()).toBeNull();
    });
  });

  describe("afleidingswijzeBrondatum", () => {
    const findAfleidingswijzeField = () =>
      findDetailField("afleidingswijzeBrondatum");

    it("should show the field when afleidingswijze is set", () => {
      renderZaak({
        ...zaak,
        resultaat: fromPartial<GeneratedType<"RestZaakResultaat">>({
          resultaattype: fromPartial<GeneratedType<"RestResultaattype">>({
            bronArchiefprocedure: fromPartial<
              GeneratedType<"BrondatumArchiefprocedure">
            >({
              afleidingswijze: "TERMIJN",
            }),
          }),
        }),
      });

      expect(findAfleidingswijzeField()?.value).toBe(
        "afleidingswijzeBrondatum.TERMIJN",
      );
    });

    it("should show the datumKenmerkOmschrijving when afleidingswijze is EIGENSCHAP", () => {
      renderZaak({
        ...zaak,
        resultaat: fromPartial<GeneratedType<"RestZaakResultaat">>({
          resultaattype: fromPartial<GeneratedType<"RestResultaattype">>({
            datumKenmerkOmschrijving: "fakeDatumKenmerkOmschrijving",
            bronArchiefprocedure: fromPartial<
              GeneratedType<"BrondatumArchiefprocedure">
            >({
              afleidingswijze: "EIGENSCHAP",
            }),
          }),
        }),
      });

      expect(findAfleidingswijzeField()?.value).toBe(
        "fakeDatumKenmerkOmschrijving",
      );
    });

    it("should not show the field when resultaat is absent", () => {
      renderZaak({ ...zaak, resultaat: null });

      expect(findAfleidingswijzeField()).toBeUndefined();
    });

    it("should not show the field when resultaattype is absent", () => {
      renderZaak({
        ...zaak,
        resultaat: fromPartial<GeneratedType<"RestZaakResultaat">>({
          resultaattype: null,
        }),
      });

      expect(findAfleidingswijzeField()).toBeUndefined();
    });

    it("should not show the field when bronArchiefprocedure is absent", () => {
      renderZaak({
        ...zaak,
        resultaat: fromPartial<GeneratedType<"RestZaakResultaat">>({
          resultaattype: fromPartial<GeneratedType<"RestResultaattype">>({
            bronArchiefprocedure: null,
          }),
        }),
      });

      expect(findAfleidingswijzeField()).toBeUndefined();
    });
  });

  describe("zaak detail grid", () => {
    // the edit button occupies the action column, so grant the right that renders
    // it — otherwise its @else placeholder is counted along with the row closers
    const zaakWithAllDetailFields = {
      ...zaak,
      rechten: { ...zaak.rechten, wijzigen: true },
      einddatum: "2026-01-15",
      startdatumBewaartermijn: "2026-02-15",
      archiefNominatie: "BLIJVEND_BEWAREN",
      resultaat: fromPartial<GeneratedType<"RestZaakResultaat">>({
        resultaattype: fromPartial<GeneratedType<"RestResultaattype">>({
          naam: "fakeResultaattypeNaam",
          bronArchiefprocedure: fromPartial<
            GeneratedType<"BrondatumArchiefprocedure">
          >({
            afleidingswijze: "TERMIJN",
          }),
        }),
      }),
    } satisfies GeneratedType<"RestZaak">;

    it("should format date fields with the datum pipe", () => {
      renderZaak({
        ...zaak,
        registratiedatum: "2026-01-15",
        einddatum: "2026-03-31",
      });

      // the datum pipe renders non-breaking hyphens so a date never wraps
      expect(findDetailField("registratiedatum")?.value).toBe("15‑01‑2026");
      expect(findDetailField("einddatum")?.value).toBe("31‑03‑2026");
    });

    it("should close every row of three fields when all fields are shown", () => {
      renderZaak(zaakWithAllDetailFields);

      expect(detailFieldLabels()).toEqual(
        expect.arrayContaining([
          "status",
          "registratiedatum",
          "resultaat",
          "einddatum",
          "startdatumBewaartermijn",
          "afleidingswijzeBrondatum",
          "archiefNominatie",
        ]),
      );
      // seven visible fields, so rows three and six need closing
      expect(gridPlaceholderCount()).toBe(2);
    });

    it("should keep the rows aligned when conditional fields are hidden", () => {
      renderZaak({
        ...zaak,
        rechten: { ...zaak.rechten, wijzigen: true },
        einddatum: null,
        startdatumBewaartermijn: null,
        archiefNominatie: "VERNIETIGEN",
        resultaat: null,
      });

      const labels = detailFieldLabels();

      expect(labels).toEqual(
        expect.arrayContaining([
          "status",
          "registratiedatum",
          "resultaat",
          "archiefNominatie.datum.VERNIETIGEN",
        ]),
      );
      expect(labels).not.toContain("einddatum");
      expect(labels).not.toContain("startdatumBewaartermijn");
      expect(labels).not.toContain("afleidingswijzeBrondatum");
      // four visible fields, so only row three needs closing
      expect(gridPlaceholderCount()).toBe(1);
    });

    it("should fill the action column when the user may not edit the zaak", async () => {
      renderZaak({
        ...zaakWithAllDetailFields,
        rechten: { ...zaak.rechten, wijzigen: false, toekennen: false },
      });

      const editIcon = await loader.getHarnessOrNull(
        MatIconHarness.with({ name: "edit" }),
      );

      expect(editIcon).toBeNull();
      // the two row closers plus one standing in for the missing edit button
      expect(gridPlaceholderCount()).toBe(3);
    });
  });
});
