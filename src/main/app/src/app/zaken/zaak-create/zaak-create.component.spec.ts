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
import { MatAutocompleteHarness } from "@angular/material/autocomplete/testing";
import { MatButtonHarness } from "@angular/material/button/testing";
import { MatHint, MatLabel } from "@angular/material/form-field";
import { MatIcon } from "@angular/material/icon";
import { MatIconHarness } from "@angular/material/icon/testing";
import { MatInputHarness } from "@angular/material/input/testing";
import { MatSelectHarness } from "@angular/material/select/testing";
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
import { BpmnService } from "src/app/admin/bpmn.service";

interface AnimationMock {
  play: () => void;
  pause: () => void;
  cancel: () => void;
  finish: () => void;
  addEventListener: (name: string, cb: () => void) => void;
  removeEventListener: (name: string, cb: () => void) => void;
  finished: Promise<void>;
}

describe(ZaakCreateComponent.name, () => {
  let identityService: IdentityService;
  let zakenService: ZakenService;
  let bpmnService: BpmnService;
  let utilService: UtilService;
  let referentieTabelService: ReferentieTabelService;
  let fixture: ComponentFixture<ZaakCreateComponent>;
  let loader: HarnessLoader;
  let component: ZaakCreateComponent;
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [ZaakCreateComponent, ZacInput],
      providers: [
        ZakenService,
        BpmnService,
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
          vertrouwelijkheidaanduiding: "OPENBAAR",
          zaakafhandelparameters: {
            defaultGroepId: "test-cmmn-group-id",
            defaultBehandelaarId: "test-user-id",
            zaaktype: { uuid: "uuid-test-cmmn-zaaktype-1" },
          },
        }),
        fromPartial<GeneratedType<"RestZaaktype">>({
          uuid: "test-cmmn-zaaktype-2",
          omschrijving: "test-cmmn-description-2",
          vertrouwelijkheidaanduiding: "OPENBAAR",
          zaakafhandelparameters: {
            zaaktype: { uuid: "uuid-test-cmmn-zaaktype-2" },
          },
        }),
        fromPartial<GeneratedType<"RestZaaktype">>({
          uuid: "test-bpmn-zaaktype-1",
          omschrijving: "test-bpmn-description-1",
          vertrouwelijkheidaanduiding: "OPENBAAR",
          zaakafhandelparameters: {
            zaaktype: { uuid: "uuid-test-bpmn-zaaktype-1" },
          },
        }),
      ]),
    );

    bpmnService = TestBed.inject(BpmnService);
    jest.spyOn(bpmnService, "listbpmnProcessConfigurations").mockReturnValue(
      of([
        fromPartial<GeneratedType<"RestZaaktypeBpmnConfiguration">>({
          bpmnProcessDefinitionKey: "bpmn-process-1",
          groepNaam: "test-bpmn-group-id",
          zaaktypeUuid: "uuid-test-bpmn-zaaktype-1",
        }),
      ]),
    );

    utilService = TestBed.inject(UtilService);
    jest.spyOn(utilService, "getEnumAsSelectList").mockImplementation(
      jest.fn(() => [
        { label: "Openbaar", value: "OPENBAAR" },
        { label: "INTERN", value: "INTERN" },
      ]),
    );
    jest.spyOn(utilService, "setTitle").mockImplementation(jest.fn());

    referentieTabelService = TestBed.inject(ReferentieTabelService);
    jest
      .spyOn(referentieTabelService, "listCommunicatiekanalen")
      .mockReturnValue(
        of([
          "Boeman",
          "Koebel",
          "Telegram",
          "Tamtam",
          "PostDuif",
          "Rooksignalen",
          "Telefoon",
        ]),
      );

    router = TestBed.inject(Router);
    jest.spyOn(router, "navigate").mockImplementation(async () => true);

    fixture = TestBed.createComponent(ZaakCreateComponent);
    component = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);
  });

  describe(ZaakCreateComponent.prototype.caseTypeSelected.name, () => {
    beforeAll(() => {
      // disable animations for the tests
      (
        Element.prototype as unknown as {
          animate: (
            keyframes: Keyframe[] | PropertyIndexedKeyframes,
            options?: number | KeyframeAnimationOptions,
          ) => AnimationMock;
        }
      ).animate = () => {
        const listeners: Record<string, Array<() => void>> = {};

        const player: AnimationMock = {
          play: () => {},
          pause: () => {},
          cancel: () => {},
          finish: () => {
            (listeners["finish"] || []).forEach((cb) => cb());
          },
          addEventListener: (name: string, cb: () => void) => {
            (listeners[name] ||= []).push(cb);
          },
          removeEventListener: (name: string, cb: () => void) => {
            listeners[name] = (listeners[name] || []).filter((fn) => fn !== cb);
          },
          finished: Promise.resolve(),
        };

        return player;
      };
    });

    const assertAutocomplete = async (options: {
      loader: HarnessLoader;
      inputs: MatInputHarness[];
      inputIndex: number;
      name: string;
      query: string;
      assertedOptionsCount: number;
      assertedSelectedValue: string;
    }): Promise<void> => {
      const {
        loader,
        inputs,
        inputIndex,
        name,
        query,
        assertedOptionsCount,
        assertedSelectedValue,
      } = options;

      const input = inputs[inputIndex];
      await input.focus();
      await input.setValue(query);

      const autocomplete = await loader.getHarness(
        MatAutocompleteHarness.with({
          selector: `[ng-reflect-name="${name}"]`,
        }),
      );
      const autocompleteOptions = await autocomplete.getOptions();
      expect(autocompleteOptions.length).toEqual(assertedOptionsCount);
      await autocompleteOptions[0].click();
      const value = await input.getValue();
      expect(value).toBe(assertedSelectedValue);
    };

    describe("case type selection", () => {
      it("should handle CMMN case type selection", async () => {
        const inputs = await loader.getAllHarnesses(MatInputHarness);
        expect(zakenService.listZaaktypesForCreation).toHaveBeenCalled();

        await assertAutocomplete({
          loader,
          inputs,
          inputIndex: 0,
          name: "zaaktype",
          query: "cmmn",
          assertedOptionsCount: 2,
          assertedSelectedValue: "test-cmmn-description-1",
        });
        expect(identityService.listGroups).toHaveBeenCalled();

        await assertAutocomplete({
          loader,
          inputs,
          inputIndex: 4,
          name: "groep",
          query: "cmmn",
          assertedOptionsCount: 1,
          assertedSelectedValue: "test group CMMN",
        });
        expect(identityService.listUsersInGroup).toHaveBeenCalled();

        await assertAutocomplete({
          loader,
          inputs,
          inputIndex: 5,
          name: "behandelaar",
          query: "test",
          assertedOptionsCount: 1,
          assertedSelectedValue: "test user",
        });
      });

      it("should handle BPMN case type selection", async () => {
        const inputs = await loader.getAllHarnesses(MatInputHarness);
        expect(zakenService.listZaaktypesForCreation).toHaveBeenCalled();

        await assertAutocomplete({
          loader,
          inputs,
          inputIndex: 0,
          name: "zaaktype",
          query: "bpmn",
          assertedOptionsCount: 1,
          assertedSelectedValue: "test-bpmn-description-1",
        });
        expect(identityService.listGroups).toHaveBeenCalled();

        await assertAutocomplete({
          loader,
          inputs,
          inputIndex: 4,
          name: "groep",
          query: "bpmn",
          assertedOptionsCount: 1,
          assertedSelectedValue: "test group BPMN",
        });
        expect(identityService.listUsersInGroup).toHaveBeenCalledTimes(0);

        expect(await inputs[5].isDisabled()).toBe(true);
      });
    });

    it(`should set the confidentiality notice`, async () => {
      const inputs = await loader.getAllHarnesses(MatInputHarness);

      await inputs[0].focus();
      await inputs[0].setValue("test");

      const autocomplete = await loader.getHarness(
        MatAutocompleteHarness.with({
          selector: '[ng-reflect-name="zaaktype"]',
        }),
      );
      const options = await autocomplete.getOptions();
      await options[0].click();
      const value = await inputs[0].getValue();
      expect(value).toBeTruthy();

      const selectFields = await loader.getAllHarnesses(MatSelectHarness);
      expect(selectFields.length).toEqual(2);
      expect(await selectFields[1].getValueText()).toBe("Openbaar");
    });

    it("should fill all fields and submit", async () => {
      const mockZaakResponse = fromPartial<GeneratedType<"RestZaak">>({
        identificatie: "test-zaak-uuid-123",
      });
      jest.spyOn(zakenService, "createZaak").mockReturnValue({
        mutationKey: [],
        mutationFn: () => Promise.resolve(mockZaakResponse),
      });
      const navigateSpy = jest
        .spyOn(router, "navigate")
        .mockResolvedValue(true);

      const inputs = await loader.getAllHarnesses(MatInputHarness);
      const selects = await loader.getAllHarnesses(MatSelectHarness);
      const submitButton = await loader.getHarness(
        MatButtonHarness.with({ text: "actie.aanmaken" }),
      );

      expect(await submitButton.isDisabled()).toBe(true);

      await assertAutocomplete({
        loader,
        inputs,
        inputIndex: 0,
        name: "zaaktype",
        query: "cmmn",
        assertedOptionsCount: 2,
        assertedSelectedValue: "test-cmmn-description-1",
      });

      await inputs[6].setValue("Automated test description");
      expect(await inputs[6].getValue()).toBe("Automated test description");

      await selects[0].clickOptions({ text: "Rooksignalen" });
      expect(await selects[0].getValueText()).toBe("Rooksignalen");

      expect(await submitButton.isDisabled()).toBe(false);
      await submitButton.click();

      expect(zakenService.createZaak).toHaveBeenCalled();
      expect(navigateSpy).toHaveBeenCalledWith([
        "/zaken/",
        "test-zaak-uuid-123",
      ]);
    });
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
