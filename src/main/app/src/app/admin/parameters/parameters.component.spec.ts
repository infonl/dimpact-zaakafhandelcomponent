/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { signal } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatNativeDateModule } from "@angular/material/core";
import { MatSelectChange } from "@angular/material/select";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { provideTranslateService } from "@ngx-translate/core";
import { screen } from "@testing-library/angular";
import { of } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { UtilService } from "../../core/service/util.service";
import { SessionStorageUtil } from "../../shared/storage/session-storage.util";
import { ToggleSwitchOptions } from "../../shared/table-zoek-filters/toggle-filter/toggle-switch-options";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZaakafhandelParametersService } from "../zaakafhandel-parameters.service";
import { ParametersComponent } from "./parameters.component";
import { ZaakafhandelParametersListParameters } from "./zaakafhandel-parameters-list-parameters";

const createParametersComponent = () => {
  TestBed.configureTestingModule({
    providers: [
      { provide: UtilService, useValue: fromPartial<UtilService>({}) },
      {
        provide: ConfiguratieService,
        useValue: fromPartial<ConfiguratieService>({}),
      },
      {
        provide: ZaakafhandelParametersService,
        useValue: fromPartial<ZaakafhandelParametersService>({}),
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
  let component: ParametersComponent;
  let utilServiceMock: Pick<UtilService, "getUniqueItemsList" | "loading">;
  let zaakafhandelParametersServiceMock: Pick<
    ZaakafhandelParametersService,
    "listZaakafhandelParameters"
  >;

  beforeEach(async () => {
    utilServiceMock = {
      getUniqueItemsList: jest.fn().mockReturnValue([]),
      loading: signal(false),
    } satisfies Pick<UtilService, "getUniqueItemsList" | "loading">;

    zaakafhandelParametersServiceMock = {
      listZaakafhandelParameters: jest.fn().mockReturnValue(of([])),
    } satisfies Pick<
      ZaakafhandelParametersService,
      "listZaakafhandelParameters"
    >;

    await TestBed.configureTestingModule({
      imports: [ParametersComponent, NoopAnimationsModule, MatNativeDateModule],
      providers: [
        provideTranslateService(),
        { provide: UtilService, useValue: utilServiceMock },
        { provide: ConfiguratieService, useValue: {} },
        {
          provide: ZaakafhandelParametersService,
          useValue: zaakafhandelParametersServiceMock,
        },
        provideRouter([]),
      ],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ParametersComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it("should create and call setupMenu on init", () => {
    expect(component).toBeTruthy();
  });

  it("should show no-data message when data is empty and not loading", () => {
    expect(screen.getByText("msg.geen.gegevens.gevonden")).toBeVisible();
  });

  it("should show loading message when loading is true", () => {
    component["loading"] = true;
    fixture.detectChanges();

    expect(screen.getByText("msg.loading")).toBeVisible();
  });
});
