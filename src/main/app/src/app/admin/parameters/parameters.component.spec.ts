/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { signal } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatNativeDateModule } from "@angular/material/core";
import { MatSelectChange } from "@angular/material/select";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { fromPartial } from "src/test-helpers";
import { of } from "rxjs";
import { DatumRange } from "src/app/zoeken/model/datum-range";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { UtilService } from "../../core/service/util.service";
import { SessionStorageUtil } from "../../shared/storage/session-storage.util";
import { ToggleSwitchOptions } from "../../shared/table-zoek-filters/toggle-filter/toggle-switch-options";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZaakafhandelParametersService } from "../zaakafhandel-parameters.service";
import { ParametersComponent } from "./parameters.component";
import { ZaakafhandelParametersListParameters } from "./zaakafhandel-parameters-list-parameters";

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
      filter: "caseDefinition" as keyof ZaakafhandelParametersListParameters,
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

      const component = new ParametersComponent(
        fromPartial<UtilService>({}),
        fromPartial<ConfiguratieService>({}),
        fromPartial<ZaakafhandelParametersService>({}),
      );

      component["storedParameterFilters"] = "test-key";
      component["filterParameters"] = {
        valide: ToggleSwitchOptions.INDETERMINATE,
        geldig: ToggleSwitchOptions.INDETERMINATE,
        zaaktype: null,
        caseDefinition: null,
        beginGeldigheid: new DatumRange(),
        eindeGeldigheid: new DatumRange(),
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
  const makeComponent = () =>
    new ParametersComponent(
      fromPartial<UtilService>({}),
      fromPartial<ConfiguratieService>({}),
      fromPartial<ZaakafhandelParametersService>({}),
    );

  describe("compareZaaktype", () => {
    it("should return true when identificatie matches", () => {
      const component = makeComponent();
      const a = fromPartial<GeneratedType<"RestZaaktype">>({
        identificatie: "ZT-001",
      });
      const b = fromPartial<GeneratedType<"RestZaaktype">>({
        identificatie: "ZT-001",
      });
      expect(component["compareZaaktype"](a, b)).toBe(true);
    });

    it("should return false when identificatie differs", () => {
      const component = makeComponent();
      const a = fromPartial<GeneratedType<"RestZaaktype">>({
        identificatie: "ZT-001",
      });
      const b = fromPartial<GeneratedType<"RestZaaktype">>({
        identificatie: "ZT-002",
      });
      expect(component["compareZaaktype"](a, b)).toBe(false);
    });
  });

  describe("compareCaseDefinition", () => {
    it("should return true when key matches", () => {
      const component = makeComponent();
      const a = fromPartial<GeneratedType<"RESTCaseDefinition">>({
        key: "key-1",
      });
      const b = fromPartial<GeneratedType<"RESTCaseDefinition">>({
        key: "key-1",
      });
      expect(component["compareCaseDefinition"](a, b)).toBe(true);
    });

    it("should return false when key differs", () => {
      const component = makeComponent();
      const a = fromPartial<GeneratedType<"RESTCaseDefinition">>({
        key: "key-1",
      });
      const b = fromPartial<GeneratedType<"RESTCaseDefinition">>({
        key: "key-2",
      });
      expect(component["compareCaseDefinition"](a, b)).toBe(false);
    });
  });
});

describe(ParametersComponent.name, () => {
  let fixture: ComponentFixture<ParametersComponent>;
  let component: ParametersComponent;
  let utilServiceMock: Pick<
    UtilService,
    "setTitle" | "getUniqueItemsList" | "loading"
  >;
  let zaakafhandelParametersServiceMock: Pick<
    ZaakafhandelParametersService,
    "listZaakafhandelParameters"
  >;

  beforeEach(async () => {
    utilServiceMock = {
      setTitle: jest.fn(),
      getUniqueItemsList: jest.fn().mockReturnValue([]),
      loading: signal(false),
    } satisfies Pick<
      UtilService,
      "setTitle" | "getUniqueItemsList" | "loading"
    >;

    zaakafhandelParametersServiceMock = {
      listZaakafhandelParameters: jest.fn().mockReturnValue(of([])),
    } satisfies Pick<
      ZaakafhandelParametersService,
      "listZaakafhandelParameters"
    >;

    await TestBed.configureTestingModule({
      imports: [
        ParametersComponent,
        NoopAnimationsModule,
        MatNativeDateModule,
        TranslateModule.forRoot(),
      ],
      providers: [
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
    expect(utilServiceMock.setTitle).toHaveBeenCalledWith(
      "title.parameters",
      undefined,
    );
  });

  it("should show no-data message when data is empty and not loading", () => {
    const paragraphs = Array.from(
      fixture.nativeElement.querySelectorAll("p"),
    ) as HTMLElement[];
    expect(
      paragraphs.some((p) =>
        p.textContent?.includes("msg.geen.gegevens.gevonden"),
      ),
    ).toBe(true);
  });

  it("should show loading message when loading is true", () => {
    component["loading"] = true;
    fixture.detectChanges();
    const paragraphs = Array.from(
      fixture.nativeElement.querySelectorAll("p"),
    ) as HTMLElement[];
    expect(paragraphs.some((p) => p.textContent?.includes("msg.loading"))).toBe(
      true,
    );
  });
});
