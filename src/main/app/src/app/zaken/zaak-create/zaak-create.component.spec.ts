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
import {
  provideQueryClient,
  QueryClient,
} from "@tanstack/angular-query-experimental";
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
import { MatAutocompleteHarness } from "@angular/material/autocomplete/testing";
import { MatInputHarness } from "@angular/material/input/testing";
import { FoutAfhandelingService } from "src/app/fout-afhandeling/fout-afhandeling.service";
import { input } from "@angular/core";
import { BpmnConfigurationService } from "src/app/admin/bpmn-configuration.service";

describe(ZaakCreateComponent.name, () => {
  let identityService: IdentityService;
  let zakenService: ZakenService;
  let bpmnConfigurationService: BpmnConfigurationService;
  let fixture: ComponentFixture<ZaakCreateComponent>;
  let loader: HarnessLoader;
  let component: ZaakCreateComponent;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [ZaakCreateComponent, ZacInput],
      providers: [
        ZakenService,
        BpmnConfigurationService,
        NavigationService,
        KlantenService,
        ReferentieTabelService,
        UtilService,
        IdentityService,
        provideHttpClient(),
        provideQueryClient(new QueryClient()),
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
        NoopAnimationsModule,
      ],
    }).compileComponents();

    identityService = TestBed.inject(IdentityService);
    jest.spyOn(identityService, "listGroups").mockReturnValue(
      of([
        { id: "test-cmmn-group-id", naam: "test group CMMN" },
        { id: "test-bpmn-group-id", naam: "test group BPMN" },
      ]),
    );
    jest
      .spyOn(identityService, "listUsersInGroup")
      .mockReturnValue(of([{ id: "test-user-id", naam: "test user" }]));

    zakenService = TestBed.inject(ZakenService);
    jest.spyOn(zakenService, "listZaaktypesForCreation").mockReturnValue(
      of([
        fromPartial<GeneratedType<"RestZaaktype">>({
          uuid: "test-cmmn-zaaktype-1",
          omschrijving: "test-cmmn-description-1",
          zaakafhandelparameters: {
            defaultGroepId: "test-cmmn-group-id",
            defaultBehandelaarId: "test-user-id",
          },
        }),
        fromPartial<GeneratedType<"RestZaaktype">>({
          uuid: "test-cmmn-zaaktype-2",
          omschrijving: "test-cmmn-description-2",
        }),
        fromPartial<GeneratedType<"RestZaaktype">>({
          uuid: "test-bpmn-zaaktype-1",
          omschrijving: "test-bpmn-description-1",
          zaakafhandelparameters: { defaultGroepId: "test-bpmn-group-id" },
        }),
      ]),
    );

    bpmnConfigurationService = TestBed.inject(BpmnConfigurationService);
    jest
      .spyOn(bpmnConfigurationService, "listbpmnProcessConfigurations")
      .mockReturnValue(
        of([
          fromPartial<GeneratedType<"RestZaaktypeBpmnConfiguration">>({
            bpmnProcessDefinitionKey: "bpmn-process-1",
            zaaktypeUuid: "test-bpmn-zaaktype-1",
          }),
        ]),
      );

    fixture = TestBed.createComponent(ZaakCreateComponent);
    component = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);
  });

  describe(ZaakCreateComponent.prototype.caseTypeSelected.name, () => {
    beforeAll(() => {
      // disable animations for the tests
      (Element.prototype as any).animate = () => {
        const listeners: Record<string, Function[]> = {};

        const player = {
          play: () => {},
          pause: () => {},
          cancel: () => {},
          finish: () => {
            (listeners["finish"] || []).forEach((cb) => cb());
          },
          addEventListener: (name: string, cb: Function) => {
            (listeners[name] ||= []).push(cb);
          },
          removeEventListener: (name: string, cb: Function) => {
            listeners[name] = (listeners[name] || []).filter((fn) => fn !== cb);
          },
          finished: Promise.resolve(),
        };

        return player;
      };
    });

    beforeEach(() => {
      const router = TestBed.inject(Router);
      jest.spyOn(router, "navigate").mockImplementation(async () => true);
    });

    it("should handle CMMN case type selection", async () => {
      let inputs = await loader.getAllHarnesses(MatInputHarness);
      expect(inputs.length).toEqual(8);
      expect(zakenService.listZaaktypesForCreation).toHaveBeenCalled();

      // Select case type
      await inputs[0].focus();
      await inputs[0].setValue("cmmn");

      let autocomplete = await loader.getHarness(
        MatAutocompleteHarness.with({
          selector: '[ng-reflect-name="zaaktype"]',
        }),
      );
      let options = await autocomplete.getOptions();
      expect(options.length).toEqual(2);

      await options[0].click();
      let value = await inputs[0].getValue();
      expect(value).toBeTruthy();

      // select group
      expect(identityService.listGroups).toHaveBeenCalled();
      await inputs[4].focus();
      await inputs[4].setValue("cmmn");

      autocomplete = await loader.getHarness(
        MatAutocompleteHarness.with({
          selector: '[ng-reflect-name="groep"]',
        }),
      );
      options = await autocomplete.getOptions();
      expect(options.length).toEqual(1);

      await options[0].click();
      value = await inputs[0].getValue();
      expect(value).toBeTruthy();

      // select employee
      expect(identityService.listUsersInGroup).toHaveBeenCalled();
      await inputs[5].focus();
      await inputs[5].setValue("test");

      autocomplete = await loader.getHarness(
        MatAutocompleteHarness.with({
          selector: '[ng-reflect-name="behandelaar"]',
        }),
      );
      options = await autocomplete.getOptions();
      expect(options.length).toEqual(1);

      await options[0].click();
      value = await inputs[0].getValue();
      expect(value).toBeTruthy();
    });

    it("should handle BPMN case type selection", async () => {
      let inputs = await loader.getAllHarnesses(MatInputHarness);
      expect(inputs.length).toEqual(8);
      expect(zakenService.listZaaktypesForCreation).toHaveBeenCalled();

      // Select case type
      await inputs[0].focus();
      await inputs[0].setValue("bpmn");

      let autocomplete = await loader.getHarness(
        MatAutocompleteHarness.with({
          selector: '[ng-reflect-name="zaaktype"]',
        }),
      );
      let options = await autocomplete.getOptions();
      expect(options.length).toEqual(1);

      await options[0].click();
      let value = await inputs[0].getValue();
      expect(value).toBeTruthy();

      // select group
      expect(identityService.listGroups).toHaveBeenCalled();
      await inputs[4].focus();
      await inputs[4].setValue("bpmn");

      autocomplete = await loader.getHarness(
        MatAutocompleteHarness.with({
          selector: '[ng-reflect-name="groep"]',
        }),
      );
      options = await autocomplete.getOptions();
      expect(options.length).toEqual(1);

      await options[0].click();
      value = await inputs[0].getValue();
      expect(value).toBeTruthy();

      // select employee
      expect(identityService.listUsersInGroup).toHaveBeenCalledTimes(0);
      expect(await inputs[5].isDisabled()).toBe(true);
    });

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
    const mockInitiator: GeneratedType<"BetrokkeneIdentificatie"> = {
      bsnNummer: "123456789",
      type: "BSN",
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

      component["form"].controls.initiatorIdentificatie.setValue(mockInitiator);
      fixture.detectChanges();

      expect(component.hasInitiator()).toBe(true);

      const buttons = await loader.getAllHarnesses(
        MatButtonHarness.with({
          text: "clear",
        }),
      );

      await buttons[0].click();
      fixture.detectChanges();

      expect(component.hasInitiator()).toBe(false);
    });
  });
});
