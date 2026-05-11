/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { provideHttpClient } from "@angular/common/http";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatAutocompleteHarness } from "@angular/material/autocomplete/testing";
import { MatButtonHarness } from "@angular/material/button/testing";
import { MatHint, MatLabel } from "@angular/material/form-field";
import { MatIcon } from "@angular/material/icon";
import { MatInputHarness } from "@angular/material/input/testing";
import { MatSelectHarness } from "@angular/material/select/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { Router, RouterModule, Routes } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import {
  provideQueryClient,
  QueryClient,
} from "@tanstack/angular-query-experimental";
import { of } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { ReferentieTabelService } from "../../admin/referentie-tabel.service";
import { ZaakafhandelParametersService } from "../../admin/zaakafhandel-parameters.service";
import { UtilService } from "../../core/service/util.service";
import { IdentityService } from "../../identity/identity.service";
import { KlantenService } from "../../klanten/klanten.service";
import { MaterialFormBuilderModule } from "../../shared/material-form-builder/material-form-builder.module";
import { NavigationService } from "../../shared/navigation/navigation.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenService } from "../zaken.service";
import { ZaakCreateComponent } from "./zaak-create.component";

const routes: Routes = [{ path: "", component: ZaakCreateComponent }];

interface AnimationMock {
  play: () => unknown;
  pause: () => unknown;
  cancel: () => unknown;
  finish: () => unknown;
  addEventListener: (name: string, cb: () => unknown) => unknown;
  removeEventListener: (name: string, cb: () => unknown) => unknown;
  finished: Promise<void>;
}

describe(ZaakCreateComponent.name, () => {
  let identityService: IdentityService;
  let zakenService: ZakenService;
  let zaakafhandelParametersService: ZaakafhandelParametersService;
  let utilService: UtilService;
  let referentieTabelService: ReferentieTabelService;
  let fixture: ComponentFixture<ZaakCreateComponent>;
  let loader: HarnessLoader;
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        ZakenService,
        NavigationService,
        KlantenService,
        ReferentieTabelService,
        UtilService,
        IdentityService,
        provideHttpClient(),
        provideQueryClient(new QueryClient()),
      ],
      imports: [
        ZaakCreateComponent,
        RouterModule.forRoot(routes),
        TranslateModule.forRoot(),
        NoopAnimationsModule,
        MaterialFormBuilderModule,
        MatHint,
        MatIcon,
        MatLabel,
      ],
    }).compileComponents();

    identityService = TestBed.inject(IdentityService);
    jest
      .spyOn(identityService, "listBehandelaarGroupsForZaaktype")
      .mockReturnValue(
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
    jest.spyOn(zakenService, "createZaak").mockReturnValue({
      mutationKey: [],
      mutationFn: () =>
        Promise.resolve(
          fromPartial<GeneratedType<"RestZaak">>({
            identificatie: "test-zaak-uuid-123",
          }),
        ),
    });

    zaakafhandelParametersService = TestBed.inject(
      ZaakafhandelParametersService,
    );
    jest
      .spyOn(zaakafhandelParametersService, "getZaaktypeBpmnConfiguration")
      .mockReturnValue(
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
    loader = TestbedHarnessEnvironment.loader(fixture);

    fixture.detectChanges();
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

    const getAutocompleteOptions = async (options: {
      loader: HarnessLoader;
      inputs: MatInputHarness[];
      inputIndex: number;
      name: string;
      query: string;
    }) => {
      const { loader, inputs, inputIndex, name, query } = options;

      const input = inputs[inputIndex];
      await input.focus();
      await input.setValue(query);

      const autocomplete = await loader.getHarness(
        MatAutocompleteHarness.with({
          selector: `[ng-reflect-name="${name}"]`,
        }),
      );
      const autocompleteOptions = await autocomplete.getOptions();

      return { autocomplete, autocompleteOptions, input };
    };

    describe("case type selection", () => {
      it("should handle CMMN case type selection", async () => {
        const inputs = await loader.getAllHarnesses(MatInputHarness);
        expect(zakenService.listZaaktypesForCreation).toHaveBeenCalled();

        // --- Zaaktype ---
        let { autocompleteOptions, input } = await getAutocompleteOptions({
          loader,
          inputs,
          inputIndex: 0,
          name: "zaaktype",
          query: "cmmn",
        });
        expect(autocompleteOptions.length).toEqual(2);

        await autocompleteOptions[0].click();
        expect(await input.getValue()).toBe("test-cmmn-description-1");
        expect(
          identityService.listBehandelaarGroupsForZaaktype,
        ).toHaveBeenCalled();

        // --- Groep ---
        ({ autocompleteOptions, input } = await getAutocompleteOptions({
          loader,
          inputs,
          inputIndex: 4,
          name: "groep",
          query: "cmmn",
        }));
        expect(autocompleteOptions.length).toEqual(1);

        await autocompleteOptions[0].click();
        expect(await input.getValue()).toBe("test group CMMN");
        expect(identityService.listUsersInGroup).toHaveBeenCalled();

        // --- Behandelaar ---
        ({ autocompleteOptions, input } = await getAutocompleteOptions({
          loader,
          inputs,
          inputIndex: 5,
          name: "behandelaar",
          query: "test",
        }));
        expect(autocompleteOptions.length).toEqual(1);

        await autocompleteOptions[0].click();
        expect(await input.getValue()).toBe("test user");
      });

      it("should handle BPMN case type selection", async () => {
        const inputs = await loader.getAllHarnesses(MatInputHarness);
        expect(zakenService.listZaaktypesForCreation).toHaveBeenCalled();

        // --- Zaaktype ---
        let { autocompleteOptions, input } = await getAutocompleteOptions({
          loader,
          inputs,
          inputIndex: 0,
          name: "zaaktype",
          query: "bpmn",
        });
        expect(autocompleteOptions.length).toEqual(1);

        await autocompleteOptions[0].click();
        expect(await input.getValue()).toBe("test-bpmn-description-1");
        expect(
          identityService.listBehandelaarGroupsForZaaktype,
        ).toHaveBeenCalled();

        // --- Groep ---
        ({ autocompleteOptions, input } = await getAutocompleteOptions({
          loader,
          inputs,
          inputIndex: 4,
          name: "groep",
          query: "bpmn",
        }));
        expect(autocompleteOptions.length).toEqual(1);

        await autocompleteOptions[0].click();
        expect(await input.getValue()).toBe("test group BPMN");
        expect(identityService.listUsersInGroup).toHaveBeenCalled();

        // --- Behandelaar should be enabled ---
        expect(await inputs[5].isDisabled()).toBe(false);
      });

      it("should set the confidentiality notice", async () => {
        const inputs = await loader.getAllHarnesses(MatInputHarness);

        // --- Zaaktype ---
        const { autocompleteOptions, input } = await getAutocompleteOptions({
          loader,
          inputs,
          inputIndex: 0,
          name: "zaaktype",
          query: "test",
        });
        expect(autocompleteOptions.length).toBeGreaterThan(0);

        await autocompleteOptions[0].click();
        expect(await input.getValue()).toBeTruthy();

        const selectFields = await loader.getAllHarnesses(MatSelectHarness);
        expect(selectFields.length).toEqual(2);
        expect(await selectFields[1].getValueText()).toBe("Openbaar");
      });

      it("should fill all fields and submit", async () => {
        const navigateSpy = jest
          .spyOn(router, "navigate")
          .mockResolvedValue(true);

        const inputs = await loader.getAllHarnesses(MatInputHarness);
        const selects = await loader.getAllHarnesses(MatSelectHarness);
        const submitButton = await loader.getHarness(
          MatButtonHarness.with({ text: "actie.aanmaken" }),
        );

        expect(await submitButton.isDisabled()).toBe(true);

        // --- Zaaktype ---
        const { autocompleteOptions, input } = await getAutocompleteOptions({
          loader,
          inputs,
          inputIndex: 0,
          name: "zaaktype",
          query: "cmmn",
        });
        expect(autocompleteOptions.length).toEqual(2);

        await autocompleteOptions[0].click();
        expect(await input.getValue()).toBe("test-cmmn-description-1");

        // --- Description ---
        await inputs[6].setValue("Automated test description");
        expect(await inputs[6].getValue()).toBe("Automated test description");

        // --- Select Communication Channel ---
        await selects[0].clickOptions({ text: "Rooksignalen" });
        expect(await selects[0].getValueText()).toBe("Rooksignalen");

        // --- Submit ---
        expect(await submitButton.isDisabled()).toBe(false);
        await submitButton.click();

        expect(zakenService.createZaak).toHaveBeenCalled();
        expect(navigateSpy).toHaveBeenCalledWith([
          "/zaken/",
          "test-zaak-uuid-123",
        ]);
      });
    });
  });

  describe("communication channel", () => {
    it("pre-selects the default channel when it is present in the list", () => {
      jest
        .spyOn(referentieTabelService, "listCommunicatiekanalen")
        .mockReturnValue(of([ZaakCreateComponent.DEFAULT_CHANNEL]));
      fixture = TestBed.createComponent(ZaakCreateComponent);
      fixture.detectChanges();
      expect(
        fixture.componentInstance["form"].controls.communicatiekanaal.value,
      ).toBe(ZaakCreateComponent.DEFAULT_CHANNEL);
    });
  });

  describe("cancel button", () => {
    it("calls navigationService.back() when cancel button is clicked", async () => {
      const navigationService = TestBed.inject(NavigationService);
      jest.spyOn(navigationService, "back");
      const cancelButton = await loader.getHarness(
        MatButtonHarness.with({ text: "actie.annuleren" }),
      );
      await cancelButton.click();
      expect(navigationService.back).toHaveBeenCalled();
    });
  });

  describe("initiator button", () => {
    it("hasInitiator() returns false when no initiator is set", () => {
      expect(fixture.componentInstance.hasInitiator()).toBe(false);
    });

    it("hasInitiator() returns true when an initiator is set", () => {
      fixture.componentInstance[
        "form"
      ].controls.initiatorIdentificatie.enable();
      fixture.componentInstance[
        "form"
      ].controls.initiatorIdentificatie.setValue(
        fromPartial<GeneratedType<"BetrokkeneIdentificatie">>({
          type: "BSN",
          bsn: "123456789",
        }),
      );
      expect(fixture.componentInstance.hasInitiator()).toBe(true);
    });

    it("clears the initiator value when clearInitiator() is called", () => {
      fixture.componentInstance[
        "form"
      ].controls.initiatorIdentificatie.enable();
      fixture.componentInstance[
        "form"
      ].controls.initiatorIdentificatie.setValue(
        fromPartial<GeneratedType<"BetrokkeneIdentificatie">>({
          type: "BSN",
          bsn: "123456789",
        }),
      );
      fixture.componentInstance.clearInitiator();
      expect(
        fixture.componentInstance["form"].controls.initiatorIdentificatie.value,
      ).toBeNull();
    });
  });

  describe("BAG object button", () => {
    it("hasBagObject() returns false when no BAG objects are linked", () => {
      expect(fixture.componentInstance.hasBagObject()).toBe(false);
    });

    it("hasBagObject() returns true when BAG objects are linked", () => {
      fixture.componentInstance["form"].controls.bagObjecten.setValue([
        fromPartial<GeneratedType<"RESTBAGObject">>({
          omschrijving: "Teststraat 1",
        }),
      ]);
      expect(fixture.componentInstance.hasBagObject()).toBe(true);
    });

    it("clears BAG objects when clearBagObjecten() is called", () => {
      fixture.componentInstance["form"].controls.bagObjecten.setValue([
        fromPartial<GeneratedType<"RESTBAGObject">>({
          omschrijving: "Teststraat 1",
        }),
      ]);
      fixture.componentInstance.clearBagObjecten();
      expect(
        fixture.componentInstance["form"].controls.bagObjecten.value,
      ).toEqual([]);
    });

    it("bagDisplayValue() joins omschrijving values when total length ≤ 100", () => {
      const result = fixture.componentInstance["bagDisplayValue"]([
        fromPartial<GeneratedType<"RESTBAGObject">>({
          omschrijving: "Straat 1",
        }),
        fromPartial<GeneratedType<"RESTBAGObject">>({
          omschrijving: "Straat 2",
        }),
      ]);
      expect(result).toBe("Straat 1 | Straat 2");
    });

    it("bagDisplayValue() returns translated count label when total length > 100", () => {
      const longOmschrijving = "A".repeat(60);
      const result = fixture.componentInstance["bagDisplayValue"]([
        fromPartial<GeneratedType<"RESTBAGObject">>({
          omschrijving: longOmschrijving,
        }),
        fromPartial<GeneratedType<"RESTBAGObject">>({
          omschrijving: longOmschrijving,
        }),
      ]);
      expect(result).not.toBe(`${longOmschrijving} | ${longOmschrijving}`);
    });
  });

  describe("sidenav", () => {
    it("sets activeSideAction and opens sidenav for initiator action", async () => {
      await fixture.componentInstance["openSideNav"](
        "actie.initiator.koppelen",
      );
      fixture.detectChanges();
      expect(fixture.componentInstance["activeSideAction"]).toBe(
        "actie.initiator.koppelen",
      );
      expect(fixture.componentInstance["actionsSidenav"].opened).toBe(true);
    });

    it("sets activeSideAction and opens sidenav for BAG action", async () => {
      await fixture.componentInstance["openSideNav"](
        "actie.bagObject.koppelen",
      );
      fixture.detectChanges();
      expect(fixture.componentInstance["activeSideAction"]).toBe(
        "actie.bagObject.koppelen",
      );
      expect(fixture.componentInstance["actionsSidenav"].opened).toBe(true);
    });

    it("renders zac-klant-koppel for initiator action", () => {
      fixture.componentInstance["activeSideAction"] =
        "actie.initiator.koppelen";
      fixture.detectChanges();
      expect(
        fixture.nativeElement.querySelector("zac-klant-koppel"),
      ).not.toBeNull();
    });

    it("renders zac-bag-zoek for BAG action", () => {
      fixture.componentInstance["activeSideAction"] =
        "actie.bagObject.koppelen";
      fixture.detectChanges();
      expect(
        fixture.nativeElement.querySelector("zac-bag-zoek"),
      ).not.toBeNull();
    });
  });

  describe("initiatorSelected()", () => {
    it("sets initiatorIdentificatie value and closes the sidenav", async () => {
      await fixture.componentInstance["openSideNav"](
        "actie.initiator.koppelen",
      );
      await fixture.componentInstance.initiatorSelected(
        fromPartial<GeneratedType<"RestPersoon">>({
          identificatieType: "BSN",
          bsn: "123456789",
        }),
      );
      expect(
        fixture.componentInstance["form"].controls.initiatorIdentificatie.value,
      ).toEqual(expect.objectContaining({ type: "BSN" }));
      expect(fixture.componentInstance["actionsSidenav"].opened).toBe(false);
    });
  });

  describe("canAddInitiator()", () => {
    it("returns false when both brpKoppelen and kvkKoppelen are false", () => {
      fixture.componentInstance["form"].controls.zaaktype.setValue(
        fromPartial<GeneratedType<"RestZaaktype">>({
          zaakafhandelparameters: {
            betrokkeneKoppelingen: { brpKoppelen: false, kvkKoppelen: false },
          },
        }),
      );
      expect(fixture.componentInstance["canAddInitiator"]()).toBe(false);
    });

    it("returns false when zaaktype has no betrokkeneKoppelingen", () => {
      fixture.componentInstance["form"].controls.zaaktype.setValue(
        fromPartial<GeneratedType<"RestZaaktype">>({
          zaakafhandelparameters: { betrokkeneKoppelingen: undefined },
        }),
      );
      expect(fixture.componentInstance["canAddInitiator"]()).toBe(false);
    });

    it("returns true when brpKoppelen is enabled", () => {
      fixture.componentInstance["form"].controls.zaaktype.setValue(
        fromPartial<GeneratedType<"RestZaaktype">>({
          zaakafhandelparameters: {
            betrokkeneKoppelingen: { brpKoppelen: true, kvkKoppelen: false },
          },
        }),
      );
      expect(fixture.componentInstance["canAddInitiator"]()).toBe(true);
    });

    it("returns true when kvkKoppelen is enabled", () => {
      fixture.componentInstance["form"].controls.zaaktype.setValue(
        fromPartial<GeneratedType<"RestZaaktype">>({
          zaakafhandelparameters: {
            betrokkeneKoppelingen: { brpKoppelen: false, kvkKoppelen: true },
          },
        }),
      );
      expect(fixture.componentInstance["canAddInitiator"]()).toBe(true);
    });
  });
});
