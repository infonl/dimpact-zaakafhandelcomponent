/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { NgIf } from "@angular/common";
import { provideHttpClient } from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { Component } from "@angular/core";
import {
  ComponentFixture,
  TestBed,
  fakeAsync,
  tick,
} from "@angular/core/testing";
import { MatButtonModule } from "@angular/material/button";
import { MatCardModule } from "@angular/material/card";
import { MatExpansionModule } from "@angular/material/expansion";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatIconModule } from "@angular/material/icon";
import { MatInputModule } from "@angular/material/input";
import { MatSidenavModule } from "@angular/material/sidenav";
import { MatSortModule } from "@angular/material/sort";
import { MatSortHarness } from "@angular/material/sort/testing";
import { MatTableModule } from "@angular/material/table";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { screen, within } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { of } from "rxjs";
import { delay } from "rxjs/operators";
import { sleep, testQueryClient } from "../../../../setupJest";
import { createMutationOptions, fromPartial } from "../../../test-helpers";
import { UtilService } from "../../core/service/util.service";
import { DatumPipe } from "../../shared/pipes/datum.pipe";
import { ReadMoreComponent } from "../../shared/read-more/read-more.component";
import { SideNavComponent } from "../../shared/side-nav/side-nav.component";
import { ToggleFilterComponent } from "../../shared/table-zoek-filters/toggle-filter/toggle-filter.component";
import { ToggleSwitchOptions } from "../../shared/table-zoek-filters/toggle-filter/toggle-switch-options";
import { GeneratedType } from "../../shared/utils/generated-types";
import { VersionComponent } from "../../shared/version/version.component";
import { HealthCheckService } from "../health-check.service";
import { InrichtingscheckComponent } from "./inrichtingscheck.component";

const VALIDE_ICON_LABEL = "healthCheck.zaaktype.status.valide";
const WAARSCHUWING_ICON_LABEL = "healthCheck.zaaktype.status.waarschuwing";
const NIET_VALIDE_ICON_LABEL = "healthCheck.zaaktype.status.niet-valide";
const ROLTYPE_ONTBREEKT_MESSAGE =
  "healthCheck.zaaktype.zaakspecifieke-autorisatie.roltype-ontbreekt";
const EIGENSCHAP_ONTBREEKT_MESSAGE =
  "healthCheck.zaaktype.zaakspecifieke-autorisatie.eigenschap-ontbreekt";

const volledigIngerichtZaaktype = fromPartial<
  GeneratedType<"RESTZaaktypeInrichtingscheck">
>({
  zaaktype: {
    uuid: "fakeZaaktypeUuid1",
    omschrijving: "Zaaktype A",
    doel: "Doel A",
    beginGeldigheid: "2024-01-01",
  },
  valide: true,
  heeftWaarschuwingen: false,
  zaakafhandelParametersValide: true,
  statustypeIntakeAanwezig: true,
  statustypeInBehandelingAanwezig: true,
  statustypeHeropendAanwezig: true,
  statustypeAanvullendeInformatieVereist: true,
  statustypeAfgerondAanwezig: true,
  statustypeAfgerondLaatsteVolgnummer: true,
  resultaattypeAanwezig: true,
  aantalInitiatorroltypen: 1,
  aantalBehandelaarroltypen: 1,
  rolOverigeAanwezig: true,
  informatieobjecttypeEmailAanwezig: true,
  resultaattypesMetVerplichtBesluit: [],
  besluittypeAanwezig: true,
  brpInstellingenCorrect: true,
  isZaakspecifiekeAutorisatieEigenschapAanwezig: true,
  isZaakspecifiekeAutorisatieRoltypeAanwezig: true,
});

const nietValideZaaktype = fromPartial<
  GeneratedType<"RESTZaaktypeInrichtingscheck">
>({
  ...volledigIngerichtZaaktype,
  zaaktype: {
    uuid: "fakeZaaktypeUuid2",
    omschrijving: "Zaaktype B",
    doel: "Doel B",
    beginGeldigheid: "2024-06-01",
  },
  valide: false,
  zaakafhandelParametersValide: false,
});

const zaaktypeZonderRoltype = fromPartial<
  GeneratedType<"RESTZaaktypeInrichtingscheck">
>({
  ...volledigIngerichtZaaktype,
  zaaktype: {
    uuid: "fakeZaaktypeUuid3",
    omschrijving: "Zaaktype C",
    doel: "Doel C",
    beginGeldigheid: "2024-07-01",
  },
  heeftWaarschuwingen: true,
  isZaakspecifiekeAutorisatieEigenschapAanwezig: true,
  isZaakspecifiekeAutorisatieRoltypeAanwezig: false,
});

const zaaktypeZonderEigenschap = fromPartial<
  GeneratedType<"RESTZaaktypeInrichtingscheck">
>({
  ...volledigIngerichtZaaktype,
  zaaktype: {
    uuid: "fakeZaaktypeUuid4",
    omschrijving: "Zaaktype D",
    doel: "Doel D",
    beginGeldigheid: "2024-08-01",
  },
  heeftWaarschuwingen: true,
  isZaakspecifiekeAutorisatieEigenschapAanwezig: false,
  isZaakspecifiekeAutorisatieRoltypeAanwezig: true,
});

@Component({
  templateUrl: "./inrichtingscheck.component.html",
  standalone: true,
  imports: [
    NgIf,
    MatSidenavModule,
    MatCardModule,
    MatExpansionModule,
    MatIconModule,
    MatTableModule,
    MatSortModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    TranslateModule,
    DatumPipe,
    SideNavComponent,
    ToggleFilterComponent,
    VersionComponent,
    ReadMoreComponent,
  ],
})
class TestInrichtingscheckComponent extends InrichtingscheckComponent {}

describe(InrichtingscheckComponent.name, () => {
  let fixture: ComponentFixture<TestInrichtingscheckComponent>;
  let component: TestInrichtingscheckComponent;
  let loader: HarnessLoader;
  let healthCheckService: HealthCheckService;
  let clearZTCCachesMutation: ReturnType<typeof createMutationOptions<string>>;
  let utilServiceMock: Pick<UtilService, "setTitle" | "openSnackbar">;

  const user = userEvent.setup();

  function setValideFilter(option: ToggleSwitchOptions) {
    component["valideFilter"] = option;
    component["applyFilter"]();
    fixture.detectChanges();
  }

  function zaaktypeRow(omschrijving: string) {
    return screen.getByRole("row", { name: new RegExp(omschrijving) });
  }

  beforeEach(async () => {
    utilServiceMock = {
      setTitle: jest.fn(),
      openSnackbar: jest.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [
        TestInrichtingscheckComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideQueryClient(testQueryClient),
        provideRouter([]),
        { provide: UtilService, useValue: utilServiceMock },
      ],
    }).compileComponents();

    healthCheckService = TestBed.inject(HealthCheckService);

    jest
      .spyOn(healthCheckService, "listZaaktypeInrichtingschecks")
      .mockReturnValue(
        of([
          volledigIngerichtZaaktype,
          nietValideZaaktype,
          zaaktypeZonderRoltype,
          zaaktypeZonderEigenschap,
        ]).pipe(delay(0)) as ReturnType<
          typeof healthCheckService.listZaaktypeInrichtingschecks
        >,
      );
    jest
      .spyOn(healthCheckService, "readBestaatCommunicatiekanaalEformulier")
      .mockReturnValue(
        of(true).pipe(delay(0)) as ReturnType<
          typeof healthCheckService.readBestaatCommunicatiekanaalEformulier
        >,
      );
    jest
      .spyOn(healthCheckService, "readZTCCacheTime")
      .mockReturnValue(of("2024-01-01T12:00:00").pipe(delay(0)));
    jest
      .spyOn(healthCheckService, "readBuildInformatie")
      .mockReturnValue(
        of(null).pipe(delay(0)) as unknown as ReturnType<
          typeof healthCheckService.readBuildInformatie
        >,
      );
    clearZTCCachesMutation = createMutationOptions("2024-03-19T10:00:00");
    jest
      .spyOn(healthCheckService, "clearZTCCaches")
      .mockReturnValue(clearZTCCachesMutation as never);
  });

  beforeEach(fakeAsync(() => {
    fixture = TestBed.createComponent(TestInrichtingscheckComponent);
    component = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
    tick(0);
    fixture.detectChanges();
  }));

  it("should call setTitle on init", () => {
    expect(utilServiceMock.setTitle).toHaveBeenCalledWith(
      "title.inrichtingscheck",
      undefined,
    );
  });

  it("should call all three health-check endpoints on init", () => {
    expect(healthCheckService.listZaaktypeInrichtingschecks).toHaveBeenCalled();
    expect(
      healthCheckService.readBestaatCommunicatiekanaalEformulier,
    ).toHaveBeenCalled();
    expect(healthCheckService.readZTCCacheTime).toHaveBeenCalled();
  });

  it("should populate dataSource with zaaktype data after init", () => {
    expect(component["dataSource"].data.length).toBe(4);
  });

  it("should store ztcCacheTime from service response", () => {
    expect(component["ztcCacheTime"]).toBe("2024-01-01T12:00:00");
  });

  it("should store bestaatCommunicatiekanaalEformulier from service response", () => {
    expect(component["bestaatCommunicatiekanaalEformulier"]).toBe(true);
  });

  it("should set loadingZaaktypes to false after data loads", () => {
    expect(component["loadingZaaktypes"]).toBe(false);
  });

  it("should show the valide icon and no expand affordance for a fully configured zaaktype", () => {
    setValideFilter(ToggleSwitchOptions.CHECKED);

    const row = zaaktypeRow("Zaaktype A");

    expect(
      within(row).getByRole("img", { name: VALIDE_ICON_LABEL }),
    ).toBeInTheDocument();
    expect(
      within(row).queryByRole("img", { name: WAARSCHUWING_ICON_LABEL }),
    ).not.toBeInTheDocument();
    expect(
      within(row).queryByText("keyboard_arrow_down"),
    ).not.toBeInTheDocument();
    expect(row).toHaveClass("ok");
  });

  it("should not expand a fully configured zaaktype on click", async () => {
    setValideFilter(ToggleSwitchOptions.CHECKED);

    await user.click(zaaktypeRow("Zaaktype A"));

    expect(component["expandedRow"]).toBeNull();
  });

  it("should show the error icon for a zaaktype that is not valide", () => {
    const row = zaaktypeRow("Zaaktype B");

    expect(
      within(row).getByRole("img", { name: NIET_VALIDE_ICON_LABEL }),
    ).toBeInTheDocument();
    expect(row).toHaveClass("error");
  });

  it("should expand a zaaktype that is not valide on click and collapse it on second click", async () => {
    const row = zaaktypeRow("Zaaktype B");

    expect(component["expandedRow"]).toBeNull();

    await user.click(row);
    expect(component["expandedRow"]).toBe(nietValideZaaktype);

    await user.click(row);
    expect(component["expandedRow"]).toBeNull();
  });

  it("should show the waarschuwing icon and an expand affordance for a valide zaaktype without the roltype", () => {
    const row = zaaktypeRow("Zaaktype C");

    expect(
      within(row).getByRole("img", { name: WAARSCHUWING_ICON_LABEL }),
    ).toBeInTheDocument();
    expect(
      within(row).queryByRole("img", { name: NIET_VALIDE_ICON_LABEL }),
    ).not.toBeInTheDocument();
    expect(within(row).getByText("keyboard_arrow_down")).toBeInTheDocument();
    expect(row).toHaveClass("warning");
  });

  it("should expand a valide zaaktype without the roltype and reveal that the roltype is missing", async () => {
    await user.click(zaaktypeRow("Zaaktype C"));
    fixture.detectChanges();

    expect(component["expandedRow"]).toBe(zaaktypeZonderRoltype);

    const detailRow = screen.getByRole("row", {
      name: new RegExp(ROLTYPE_ONTBREEKT_MESSAGE),
    });
    expect(within(detailRow).getByText("rol")).toBeInTheDocument();
    expect(
      within(detailRow).getByText(ROLTYPE_ONTBREEKT_MESSAGE),
    ).toBeInTheDocument();
  });

  it("should expand a valide zaaktype without the eigenschap and reveal that the eigenschap is missing", async () => {
    await user.click(zaaktypeRow("Zaaktype D"));
    fixture.detectChanges();

    expect(component["expandedRow"]).toBe(zaaktypeZonderEigenschap);

    const detailRow = screen.getByRole("row", {
      name: new RegExp(EIGENSCHAP_ONTBREEKT_MESSAGE),
    });
    expect(within(detailRow).getByText("eigenschap")).toBeInTheDocument();
    expect(
      within(detailRow).getByText(EIGENSCHAP_ONTBREEKT_MESSAGE),
    ).toBeInTheDocument();
  });

  it("should show every zaaktype that needs attention when the filter is set to not valide", () => {
    setValideFilter(ToggleSwitchOptions.UNCHECKED);

    expect(component["dataSource"].filteredData).toEqual([
      nietValideZaaktype,
      zaaktypeZonderRoltype,
      zaaktypeZonderEigenschap,
    ]);
    expect(
      screen.queryByRole("row", { name: /Zaaktype A/ }),
    ).not.toBeInTheDocument();
  });

  it("should show only the zaaktypen without findings when the filter is set to valide", () => {
    setValideFilter(ToggleSwitchOptions.CHECKED);

    expect(component["dataSource"].filteredData).toEqual([
      volledigIngerichtZaaktype,
    ]);
    expect(
      screen.queryByRole("row", { name: /Zaaktype C/ }),
    ).not.toBeInTheDocument();
  });

  it("should keep the valide filter a total partition so that no zaaktype disappears from both sides", () => {
    setValideFilter(ToggleSwitchOptions.UNCHECKED);
    const needingAttention = component["dataSource"].filteredData;
    setValideFilter(ToggleSwitchOptions.CHECKED);
    const withoutFindings = component["dataSource"].filteredData;

    expect(
      [...needingAttention, ...withoutFindings]
        .map(
          (zaaktypeInrichtingscheck) => zaaktypeInrichtingscheck.zaaktype.uuid,
        )
        .sort(),
    ).toEqual(
      component["dataSource"].data
        .map(
          (zaaktypeInrichtingscheck) => zaaktypeInrichtingscheck.zaaktype.uuid,
        )
        .sort(),
    );
  });

  it("should show 'beschikbaar' text when communicatiekanaal e-formulier exists", () => {
    expect(
      screen.getByText(
        "healthCheck.communicatiekanaal.e-formulier.beschikbaar",
      ),
    ).toBeInTheDocument();
  });

  it("should show 'niet beschikbaar' text when communicatiekanaal e-formulier does not exist", () => {
    component["bestaatCommunicatiekanaalEformulier"] = false;
    component["loadingCommunicatiekanaal"] = false;
    fixture.detectChanges();

    expect(
      screen.getByText(
        "healthCheck.communicatiekanaal.e-formulier.niet.beschikbaar",
      ),
    ).toBeInTheDocument();
  });

  it("should filter rows by zaaktype omschrijving text", () => {
    setValideFilter(ToggleSwitchOptions.INDETERMINATE);
    expect(component["dataSource"].filteredData.length).toBe(4);

    const event = { target: { value: "Zaaktype A" } } as unknown as Event;
    component["applyFilter"](event);
    expect(component["dataSource"].filteredData.length).toBe(1);
    expect(component["dataSource"].filteredData[0].zaaktype.omschrijving).toBe(
      "Zaaktype A",
    );
  });

  it("should show loading message while zaaktypes are loading", () => {
    component["dataSource"].data = [];
    component["loadingZaaktypes"] = true;
    fixture.detectChanges();

    expect(screen.getByText("msg.loading")).toBeInTheDocument();
  });

  it("should disable the sync button while zaaktypes are loading", () => {
    component["loadingZaaktypes"] = true;
    fixture.detectChanges();

    expect(
      screen.getByRole("button", {
        name: "healthCheck.synchroniseer.ztc.button",
      }),
    ).toBeDisabled();
  });

  it("should show 'geen gegevens' message when data source is empty and not loading", () => {
    component["dataSource"].data = [];
    component["loadingZaaktypes"] = false;
    fixture.detectChanges();

    expect(screen.getByText("msg.geen.gegevens.gevonden")).toBeInTheDocument();
  });

  it("should sort by doel ascending then descending via column header click", async () => {
    setValideFilter(ToggleSwitchOptions.INDETERMINATE);

    const sort = await loader.getHarness(MatSortHarness);
    const [doelHeader] = await sort.getSortHeaders({ label: "doel" });

    await doelHeader.click();
    expect(component["dataSource"].data[0].zaaktype.doel).toBe("Doel A");
    expect(component["dataSource"].data[3].zaaktype.doel).toBe("Doel D");

    await doelHeader.click();
    expect(component["dataSource"].data[0].zaaktype.doel).toBe("Doel D");
    expect(component["dataSource"].data[3].zaaktype.doel).toBe("Doel A");
  });

  it("should reload zaaktypes and update cache time on clearZTCCache", async () => {
    const listSpy =
      healthCheckService.listZaaktypeInrichtingschecks as jest.Mock;
    listSpy.mockClear();

    const event = new MouseEvent("click");
    jest.spyOn(event, "stopPropagation");

    component["clearZTCCache"](event);
    await sleep();

    expect(event.stopPropagation).toHaveBeenCalled();
    expect(clearZTCCachesMutation.mutationFn).toHaveBeenCalled();
    expect(component["ztcCacheTime"]).toBe("2024-03-19T10:00:00");
    expect(listSpy).toHaveBeenCalled();
  });
});
