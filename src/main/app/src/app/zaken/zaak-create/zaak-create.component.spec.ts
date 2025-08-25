/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { provideHttpClient } from "@angular/common/http";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { MatButtonHarness } from "@angular/material/button/testing";
import { MatHint, MatLabel } from "@angular/material/form-field";
import { MatIcon } from "@angular/material/icon";
import { MatIconHarness } from "@angular/material/icon/testing";
import { MatSidenavModule } from "@angular/material/sidenav";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { Router, RouterModule } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { fromPartial } from "@total-typescript/shoehorn";
import { of } from "rxjs";
import { ZacInput } from "src/app/shared/form/input/input";
import { ReferentieTabelService } from "../../admin/referentie-tabel.service";
import { UtilService } from "../../core/service/util.service";
import { IdentityService } from "../../identity/identity.service";
import { KlantenService } from "../../klanten/klanten.service";
import { MaterialFormBuilderModule } from "../../shared/material-form-builder/material-form-builder.module";
import { NavigationService } from "../../shared/navigation/navigation.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenService } from "../zaken.service";
import { ZaakCreateComponent } from "./zaak-create.component";

describe(ZaakCreateComponent.name, () => {
  let identityService: IdentityService;
  let zakenService: ZakenService;
  let fixture: ComponentFixture<ZaakCreateComponent>;
  let loader: HarnessLoader;
  let component: ZaakCreateComponent;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [ZaakCreateComponent, ZacInput],
      providers: [
        ZakenService,
        NavigationService,
        KlantenService,
        ReferentieTabelService,
        UtilService,
        IdentityService,
        provideHttpClient(),
      ],
      imports: [
        RouterModule.forRoot([]),
        TranslateModule.forRoot(),
        NoopAnimationsModule,
        MatSidenavModule,
        MaterialFormBuilderModule,
        MatHint,
        MatIcon,
        MatLabel,
        FormsModule,
        ReactiveFormsModule,
      ],
    }).compileComponents();

    identityService = TestBed.inject(IdentityService);
    jest
      .spyOn(identityService, "listGroups")
      .mockReturnValue(of([{ id: "test-group-id", naam: "test group" }]));
    jest
      .spyOn(identityService, "listUsersInGroup")
      .mockReturnValue(of([{ id: "test-user-id", naam: "test user" }]));

    zakenService = TestBed.inject(ZakenService);
    jest.spyOn(zakenService, "listZaaktypes").mockReturnValue(
      of([
        fromPartial<GeneratedType<"RestZaaktype">>({
          uuid: "test-zaaktype-1",
          omschrijving: "test-description-1",
        }),
        fromPartial<GeneratedType<"RestZaaktype">>({
          uuid: "test-zaaktype-2",
          omschrijving: "test-description-2",
        }),
      ]),
    );
    fixture = TestBed.createComponent(ZaakCreateComponent);
    component = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);
  });

  describe(ZaakCreateComponent.prototype.caseTypeSelected.name, () => {
    // TODO validate that we can use the `MatAutocompleteHarness` to write this test
    it.todo(`should set the default group`);

    it.todo(`should set the default case worker`);

    it.todo(`should set the confidentiality notice`);
  });

  describe("Bag objects field editing", () => {
    const mockBagObjects: GeneratedType<"RESTBAGObject">[] = [
      {
        url: "https://example.com/bagobject/123",
        identificatie: "123456789",
        geconstateerd: true,
        bagObjectType: "PAND",
        omschrijving: "Test Pand aan de Dorpsstraat 1",
      },
      {
        url: "https://example.com/bagobject/456",
        identificatie: "987654321",
        geconstateerd: false,
        bagObjectType: "ADRES",
        omschrijving: "Test Adres in de Dorpsstraat 2",
      },
    ];

    beforeEach(() => {
      const router = TestBed.inject(Router);
      jest.spyOn(router, "navigate").mockImplementation(async () => true);
    });

    it("should show the BagObjects icon when there are no bag objects selected", async () => {
      component.clearBagObjecten();

      fixture.detectChanges();
      const icon = await loader.getAllHarnesses(
        MatIconHarness.with({ name: "gps_fixed" }),
      );
      expect(icon.length).toBeTruthy();
    });

    it("should empty the bagobjects field when the close icon has been clicked", async () => {
      component.clearBagObjecten();

      component["form"].get("bagObjecten")?.setValue(mockBagObjects);
      fixture.detectChanges();

      expect(component.hasBagObject()).toBe(true);

      const closeIcon = await loader.getHarness(
        MatIconHarness.with({ name: "close" }),
      );
      expect(closeIcon).toBeTruthy();

      const button = await loader.getHarness(
        MatButtonHarness.with({
          selector:
            'zac-input[key="bagObjecten"] button[mat-icon-button][matSuffix]',
        }),
      );

      await button.click();
      fixture.detectChanges();

      expect(component.hasBagObject()).toBe(false);
    });
  });

  describe("Initiator field editing", () => {
    const mockInitiator: GeneratedType<"RestPersoon"> = {
      bsn: "123456789",
      geslacht: "V",
      geboortedatum: "1990-01-01",
      verblijfplaats: "Amsterdam",
      naam: "Test User",
      emailadres: "test.user@example.com",
      telefoonnummer: "0612345678",
      indicaties: [],
      identificatieType: "BSN",
      identificatie: "123456789",
    };

    beforeEach(() => {
      const router = TestBed.inject(Router);
      jest.spyOn(router, "navigate").mockImplementation(async () => true);

      component["form"].get("zaaktype")?.setValue(
        fromPartial({
          zaakafhandelparameters: {
            betrokkeneKoppelingen: { brpKoppelen: true },
          },
        }),
      );

      fixture.detectChanges();
    });

    it("should show the person icon when there is no initiator selected", async () => {
      component.clearInitiator();
      fixture.detectChanges();

      const icon = await loader.getAllHarnesses(
        MatIconHarness.with({ name: "person" }),
      );

      expect(icon.length).toBeTruthy();
    });

    it("should clear the initiator field when the close icon is clicked", async () => {
      component.clearInitiator();

      component["form"].get("initiator")?.setValue(mockInitiator);
      fixture.detectChanges();

      expect(component.hasInitiator()).toBe(true);

      const closeIcon = await loader.getHarness(
        MatIconHarness.with({ name: "close" }),
      );
      expect(closeIcon).toBeTruthy();

      const button = await loader.getHarness(
        MatButtonHarness.with({
          selector:
            'zac-input[key="initiator"] button[mat-icon-button][matSuffix]',
        }),
      );

      await button.click();
      fixture.detectChanges();

      expect(component.hasInitiator()).toBe(false);
    });
  });
});
