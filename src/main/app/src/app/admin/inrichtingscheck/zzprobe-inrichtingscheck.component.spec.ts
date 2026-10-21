/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { provideHttpClient } from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { Component } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
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
import { sleep, testQueryClient } from "../../../../setupJest";
import { fromPartial } from "../../../test-helpers";
import { UtilService } from "../../core/service/util.service";
import { DatumPipe } from "../../shared/pipes/datum.pipe";
import { ReadMoreComponent } from "../../shared/read-more/read-more.component";
import { SideNavComponent } from "../../shared/side-nav/side-nav.component";
import { ToggleFilterComponent } from "../../shared/table-zoek-filters/toggle-filter/toggle-filter.component";
import { ToggleSwitchOptions } from "../../shared/table-zoek-filters/toggle-filter/toggle-switch-options";
import { GeneratedType } from "../../shared/utils/generated-types";
import { VersionComponent } from "../../shared/version/version.component";
import { InrichtingscheckComponent } from "./inrichtingscheck.component";

const ZAAKTYPES_URL = "/rest/health-check/zaaktypes";
const COMMUNICATIEKANAAL_URL =
  "/rest/health-check/bestaat-communicatiekanaal-eformulier";
const BUILD_INFORMATIE_URL = "/rest/health-check/build-informatie";

const VALIDE_ICON_LABEL = "health-check.zaaktype.status.valide";
const WAARSCHUWING_ICON_LABEL = "health-check.zaaktype.status.waarschuwing";
const NIET_VALIDE_ICON_LABEL = "health-check.zaaktype.status.niet-valide";
const ROLTYPE_ONTBREEKT_MESSAGE =
  "health-check.zaaktype.zaakspecifieke-autorisatie.roltype-ontbreekt";
const EIGENSCHAP_ONTBREEKT_MESSAGE =
  "health-check.zaaktype.zaakspecifieke-autorisatie.eigenschap-ontbreekt";

const volledigIngerichtZaaktype = fromPartial<
  GeneratedType<"RestZaaktypeInrichtingscheck">
>({
  zaaktype: {
    uuid: "fakeZaaktypeUuid1",
    omschrijving: "Zaaktype A",
    doel: "Doel A",
    beginGeldigheid: "2024-01-01",
  },
  isValide: true,
  hasWaarschuwingen: false,
  isZaakafhandelParametersValide: true,
  isStatustypeIntakeAanwezig: true,
  isStatustypeInBehandelingAanwezig: true,
  isStatustypeHeropendAanwezig: true,
  isStatustypeAanvullendeInformatieVereist: true,
  isStatustypeAfgerondAanwezig: true,
  isStatustypeAfgerondLaatsteVolgnummer: true,
  isResultaattypeAanwezig: true,
  aantalInitiatorroltypen: 1,
  aantalBehandelaarroltypen: 1,
  isRolOverigeAanwezig: true,
  isInformatieobjecttypeEmailAanwezig: true,
  resultaattypesMetVerplichtBesluit: [],
  isBesluittypeAanwezig: true,
  isBrpInstellingenCorrect: true,
  isZaakspecifiekeAutorisatieEigenschapAanwezig: true,
  isZaakspecifiekeAutorisatieRoltypeAanwezig: true,
});

const nietValideZaaktype = fromPartial<
  GeneratedType<"RestZaaktypeInrichtingscheck">
>({
  ...volledigIngerichtZaaktype,
  zaaktype: {
    uuid: "fakeZaaktypeUuid2",
    omschrijving: "Zaaktype B",
    doel: "Doel B",
    beginGeldigheid: "2024-06-01",
  },
  isValide: false,
  isZaakafhandelParametersValide: false,
});

const zaaktypeZonderRoltype = fromPartial<
  GeneratedType<"RestZaaktypeInrichtingscheck">
>({
  ...volledigIngerichtZaaktype,
  zaaktype: {
    uuid: "fakeZaaktypeUuid3",
    omschrijving: "Zaaktype C",
    doel: "Doel C",
    beginGeldigheid: "2024-07-01",
  },
  hasWaarschuwingen: true,
  isZaakspecifiekeAutorisatieEigenschapAanwezig: true,
  isZaakspecifiekeAutorisatieRoltypeAanwezig: false,
});

const zaaktypeZonderEigenschap = fromPartial<
  GeneratedType<"RestZaaktypeInrichtingscheck">
>({
  ...volledigIngerichtZaaktype,
  zaaktype: {
    uuid: "fakeZaaktypeUuid4",
    omschrijving: "Zaaktype D",
    doel: "Doel D",
    beginGeldigheid: "2024-08-01",
  },
  hasWaarschuwingen: true,
  isZaakspecifiekeAutorisatieEigenschapAanwezig: false,
  isZaakspecifiekeAutorisatieRoltypeAanwezig: true,
});

@Component({
  templateUrl: "./inrichtingscheck.component.html",
  standalone: true,
  imports: [
    MatSidenavModule,
    MatCardModule,
    MatExpansionModule,
    MatIconModule,
    MatTableModule,
    MatSortModule,
    MatFormFieldModule,
    MatInputModule,
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
  let httpTestingController: HttpTestingController;
  let utilServiceMock: Pick<UtilService, "openSnackbar">;

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

    httpTestingController = TestBed.inject(HttpTestingController);
  });

  async function loadHealthChecks({
    zaaktypes = [
      volledigIngerichtZaaktype,
      nietValideZaaktype,
      zaaktypeZonderRoltype,
      zaaktypeZonderEigenschap,
    ],
    bestaatCommunicatiekanaalEformulier = true,
  }: {
    zaaktypes?: GeneratedType<"RestZaaktypeInrichtingscheck">[] | null;
    bestaatCommunicatiekanaalEformulier?: boolean;
  } = {}) {
    fixture = TestBed.createComponent(TestInrichtingscheckComponent);
    component = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
    await sleep();
    httpTestingController
      .expectOne(COMMUNICATIEKANAAL_URL)
      .flush(bestaatCommunicatiekanaalEformulier);
    httpTestingController.expectOne(BUILD_INFORMATIE_URL).flush({});
    if (zaaktypes) {
      httpTestingController.expectOne(ZAAKTYPES_URL).flush(zaaktypes);
    }
    await sleep();
    await sleep();
    fixture.detectChanges();
  }

  it("PROBE: websocket-reconnect invalidateQueries() while a filter is typed", async () => {
    await loadHealthChecks();
    const input = (fixture.nativeElement.querySelector("input[matinput], input.mat-mdc-input-element") as HTMLInputElement);
    await user.type(input, "Zaaktype C");
    fixture.detectChanges();
    console.log("BEFORE rows:", screen.queryAllByRole("row").map((r) => r.textContent?.match(/Zaaktype [A-D]/)?.[0]).filter(Boolean));
    console.log("BEFORE input:", JSON.stringify(((fixture.nativeElement.querySelector("input[matinput], input.mat-mdc-input-element") as HTMLInputElement) as HTMLInputElement).value));

    void testQueryClient.invalidateQueries();
    await sleep();
    fixture.detectChanges();
    console.log("DURING loading text:", screen.queryByText("msg.loading") !== null, "table:", screen.queryByRole("table") !== null);
    const pending = httpTestingController.match(() => true).map((r) => r.request.url);
    console.log("REFETCHED:", pending);
    httpTestingController.match(ZAAKTYPES_URL).forEach((r) => r.flush([volledigIngerichtZaaktype, nietValideZaaktype, zaaktypeZonderRoltype, zaaktypeZonderEigenschap]));
    httpTestingController.match(() => true).forEach((r) => r.flush(r.request.url === COMMUNICATIEKANAAL_URL ? true : {}));
    await sleep(); await sleep();
    fixture.detectChanges();
    console.log("AFTER rows:", screen.queryAllByRole("row").map((r) => r.textContent?.match(/Zaaktype [A-D]/)?.[0]).filter(Boolean));
    console.log("AFTER input:", JSON.stringify(((fixture.nativeElement.querySelector("input[matinput], input.mat-mdc-input-element") as HTMLInputElement) as HTMLInputElement).value));
  });
});
