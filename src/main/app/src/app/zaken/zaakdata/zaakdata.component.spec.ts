/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Clipboard } from "@angular/cdk/clipboard";
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
import { MatIconHarness } from "@angular/material/icon/testing";
import { MatInputModule } from "@angular/material/input";
import { MatDrawer } from "@angular/material/sidenav";
import { MatToolbarModule } from "@angular/material/toolbar";
import { MatToolbarHarness } from "@angular/material/toolbar/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { notifyManager } from "@tanstack/query-core";
import { within } from "@testing-library/angular";
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
    sideNav: MatDrawer = makeSideNav(),
  ) => {
    fixture = TestBed.createComponent(ZaakdataComponent);
    fixture.componentRef.setInput("zaak", zaak);
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
  });

  it("shows bekijken title", async () => {
    setup();
    const toolbar = await loader.getHarness(MatToolbarHarness);
    expect(await (await toolbar.host()).text()).toContain(
      "actie.zaakdata.bekijken",
    );
  });

  it("shows archief title when the zaakdata is archived", async () => {
    setup(makeZaak({ isZaakdataGearchiveerd: true }));
    const toolbar = await loader.getHarness(MatToolbarHarness);
    expect(await (await toolbar.host()).text()).toContain(
      "actie.zaakdata.archief",
    );
  });

  it("shows the archief toelichting when the zaakdata is archived", () => {
    setup(makeZaak({ isZaakdataGearchiveerd: true }));

    expect(
      within(fixture.nativeElement as HTMLElement).getByText(
        "msg.zaakdata.archief.toelichting",
      ),
    ).toBeInTheDocument();
  });

  it("does not show the archief toelichting when the zaakdata is not archived", () => {
    setup();

    expect(
      within(fixture.nativeElement as HTMLElement).queryByText(
        "msg.zaakdata.archief.toelichting",
      ),
    ).not.toBeInTheDocument();
  });

  it("calls sideNav close when close button is clicked", async () => {
    const sideNav = makeSideNav();
    setup(makeZaak(), sideNav);
    const closeButton = await loader.getHarness(
      MatButtonHarness.with({ selector: "mat-toolbar button" }),
    );
    await closeButton.click();
    expect(sideNav.close).toHaveBeenCalled();
  });

  it("has no save/cancel buttons", async () => {
    setup();
    const buttons = await loader.getAllHarnesses(
      MatButtonHarness.with({ selector: "mat-action-row button" }),
    );
    expect(buttons).toHaveLength(0);
  });

  const setupWithForm = (zaak: GeneratedType<"RestZaak">) => {
    testQueryClient.setQueryData(["procesvariabelen"], []);
    const result = setup(zaak);
    fixture.detectChanges();
    return result;
  };

  describe("clipboard copy icons", () => {
    it("copies the field name to clipboard when icon is clicked", async () => {
      setupWithForm(makeZaak({ zaakdata: { myField: "someValue" } }));
      const clipboard = TestBed.inject(Clipboard);
      jest.spyOn(clipboard, "copy");
      const icon = await loader.getHarness(
        MatIconHarness.with({ name: "content_copy" }),
      );
      await (await icon.host()).click();
      expect(clipboard.copy).toHaveBeenCalledWith("myField");
    });

    it("applies copy-icon--sm class only when control value is null/undefined/empty string", async () => {
      setupWithForm(
        makeZaak({
          zaakdata: { emptyField: "", filledField: "hello", zeroField: 0 },
        }),
      );
      const icons = await loader.getAllHarnesses(
        MatIconHarness.with({ name: "content_copy" }),
      );
      expect(await (await icons[0].host()).hasClass("copy-icon--sm")).toBe(
        true,
      );
      expect(await (await icons[1].host()).hasClass("copy-icon--sm")).toBe(
        false,
      );
      expect(await (await icons[2].host()).hasClass("copy-icon--sm")).toBe(
        false,
      );
    });
  });
});
