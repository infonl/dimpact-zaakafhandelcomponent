/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { provideHttpClient } from "@angular/common/http";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatButtonHarness } from "@angular/material/button/testing";
import { MatDrawer } from "@angular/material/sidenav";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { of } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { testQueryClient } from "../../../../setupJest";
import { InformatieObjectenService } from "../../informatie-objecten/informatie-objecten.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenService } from "../zaken.service";
import { BesluitCreateComponent } from "./besluit-create.component";

const fakeZaak = fromPartial<GeneratedType<"RestZaak">>({
  uuid: "zaak-uuid-1",
  zaaktype: { uuid: "zaaktype-uuid-1" },
});

const fakeBesluittype = fromPartial<GeneratedType<"RestBesluitType">>({
  id: "besluittype-id-1",
  naam: "Besluittype 1",
  publication: { enabled: false },
});

const fakeBesluittypeWithPublication = fromPartial<
  GeneratedType<"RestBesluitType">
>({
  id: "besluittype-id-2",
  naam: "Besluittype 2",
  publication: {
    enabled: true,
    responseTermDays: 6,
    publicationTermDays: 1,
  },
});

describe(BesluitCreateComponent.name, () => {
  let fixture: ComponentFixture<BesluitCreateComponent>;
  let component: BesluitCreateComponent;
  let loader: HarnessLoader;
  let zakenService: ZakenService;
  let informatieObjectenService: InformatieObjectenService;
  let sideNavSpy: jest.SpyInstance;
  // The create mutation stays pending so onSuccess/onError never fire; we only
  // assert that submit() forwards the built payload to the mutation.
  let createBesluitMutationFn: jest.Mock;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        BesluitCreateComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [
        provideHttpClient(),
        provideQueryClient(testQueryClient),
        provideRouter([]),
      ],
    }).compileComponents();

    zakenService = TestBed.inject(ZakenService);
    informatieObjectenService = TestBed.inject(InformatieObjectenService);

    jest
      .spyOn(zakenService, "listResultaattypes")
      .mockReturnValue(of([] as never));
    jest
      .spyOn(zakenService, "listBesluittypes")
      .mockReturnValue(of([fakeBesluittype] as never));

    createBesluitMutationFn = jest.fn(() => new Promise<void>(() => {}));
    jest.spyOn(zakenService, "createBesluit").mockReturnValue(
      fromPartial({
        mutationKey: ["/rest/zaken/besluit"],
        mutationFn: createBesluitMutationFn,
      }),
    );

    fixture = TestBed.createComponent(BesluitCreateComponent);
    component = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);

    const mockDrawer = fromPartial<MatDrawer>({ close: jest.fn() });
    sideNavSpy = jest.spyOn(mockDrawer, "close");
    component.zaak = fakeZaak;
    component.sideNav = mockDrawer;

    fixture.detectChanges();
  });

  afterEach(() => {
    testQueryClient.clear();
    jest.clearAllMocks();
  });

  describe("initialisation", () => {
    it("loads resultaattypes and besluittypes for the zaak's zaaktype", () => {
      expect(zakenService.listResultaattypes).toHaveBeenCalledWith(
        "zaaktype-uuid-1",
      );
      expect(zakenService.listBesluittypes).toHaveBeenCalledWith(
        "zaaktype-uuid-1",
      );
    });
  });

  describe("close button", () => {
    it("calls sideNav.close() when close icon-button is clicked", async () => {
      const buttons = await loader.getAllHarnesses(MatButtonHarness);
      const closeButton = buttons[0];
      await closeButton.click();
      expect(sideNavSpy).toHaveBeenCalled();
    });
  });

  describe("submit button", () => {
    it("is disabled when no besluit is selected", async () => {
      const submitButton = await loader.getHarness(
        MatButtonHarness.with({ text: /actie.aanmaken/ }),
      );
      expect(await submitButton.isDisabled()).toBe(true);
    });

    it("is enabled when required fields are set", async () => {
      component["form"].controls.besluit.setValue(fakeBesluittype);
      component["form"].markAsDirty();
      fixture.detectChanges();
      const submitButton = await loader.getHarness(
        MatButtonHarness.with({ text: /actie.aanmaken/ }),
      );
      expect(await submitButton.isDisabled()).toBe(false);
    });
  });

  const countDateFields = () =>
    fixture.nativeElement.querySelectorAll("zac-date").length;

  describe("publication section", () => {
    it("is hidden when selected besluittype has publication disabled", () => {
      component["form"].controls.besluit.setValue(fakeBesluittype);
      fixture.detectChanges();
      // Only ingangsdatum and vervaldatum are rendered.
      expect(countDateFields()).toBe(2);
    });

    it("is shown when selected besluittype has publication enabled", () => {
      jest
        .spyOn(informatieObjectenService, "listEnkelvoudigInformatieobjecten")
        .mockReturnValue(of([] as never));
      component["form"].controls.besluit.setValue(
        fakeBesluittypeWithPublication,
      );
      fixture.detectChanges();
      // ingangsdatum, vervaldatum + publicatiedatum, uiterlijkereactiedatum.
      expect(countDateFields()).toBe(4);
    });
  });

  describe("cancel button", () => {
    it("calls sideNav.close() when cancel button is clicked", async () => {
      const cancelButton = await loader.getHarness(
        MatButtonHarness.with({ text: /actie.annuleren/ }),
      );
      await cancelButton.click();
      expect(sideNavSpy).toHaveBeenCalled();
    });
  });

  describe("submit()", () => {
    it("triggers the create-besluit mutation with the form payload", async () => {
      component["form"].controls.besluit.setValue(fakeBesluittype);

      component.submit();
      await Promise.resolve();

      expect(createBesluitMutationFn.mock.calls[0][0]).toEqual(
        expect.objectContaining({
          zaakUuid: "zaak-uuid-1",
          besluittypeUuid: "besluittype-id-1",
        }),
      );
    });
  });
});
