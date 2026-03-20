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
import { MatTableModule } from "@angular/material/table";
import { MatSortHarness } from "@angular/material/sort/testing";
import { MatTableHarness } from "@angular/material/table/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { of } from "rxjs";
import { delay } from "rxjs/operators";
import { UtilService } from "../../core/service/util.service";
import { DatumPipe } from "../../shared/pipes/datum.pipe";
import { ReadMoreComponent } from "../../shared/read-more/read-more.component";
import { SideNavComponent } from "../../shared/side-nav/side-nav.component";
import { ToggleFilterComponent } from "../../shared/table-zoek-filters/toggle-filter/toggle-filter.component";
import { ToggleSwitchOptions } from "../../shared/table-zoek-filters/toggle-filter/toggle-switch-options";
import { VersionComponent } from "../../shared/version/version.component";
import { HealthCheckService } from "../health-check.service";
import { InrichtingscheckComponent } from "./inrichtingscheck.component";

const mockZaaktype1 = {
  zaaktype: {
    uuid: "uuid-1",
    omschrijving: "Zaaktype A",
    doel: "Doel A",
    beginGeldigheid: "2024-01-01",
  },
  valide: true,
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
};

const mockZaaktype2 = {
  ...mockZaaktype1,
  zaaktype: {
    ...mockZaaktype1.zaaktype,
    uuid: "uuid-2",
    omschrijving: "Zaaktype B",
    doel: "Doel B",
    beginGeldigheid: "2024-06-01",
  },
  valide: false,
  zaakafhandelParametersValide: false,
};

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
  let utilServiceMock: Pick<UtilService, "setTitle" | "openSnackbar">;

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
        provideRouter([]),
        { provide: UtilService, useValue: utilServiceMock },
      ],
    }).compileComponents();

    healthCheckService = TestBed.inject(HealthCheckService);

    jest
      .spyOn(healthCheckService, "listZaaktypeInrichtingschecks")
      .mockReturnValue(
        of([mockZaaktype1, mockZaaktype2]).pipe(delay(0)) as ReturnType<
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
    expect(component["dataSource"].data.length).toBe(2);
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

  it("should expand an invalid row on click and collapse it on second click", async () => {
    const table = await loader.getHarness(MatTableHarness);
    // default valideFilter=UNCHECKED hides valid rows — only the invalid row is visible
    const mainRows = await table.getRows({ selector: ".main-row" });
    const invalidRow = mainRows[0];

    expect(component["expandedRow"]).toBeNull();

    await (await invalidRow.host()).click();
    expect(component["expandedRow"]).toBe(mockZaaktype2);

    await (await invalidRow.host()).click();
    expect(component["expandedRow"]).toBeNull();
  });

  it("should not expand a valid row on click", async () => {
    // switch filter to CHECKED so valid rows are visible
    component["valideFilter"] = ToggleSwitchOptions.CHECKED;
    component["applyFilter"]();
    fixture.detectChanges();

    const table = await loader.getHarness(MatTableHarness);
    const mainRows = await table.getRows({ selector: ".main-row" });
    const validRow = mainRows[0];

    await (await validRow.host()).click();

    expect(component["expandedRow"]).toBeNull();
  });

  it("should render zaaktype omschrijving in the visible table row", async () => {
    // default UNCHECKED filter shows only invalid rows — mockZaaktype2 is the only visible row
    const table = await loader.getHarness(MatTableHarness);
    const rows = await table.getRows({ selector: ".main-row" });
    const cells = await rows[0].getCells({
      columnName: "zaaktypeOmschrijving",
    });
    expect(await cells[0].getText()).toBe("Zaaktype B");
  });

  it("should show 'beschikbaar' text when communicatiekanaal e-formulier exists", () => {
    expect(fixture.nativeElement.textContent).toContain(
      "healthCheck.communicatiekanaal.e-formulier.beschikbaar",
    );
  });

  it("should show 'niet beschikbaar' text when communicatiekanaal e-formulier does not exist", () => {
    component["bestaatCommunicatiekanaalEformulier"] = false;
    component["loadingCommunicatiekanaal"] = false;
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain(
      "healthCheck.communicatiekanaal.e-formulier.niet.beschikbaar",
    );
  });

  it("should filter rows by zaaktype omschrijving text", () => {
    component["valideFilter"] = ToggleSwitchOptions.INDETERMINATE;
    component["applyFilter"]();
    expect(component["dataSource"].filteredData.length).toBe(2);

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
    expect(fixture.nativeElement.textContent).toContain("msg.loading");
  });

  it("should disable the sync button while zaaktypes are loading", () => {
    component["loadingZaaktypes"] = true;
    fixture.detectChanges();
    const button = fixture.nativeElement.querySelector(
      "[mat-raised-button]",
    ) as HTMLButtonElement;
    expect(button.disabled).toBe(true);
  });

  it("should show 'geen gegevens' message when data source is empty and not loading", () => {
    component["dataSource"].data = [];
    component["loadingZaaktypes"] = false;
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain(
      "msg.geen.gegevens.gevonden",
    );
  });

  it("should sort by doel ascending then descending via column header click", async () => {
    component["valideFilter"] = ToggleSwitchOptions.INDETERMINATE;
    component["applyFilter"]();
    fixture.detectChanges();

    const sort = await loader.getHarness(MatSortHarness);
    const [doelHeader] = await sort.getSortHeaders({ label: "doel" });

    await doelHeader.click();
    expect(component["dataSource"].data[0].zaaktype.doel).toBe("Doel A");
    expect(component["dataSource"].data[1].zaaktype.doel).toBe("Doel B");

    await doelHeader.click();
    expect(component["dataSource"].data[0].zaaktype.doel).toBe("Doel B");
    expect(component["dataSource"].data[1].zaaktype.doel).toBe("Doel A");
  });

  it("should reload zaaktypes and update cache time on clearZTCCache", fakeAsync(() => {
    const newCacheTime = "2024-03-19T10:00:00";
    jest
      .spyOn(healthCheckService, "clearZTCCaches")
      .mockReturnValue(
        of(newCacheTime).pipe(delay(0)) as ReturnType<
          typeof healthCheckService.clearZTCCaches
        >,
      );

    const listSpy =
      healthCheckService.listZaaktypeInrichtingschecks as jest.Mock;
    listSpy.mockClear();

    const event = new MouseEvent("click");
    jest.spyOn(event, "stopPropagation");

    component["clearZTCCache"](event);
    tick(0);

    expect(event.stopPropagation).toHaveBeenCalled();
    expect(component["ztcCacheTime"]).toBe(newCacheTime);
    expect(listSpy).toHaveBeenCalled();
  }));
});
