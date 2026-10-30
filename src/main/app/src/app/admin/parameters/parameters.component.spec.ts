/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpTestingController } from "@angular/common/http/testing";
import { signal } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatNativeDateModule } from "@angular/material/core";
import { MatSelectChange } from "@angular/material/select";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { provideTranslateService } from "@ngx-translate/core";
import { screen } from "@testing-library/angular";
import { fromPartial } from "src/test-helpers";
import { sleep } from "../../../../setupJest";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { UtilService } from "../../core/service/util.service";
import { SessionStorageUtil } from "../../shared/storage/session-storage.util";
import { ToggleSwitchOptions } from "../../shared/table-zoek-filters/toggle-filter/toggle-switch-options";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ParametersComponent } from "./parameters.component";
import { ZaakafhandelParametersListParameters } from "./zaakafhandel-parameters-list-parameters";

const createParametersComponent = () => {
  TestBed.configureTestingModule({
    providers: [
      provideTranslateService(),
      { provide: UtilService, useValue: fromPartial<UtilService>({}) },
      {
        provide: ConfiguratieService,
        useValue: fromPartial<ConfiguratieService>({}),
      },
    ],
  });
  return TestBed.runInInjectionContext(() => new ParametersComponent());
};

describe(`${ParametersComponent.name} applyFilter`, () => {
  const filterTestCases = [
    {
      event: ToggleSwitchOptions.CHECKED as ToggleSwitchOptions,
      filter: "valide" as keyof ZaakafhandelParametersListParameters,
      expectedValue: ToggleSwitchOptions.CHECKED,
    },
    {
      event: ToggleSwitchOptions.UNCHECKED as ToggleSwitchOptions,
      filter: "geldig" as keyof ZaakafhandelParametersListParameters,
      expectedValue: ToggleSwitchOptions.UNCHECKED,
    },
    {
      event: {
        value: { identificatie: "1", omschrijving: "Zaak test" },
      } as MatSelectChange,
      filter: "zaaktype" as keyof ZaakafhandelParametersListParameters,
      expectedValue: { identificatie: "1", omschrijving: "Zaak test" },
    },
    {
      event: { value: { key: "a", naam: "Case" } } as MatSelectChange,
      filter: "processDefinition" as keyof ZaakafhandelParametersListParameters,
      expectedValue: { key: "a", naam: "Case" },
    },
    {
      event: "name",
      filter: "sort" as keyof ZaakafhandelParametersListParameters,
      expectedValue: "name",
    },
    {
      event: "asc",
      filter: "order" as keyof ZaakafhandelParametersListParameters,
      expectedValue: "asc",
    },
    {
      event: 1,
      filter: "page" as keyof ZaakafhandelParametersListParameters,
      expectedValue: 1,
    },
    {
      event: 10,
      filter: "maxResults" as keyof ZaakafhandelParametersListParameters,
      expectedValue: 10,
    },
  ];

  it.each(filterTestCases)(
    "should update '%s' in filterParameters correctly",
    ({ event, filter, expectedValue }) => {
      const setItemSpy = jest
        .spyOn(SessionStorageUtil, "setItem")
        .mockImplementation(jest.fn());

      const component = createParametersComponent();

      component["storedParameterFilters"] = "test-key";
      component["filterParameters"] = {
        valide: ToggleSwitchOptions.INDETERMINATE,
        geldig: ToggleSwitchOptions.INDETERMINATE,
        zaaktype: null,
        processDefinition: null,
        beginGeldigheid: { van: null, tot: null },
        eindeGeldigheid: { van: null, tot: null },
        sort: "",
        order: "",
        page: 0,
        maxResults: 25,
      } satisfies ZaakafhandelParametersListParameters;

      component["applyFilter"]({
        event,
        filter,
      });

      expect(component["filterParameters"][filter]).toEqual(expectedValue);
      expect(setItemSpy).toHaveBeenCalledWith(
        "test-key",
        component["filterParameters"],
      );
    },
  );
});

describe(`${ParametersComponent.name} compare functions`, () => {
  describe("compareZaaktype", () => {
    it("should return true when identificatie matches", () => {
      const component = createParametersComponent();
      const a = fromPartial<GeneratedType<"RestZaaktype">>({
        identificatie: "ZT-001",
      });
      const b = fromPartial<GeneratedType<"RestZaaktype">>({
        identificatie: "ZT-001",
      });
      expect(component["compareZaaktype"](a, b)).toBe(true);
    });

    it("should return false when identificatie differs", () => {
      const component = createParametersComponent();
      const a = fromPartial<GeneratedType<"RestZaaktype">>({
        identificatie: "ZT-001",
      });
      const b = fromPartial<GeneratedType<"RestZaaktype">>({
        identificatie: "ZT-002",
      });
      expect(component["compareZaaktype"](a, b)).toBe(false);
    });
  });

  describe("compareProcessDefinition", () => {
    it("should return true when key matches", () => {
      const component = createParametersComponent();
      const a = fromPartial<GeneratedType<"RestProcessDefinition">>({
        key: "key-1",
      });
      const b = fromPartial<GeneratedType<"RestProcessDefinition">>({
        key: "key-1",
      });
      expect(component["compareProcessDefinition"](a, b)).toBe(true);
    });

    it("should return false when key differs", () => {
      const component = createParametersComponent();
      const a = fromPartial<GeneratedType<"RestProcessDefinition">>({
        key: "key-1",
      });
      const b = fromPartial<GeneratedType<"RestProcessDefinition">>({
        key: "key-2",
      });
      expect(component["compareProcessDefinition"](a, b)).toBe(false);
    });
  });
});

describe(ParametersComponent.name, () => {
  let fixture: ComponentFixture<ParametersComponent>;
  let httpTestingController: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ParametersComponent, NoopAnimationsModule, MatNativeDateModule],
      providers: [
        provideTranslateService(),
        {
          provide: UtilService,
          useValue: fromPartial<UtilService>({
            getUniqueItemsList: jest.fn().mockReturnValue([]),
            loading: signal(false),
          }),
        },
        { provide: ConfiguratieService, useValue: {} },
        provideRouter([]),
      ],
    }).compileComponents();

    httpTestingController = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ParametersComponent);
    fixture.detectChanges();
  });

  async function answerParametersWith(
    parameters: GeneratedType<"RestZaaktypeConfiguration">[],
  ) {
    await sleep();
    httpTestingController
      .expectOne("/rest/zaakafhandelparameters")
      .flush(parameters);
    await sleep();
    fixture.detectChanges();
    await sleep();
    fixture.detectChanges();
  }

  it("shows the loading message while the parameters are being read", async () => {
    await sleep();
    fixture.detectChanges();

    expect(screen.getByText("msg.loading")).toBeVisible();
  });

  it("shows the no-data message when no zaaktype has parameters", async () => {
    await answerParametersWith([]);

    expect(screen.getByText("msg.geen.gegevens.gevonden")).toBeVisible();
    expect(screen.queryByText("msg.loading")).not.toBeInTheDocument();
  });

  it("shows a row for every zaaktype it read the parameters of", async () => {
    await answerParametersWith([
      fromPartial<GeneratedType<"RestZaaktypeConfiguration">>({
        zaaktype: {
          uuid: "fakeZaaktypeUuid",
          identificatie: "fakeIdentificatie",
          omschrijving: "Fake zaaktype",
          isNuGeldig: true,
        },
        isValide: true,
      }),
    ]);

    expect(
      screen.getByRole("row", { name: /Fake zaaktype/ }),
    ).toBeInTheDocument();
    expect(
      screen.queryByText("msg.geen.gegevens.gevonden"),
    ).not.toBeInTheDocument();
  });
});
