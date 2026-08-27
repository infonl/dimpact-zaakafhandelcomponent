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

  const screen = () => within(fixture.nativeElement as HTMLElement);

  const hasDetailField = (label: string) =>
    screen().queryByText(label) !== null;

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
      providers: [{ provide: LOCALE_ID, useValue: "nl-NL" }],
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
        "warns on the fataledatum alone when only that date has passed",
        {
          einddatum: null,
          einddatumGepland: null,
          uiterlijkeEinddatumAfdoening: yesterdayDate,
        },
        1,
      ],
      [
        "warns on both the streefdatum and the fataledatum when both have passed",
        {
          einddatum: null,
          einddatumGepland: yesterdayDate,
          uiterlijkeEinddatumAfdoening: yesterdayDate,
        },
        2,
      ],
      [
        "warns on neither date when the zaak has no streefdatum nor fataledatum",
        {
          einddatum: null,
          einddatumGepland: null,
          uiterlijkeEinddatumAfdoening: null,
        },
        0,
      ],
      [
        "warns on neither date when both are still in the future",
        {
          einddatum: null,
          einddatumGepland: tomorrowDate,
          uiterlijkeEinddatumAfdoening: tomorrowDate,
        },
        0,
      ],
      [
        "warns on neither date when the zaak was afgehandeld before both",
        {
          einddatum: today,
          einddatumGepland: tomorrowDate,
          uiterlijkeEinddatumAfdoening: tomorrowDate,
        },
        0,
      ],
    ])("%s", async (_scenario, zaakData, expectedIcons) => {
      renderZaak({ ...zaak, ...zaakData });

      const icons = await loader.getAllHarnesses(
        MatIconHarness.with({ name: "report_problem" }),
      );

      expect(icons.length).toBe(expectedIcons);
    });
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
    // the remark renders its mat-icon ligature before the translation key, so the
    // key is anchored at the end to tell the plural key from the singular one
    const hasRemarkKey = (key: string) =>
      screen().queryAllByText(new RegExp(`${key.replace(/\./g, "\\.")}$`))
        .length > 0;

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

      expect(hasRemarkKey("duurDagenOpschorting")).toBe(true);
      expect(hasRemarkKey("duurDagenOpschorting.enkelvoud")).toBe(false);
    });

    it("uses the singular translation key when the zaak is opgeschort for one day", () => {
      renderOpschorting(1);

      expect(hasRemarkKey("duurDagenOpschorting.enkelvoud")).toBe(true);
    });

    it("shows the verlenging duur when the zaak has been verlengd", () => {
      renderZaak({ ...zaak, duurVerlenging: "3" });

      expect(hasRemarkKey("duurVerlenging")).toBe(true);
    });

    it("shows no remark when the zaak is neither opgeschort nor verlengd", () => {
      renderZaak(zaak);

      expect(hasRemarkKey("duurDagenOpschorting")).toBe(false);
      expect(hasRemarkKey("duurVerlenging")).toBe(false);
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
    const zaakWithAfleidingswijze = (
      afleidingswijze: GeneratedType<"AfleidingswijzeEnum">,
      datumKenmerkOmschrijving?: string,
    ) => ({
      ...zaak,
      resultaat: fromPartial<GeneratedType<"RestZaakResultaat">>({
        resultaattype: fromPartial<GeneratedType<"RestResultaattype">>({
          datumKenmerkOmschrijving,
          bronArchiefprocedure: fromPartial<
            GeneratedType<"BrondatumArchiefprocedure">
          >({
            afleidingswijze,
          }),
        }),
      }),
    });

    it("should show the field when afleidingswijze is set", () => {
      renderZaak(zaakWithAfleidingswijze("TERMIJN"));

      expect(
        screen().getByText("afleidingswijzeBrondatum.TERMIJN"),
      ).toBeInTheDocument();
    });

    it("should show the datumKenmerkOmschrijving when afleidingswijze is EIGENSCHAP", () => {
      renderZaak(
        zaakWithAfleidingswijze("EIGENSCHAP", "fakeDatumKenmerkOmschrijving"),
      );

      expect(
        screen().getByText("fakeDatumKenmerkOmschrijving"),
      ).toBeInTheDocument();
    });

    it("should not show the field when resultaat is absent", () => {
      renderZaak({ ...zaak, resultaat: null });

      expect(hasDetailField("afleidingswijzeBrondatum")).toBe(false);
    });

    it("should not show the field when resultaattype is absent", () => {
      renderZaak({
        ...zaak,
        resultaat: fromPartial<GeneratedType<"RestZaakResultaat">>({
          resultaattype: null,
        }),
      });

      expect(hasDetailField("afleidingswijzeBrondatum")).toBe(false);
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

      expect(hasDetailField("afleidingswijzeBrondatum")).toBe(false);
    });
  });

  describe("zaak detail grid", () => {
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

    it("should render date fields with the non-breaking hyphens of the datum pipe", () => {
      renderZaak({
        ...zaak,
        registratiedatum: "2026-01-15",
        einddatum: "2026-03-31",
      });

      expect(screen().getByText("15‑01‑2026")).toBeInTheDocument();
      expect(screen().getByText("31‑03‑2026")).toBeInTheDocument();
    });

    it("should close rows three and six when all seven fields are shown", () => {
      renderZaak(zaakWithAllDetailFields);

      for (const label of [
        "status",
        "registratiedatum",
        "resultaat",
        "einddatum",
        "startdatumBewaartermijn",
        "afleidingswijzeBrondatum",
        "archiefNominatie",
      ]) {
        expect(hasDetailField(label)).toBe(true);
      }
      expect(gridPlaceholderCount()).toBe(2);
    });

    it("should close only row three when the four remaining fields are shown", () => {
      renderZaak({
        ...zaak,
        rechten: { ...zaak.rechten, wijzigen: true },
        einddatum: null,
        startdatumBewaartermijn: null,
        archiefNominatie: "VERNIETIGEN",
        resultaat: null,
      });

      for (const label of [
        "status",
        "registratiedatum",
        "resultaat",
        "archiefNominatie.datum.VERNIETIGEN",
      ]) {
        expect(hasDetailField(label)).toBe(true);
      }
      expect(hasDetailField("einddatum")).toBe(false);
      expect(hasDetailField("startdatumBewaartermijn")).toBe(false);
      expect(hasDetailField("afleidingswijzeBrondatum")).toBe(false);
      expect(gridPlaceholderCount()).toBe(1);
    });

    it("should fill the action column on top of the row closers when the user may not edit the zaak", async () => {
      renderZaak({
        ...zaakWithAllDetailFields,
        rechten: { ...zaak.rechten, wijzigen: false, toekennen: false },
      });

      const editIcon = await loader.getHarnessOrNull(
        MatIconHarness.with({ name: "edit" }),
      );

      expect(editIcon).toBeNull();
      expect(gridPlaceholderCount()).toBe(3);
    });
  });
});
