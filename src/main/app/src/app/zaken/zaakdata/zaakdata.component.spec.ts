/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { provideHttpClient } from "@angular/common/http";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { ReactiveFormsModule } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import { MatButtonHarness } from "@angular/material/button/testing";
import { MatDividerModule } from "@angular/material/divider";
import { MatExpansionModule } from "@angular/material/expansion";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatIconModule } from "@angular/material/icon";
import { MatInputModule } from "@angular/material/input";
import { MatDrawer } from "@angular/material/sidenav";
import { MatToolbarModule } from "@angular/material/toolbar";
import { MatToolbarHarness } from "@angular/material/toolbar/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { notifyManager } from "@tanstack/query-core";
import { fromPartial } from "src/test-helpers";
import { testQueryClient } from "../../../../setupJest";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenService } from "../zaken.service";
import { ZaakdataComponent } from "./zaakdata.component";

const makeZaak = (
  fields: Partial<GeneratedType<"RestZaak">> = {},
): GeneratedType<"RestZaak"> =>
  fromPartial<GeneratedType<"RestZaak">>({
    uuid: "zaak-uuid-1",
    zaakdata: { field1: "value1" },
    ...fields,
  });

const makeSideNav = (): MatDrawer =>
  ({ close: jest.fn() }) as unknown as MatDrawer;

describe(ZaakdataComponent.name, () => {
  let fixture: ComponentFixture<ZaakdataComponent>;
  let loader: HarnessLoader;
  let zakenService: ZakenService;

  beforeEach(() => notifyManager.setScheduler((fn) => fn()));
  afterEach(() => notifyManager.setScheduler((fn) => setTimeout(fn, 0)));

  const setup = (
    zaak: GeneratedType<"RestZaak"> = makeZaak(),
    readonly = false,
    sideNav: MatDrawer = makeSideNav(),
  ) => {
    fixture = TestBed.createComponent(ZaakdataComponent);
    fixture.componentRef.setInput("zaak", zaak);
    fixture.componentRef.setInput("readonly", readonly);
    fixture.componentRef.setInput("sideNav", sideNav);
    fixture.detectChanges();
    loader = TestbedHarnessEnvironment.loader(fixture);
    return { fixture, component: fixture.componentInstance, sideNav };
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        ZaakdataComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
        ReactiveFormsModule,
        MatToolbarModule,
        MatButtonModule,
        MatIconModule,
        MatDividerModule,
        MatExpansionModule,
        MatFormFieldModule,
        MatInputModule,
      ],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        provideQueryClient(testQueryClient),
      ],
    }).compileComponents();

    zakenService = TestBed.inject(ZakenService);
    jest.spyOn(zakenService, "listProcesVariabelen").mockReturnValue({
      queryKey: ["procesvariabelen"],
      queryFn: async () => [],
    } as never);
    jest.spyOn(zakenService, "updateZaakdata").mockReturnValue({
      mutationKey: ["updateZaakdata"],
      mutationFn: jest.fn().mockResolvedValue(undefined),
    } as never);
  });

  it("shows wijzigen title when not readonly", async () => {
    setup(makeZaak(), false);
    const toolbar = await loader.getHarness(MatToolbarHarness);
    expect(await (await toolbar.host()).text()).toContain(
      "actie.zaakdata.wijzigen",
    );
  });

  it("shows bekijken title when readonly", async () => {
    setup(makeZaak(), true);
    const toolbar = await loader.getHarness(MatToolbarHarness);
    expect(await (await toolbar.host()).text()).toContain(
      "actie.zaakdata.bekijken",
    );
  });

  it("calls sideNav close when close button is clicked", async () => {
    const sideNav = makeSideNav();
    setup(makeZaak(), false, sideNav);
    const closeButton = await loader.getHarness(
      MatButtonHarness.with({ selector: "mat-toolbar button" }),
    );
    await closeButton.click();
    expect(sideNav.close).toHaveBeenCalled();
  });

  it("hides save and cancel buttons when readonly", async () => {
    setup(makeZaak(), true);
    const buttons = await loader.getAllHarnesses(
      MatButtonHarness.with({ selector: "mat-action-row button" }),
    );
    expect(buttons).toHaveLength(0);
  });

  it("shows save and cancel buttons when not readonly", async () => {
    setup(makeZaak(), false);
    const buttons = await loader.getAllHarnesses(
      MatButtonHarness.with({ selector: "mat-action-row button" }),
    );
    expect(buttons).toHaveLength(2);
  });

  it("calls sideNav close when cancel button is clicked", async () => {
    const sideNav = makeSideNav();
    setup(makeZaak(), false, sideNav);
    const buttons = await loader.getAllHarnesses(
      MatButtonHarness.with({ selector: "mat-action-row button" }),
    );
    await buttons[1].click();
    expect(sideNav.close).toHaveBeenCalled();
  });
});
