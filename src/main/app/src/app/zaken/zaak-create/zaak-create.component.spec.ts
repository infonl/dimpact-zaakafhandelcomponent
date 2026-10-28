/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { provideZonelessChangeDetection } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatAutocompleteHarness } from "@angular/material/autocomplete/testing";
import { MatButtonHarness } from "@angular/material/button/testing";
import { MatHint, MatLabel } from "@angular/material/form-field";
import { MatIcon } from "@angular/material/icon";
import { MatInputHarness } from "@angular/material/input/testing";
import { MatSelectHarness } from "@angular/material/select/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { Router, RouterModule, Routes } from "@angular/router";
import { provideTranslateService } from "@ngx-translate/core";
import {
  provideQueryClient,
  QueryClient,
} from "@tanstack/angular-query-experimental";
import { notifyManager } from "@tanstack/query-core";
import { screen } from "@testing-library/angular";
import { of } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { sleep } from "../../../../setupJest";
import { ReferentieTabelService } from "../../admin/referentie-tabel.service";
import { UtilService } from "../../core/service/util.service";
import { IdentityService } from "../../identity/identity.service";
import { KlantenService } from "../../klanten/klanten.service";
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
  let referentieTabelService: ReferentieTabelService;
  let fixture: ComponentFixture<ZaakCreateComponent>;
  let loader: HarnessLoader;
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideTranslateService(),
        ZakenService,
        KlantenService,
        ReferentieTabelService,
        UtilService,
        IdentityService,
        provideQueryClient(new QueryClient()),
        provideZonelessChangeDetection(),
      ],
      imports: [
        ZaakCreateComponent,
        RouterModule.forRoot(routes),
        NoopAnimationsModule,
        MatHint,
        MatIcon,
        MatLabel,
      ],
    }).compileComponents();

    identityService = TestBed.inject(IdentityService);
    const queryClient = TestBed.inject(QueryClient);
    for (const zaaktypeDescription of [
      "test-cmmn-description-1",
      "test-bpmn-description-1",
    ]) {
      queryClient.setQueryData(
        identityService.listBehandelaarGroupsForZaaktype(zaaktypeDescription)
          .queryKey,
        [
          { id: "test-cmmn-group-id", naam: "test group CMMN" },
          { id: "test-bpmn-group-id", naam: "test group BPMN" },
        ],
      );
    }
    for (const groupId of ["test-cmmn-group-id", "test-bpmn-group-id"]) {
      queryClient.setQueryData(
        identityService.listUsersInGroup(groupId).queryKey,
        [{ username: "test-user-id", naam: "test user" }],
      );
    }

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
            defaultGroepId: "test-bpmn-group-id",
            zaaktype: { uuid: "uuid-test-bpmn-zaaktype-1" },
          },
        }),
      ]),
    );
    jest.spyOn(zakenService, "createZaak").mockReturnValue({
      mutationKey: [],
      mutationFn: () =>
        Promise.resolve(
          fromPartial<GeneratedType<"CreateZaakResponse">>({
            identificatie: "test-zaak-uuid-123",
          }),
        ),
    });

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
      query: string;
    }) => {
      const { loader, inputs, inputIndex, query } = options;

      const input = inputs[inputIndex];
      await input.focus();
      await input.setValue(query);

      const autocompletes = await loader.getAllHarnesses(
        MatAutocompleteHarness,
      );
      let autocomplete: MatAutocompleteHarness | undefined;
      for (const candidate of autocompletes) {
        if (await candidate.isOpen()) {
          autocomplete = candidate;
          break;
        }
      }
      if (!autocomplete) {
        throw new Error(
          "No open autocomplete panel found for the focused input",
        );
      }
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
          query: "cmmn",
        });
        expect(autocompleteOptions.length).toEqual(2);

        await autocompleteOptions[0].click();
        expect(await input.getValue()).toBe("test-cmmn-description-1");

        // --- Groep ---
        ({ autocompleteOptions, input } = await getAutocompleteOptions({
          loader,
          inputs,
          inputIndex: 4,
          query: "cmmn",
        }));
        expect(autocompleteOptions.length).toEqual(1);

        await autocompleteOptions[0].click();
        expect(await input.getValue()).toBe("test group CMMN");

        // --- Behandelaar ---
        ({ autocompleteOptions, input } = await getAutocompleteOptions({
          loader,
          inputs,
          inputIndex: 5,
          query: "test",
        }));
        expect(autocompleteOptions.length).toEqual(1);

        await autocompleteOptions[0].click();
        expect(await input.getValue()).toBe("test user");
      });

      it("should pre-select the default groep and behandelaar of the zaaktype", async () => {
        const inputs = await loader.getAllHarnesses(MatInputHarness);
        const { autocompleteOptions } = await getAutocompleteOptions({
          loader,
          inputs,
          inputIndex: 0,
          query: "cmmn",
        });

        await autocompleteOptions[0].click();

        expect(await inputs[4].getValue()).toBe("test group CMMN");
        expect(await inputs[5].getValue()).toBe("test user");
      });

      describe("when the user switches to a zaaktype with another default behandelaar", () => {
        const pickCaseType = async (
          caseType: GeneratedType<"RestZaaktype">,
        ) => {
          fixture.componentInstance["form"].controls.zaaktype.setValue(
            caseType,
          );
          await sleep();
          await fixture.whenStable();
        };

        const caseTypeWithDefaults = (
          omschrijving: string,
          defaultGroepId: string,
          defaultBehandelaarId: string,
        ) => {
          TestBed.inject(QueryClient).setQueryData(
            identityService.listBehandelaarGroupsForZaaktype(omschrijving)
              .queryKey,
            [
              { id: "test-cmmn-group-id", naam: "test group CMMN" },
              { id: "test-bpmn-group-id", naam: "test group BPMN" },
            ],
          );
          return fromPartial<GeneratedType<"RestZaaktype">>({
            omschrijving,
            vertrouwelijkheidaanduiding: "OPENBAAR",
            zaakafhandelparameters: { defaultGroepId, defaultBehandelaarId },
          });
        };

        beforeEach(() => {
          const usersByGroupId: Record<string, GeneratedType<"RestUser">[]> = {
            "test-cmmn-group-id": [
              { username: "test-user-id", naam: "test user" },
              { username: "test-cmmn-user-id", naam: "test CMMN user" },
            ],
            "test-bpmn-group-id": [
              { username: "test-bpmn-user-id", naam: "test BPMN user" },
            ],
          };
          for (const groupId of Object.keys(usersByGroupId)) {
            TestBed.inject(QueryClient).removeQueries({
              queryKey: identityService.listUsersInGroup(groupId).queryKey,
            });
          }
          const listUsersInGroup =
            identityService.listUsersInGroup.bind(identityService);
          jest
            .spyOn(identityService, "listUsersInGroup")
            .mockImplementation((groupId) => ({
              ...listUsersInGroup(groupId),
              queryFn: () => Promise.resolve(usersByGroupId[groupId]),
            }));
        });

        it("pre-selects the default behandelaar of the new zaaktype when both have the same default groep", async () => {
          TestBed.inject(QueryClient).setQueryData(
            identityService.listUsersInGroup("test-cmmn-group-id").queryKey,
            [
              { username: "test-user-id", naam: "test user" },
              { username: "test-cmmn-user-id", naam: "test CMMN user" },
            ],
          );
          await pickCaseType(
            caseTypeWithDefaults(
              "test-zaaktype-a",
              "test-cmmn-group-id",
              "test-user-id",
            ),
          );

          await pickCaseType(
            caseTypeWithDefaults(
              "test-zaaktype-b",
              "test-cmmn-group-id",
              "test-cmmn-user-id",
            ),
          );

          expect(
            fixture.componentInstance["form"].controls.behandelaar.value,
          ).toEqual({ username: "test-cmmn-user-id", naam: "test CMMN user" });
        });

        it("pre-selects the default behandelaar of the new zaaktype from the members of its own default groep", async () => {
          await pickCaseType(
            caseTypeWithDefaults(
              "test-zaaktype-a",
              "test-cmmn-group-id",
              "test-user-id",
            ),
          );

          await pickCaseType(
            caseTypeWithDefaults(
              "test-zaaktype-b",
              "test-bpmn-group-id",
              "test-bpmn-user-id",
            ),
          );

          expect(
            fixture.componentInstance["form"].controls.behandelaar.value,
          ).toEqual({ username: "test-bpmn-user-id", naam: "test BPMN user" });
        });

        it("leaves the behandelaar empty when the default behandelaar of the new zaaktype is only a member of the previous groep", async () => {
          await pickCaseType(
            caseTypeWithDefaults(
              "test-zaaktype-a",
              "test-cmmn-group-id",
              "test-user-id",
            ),
          );

          await pickCaseType(
            caseTypeWithDefaults(
              "test-zaaktype-b",
              "test-bpmn-group-id",
              "test-cmmn-user-id",
            ),
          );

          expect(
            fixture.componentInstance["form"].controls.behandelaar.value,
          ).toBeNull();
        });
      });

      it("should keep the groep and behandelaar that the user picked when they are read again", async () => {
        const inputs = await loader.getAllHarnesses(MatInputHarness);
        const { autocompleteOptions } = await getAutocompleteOptions({
          loader,
          inputs,
          inputIndex: 0,
          query: "cmmn",
        });
        await autocompleteOptions[0].click();
        const form = fixture.componentInstance["form"];
        form.controls.groep.setValue({
          id: "test-bpmn-group-id",
          naam: "test group BPMN",
        });
        await sleep();
        await fixture.whenStable();
        form.controls.behandelaar.setValue(null);

        const queryClient = TestBed.inject(QueryClient);
        queryClient.setQueryData(
          identityService.listBehandelaarGroupsForZaaktype(
            "test-cmmn-description-1",
          ).queryKey,
          [
            { id: "test-cmmn-group-id", naam: "test group CMMN" },
            { id: "test-bpmn-group-id", naam: "test group BPMN" },
            { id: "test-new-group-id", naam: "test new group" },
          ],
        );
        queryClient.setQueryData(
          identityService.listUsersInGroup("test-bpmn-group-id").queryKey,
          [
            { username: "test-user-id", naam: "test user" },
            { username: "test-new-user-id", naam: "test new user" },
          ],
        );
        await sleep();
        await fixture.whenStable();

        expect(form.controls.groep.value?.id).toBe("test-bpmn-group-id");
        expect(form.controls.behandelaar.value).toBeNull();
      });

      it("keeps the vertrouwelijkheidaanduiding of the picked zaaktype while the user types in the zaaktype field", async () => {
        const inputs = await loader.getAllHarnesses(MatInputHarness);
        const { autocompleteOptions } = await getAutocompleteOptions({
          loader,
          inputs,
          inputIndex: 0,
          query: "cmmn",
        });
        await autocompleteOptions[0].click();
        const form = fixture.componentInstance["form"];
        const vertrouwelijkheidaanduiding =
          form.controls.vertrouwelijkheidaanduiding.value;

        await inputs[0].setValue("bpmn");
        await fixture.whenStable();

        expect(vertrouwelijkheidaanduiding).toBeTruthy();
        expect(form.controls.vertrouwelijkheidaanduiding.value).toEqual(
          vertrouwelijkheidaanduiding,
        );
      });

      it("keeps offering the users of the picked groep while the user types in the groep field", async () => {
        const inputs = await loader.getAllHarnesses(MatInputHarness);
        const { autocompleteOptions } = await getAutocompleteOptions({
          loader,
          inputs,
          inputIndex: 0,
          query: "cmmn",
        });
        await autocompleteOptions[0].click();

        fixture.componentInstance["form"].controls.groep.setValue(
          "test" as unknown as GeneratedType<"RestGroup">,
        );
        await sleep();
        await fixture.whenStable();

        expect(fixture.componentInstance["users"]()).toEqual([
          { username: "test-user-id", naam: "test user" },
        ]);
      });

      it("should handle BPMN case type selection", async () => {
        const inputs = await loader.getAllHarnesses(MatInputHarness);
        expect(zakenService.listZaaktypesForCreation).toHaveBeenCalled();

        // --- Zaaktype ---
        let { autocompleteOptions, input } = await getAutocompleteOptions({
          loader,
          inputs,
          inputIndex: 0,
          query: "bpmn",
        });
        expect(autocompleteOptions.length).toEqual(1);

        await autocompleteOptions[0].click();
        expect(await input.getValue()).toBe("test-bpmn-description-1");

        // --- Groep ---
        ({ autocompleteOptions, input } = await getAutocompleteOptions({
          loader,
          inputs,
          inputIndex: 4,
          query: "bpmn",
        }));
        expect(autocompleteOptions.length).toEqual(1);

        await autocompleteOptions[0].click();
        expect(await input.getValue()).toBe("test group BPMN");

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
          query: "test",
        });
        expect(autocompleteOptions.length).toBeGreaterThan(0);

        await autocompleteOptions[0].click();
        expect(await input.getValue()).toBeTruthy();

        const selectFields = await loader.getAllHarnesses(MatSelectHarness);
        expect(selectFields.length).toEqual(2);
        expect(await selectFields[1].getValueText()).toBe(
          "vertrouwelijkheidaanduiding.openbaar",
        );
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
        await sleep();

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
    it("goes back to the previous page when cancel button is clicked", async () => {
      Object.assign(window.navigation, { canGoBack: true });
      jest.spyOn(history, "back").mockImplementation(() => {});
      const cancelButton = await loader.getHarness(
        MatButtonHarness.with({ text: "actie.annuleren" }),
      );
      await cancelButton.click();
      expect(history.back).toHaveBeenCalled();
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
        fromPartial<GeneratedType<"RestBagObject">>({
          omschrijving: "Teststraat 1",
        }),
      ]);
      expect(fixture.componentInstance.hasBagObject()).toBe(true);
    });

    it("clears BAG objects when clearBagObjecten() is called", () => {
      fixture.componentInstance["form"].controls.bagObjecten.setValue([
        fromPartial<GeneratedType<"RestBagObject">>({
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
        fromPartial<GeneratedType<"RestBagObject">>({
          omschrijving: "Straat 1",
        }),
        fromPartial<GeneratedType<"RestBagObject">>({
          omschrijving: "Straat 2",
        }),
      ]);
      expect(result).toBe("Straat 1 | Straat 2");
    });

    it("bagDisplayValue() returns translated count label when total length > 100", () => {
      const longOmschrijving = "A".repeat(60);
      const result = fixture.componentInstance["bagDisplayValue"]([
        fromPartial<GeneratedType<"RestBagObject">>({
          omschrijving: longOmschrijving,
        }),
        fromPartial<GeneratedType<"RestBagObject">>({
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
      expect(fixture.componentInstance["actionsSidenav"]().opened).toBe(true);
    });

    it("sets activeSideAction and opens sidenav for BAG action", async () => {
      await fixture.componentInstance["openSideNav"](
        "actie.bag-object.koppelen",
      );
      fixture.detectChanges();
      expect(fixture.componentInstance["activeSideAction"]).toBe(
        "actie.bag-object.koppelen",
      );
      expect(fixture.componentInstance["actionsSidenav"]().opened).toBe(true);
    });

    it("renders zac-klant-koppel for initiator action", () => {
      fixture.componentInstance["activeSideAction"] =
        "actie.initiator.koppelen";
      fixture.detectChanges();
      expect(screen.getByText("actie.initiator.koppelen")).toBeInTheDocument();
    });

    it("renders zac-bag-zoek for BAG action", () => {
      fixture.componentInstance["activeSideAction"] =
        "actie.bag-object.koppelen";
      fixture.detectChanges();
      expect(screen.getByText("actie.bag-object.koppelen")).toBeInTheDocument();
    });
  });

  describe("formSubmit()", () => {
    beforeEach(() => notifyManager.setScheduler((fn) => fn()));
    afterEach(() => notifyManager.setScheduler((fn) => setTimeout(fn, 0)));

    it("includes selected BAG objects in the submit payload", () => {
      const bagObject = fromPartial<GeneratedType<"RestBagObject">>({
        omschrijving: "Teststraat 1",
      });

      fixture.componentInstance["form"].controls.bagObjecten.setValue([
        bagObject,
      ]);
      fixture.componentInstance["formSubmit"]();

      const variables = (
        fixture.componentInstance["createZaakMutation"] as unknown as {
          variables: () => unknown;
        }
      ).variables();
      expect(variables).toMatchObject({ bagObjecten: [bagObject] });
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
      expect(fixture.componentInstance["actionsSidenav"]().opened).toBe(false);
    });
  });

  describe("canAddInitiator()", () => {
    it("returns false when both brpKoppelen and kvkKoppelen are false", () => {
      fixture.componentInstance["form"].controls.zaaktype.setValue(
        fromPartial<GeneratedType<"RestZaaktype">>({
          zaakafhandelparameters: {
            betrokkeneKoppelingen: {
              isBrpKoppelenEnabled: false,
              isKvkKoppelenEnabled: false,
            },
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
            betrokkeneKoppelingen: {
              isBrpKoppelenEnabled: true,
              isKvkKoppelenEnabled: false,
            },
          },
        }),
      );
      expect(fixture.componentInstance["canAddInitiator"]()).toBe(true);
    });

    it("returns true when kvkKoppelen is enabled", () => {
      fixture.componentInstance["form"].controls.zaaktype.setValue(
        fromPartial<GeneratedType<"RestZaaktype">>({
          zaakafhandelparameters: {
            betrokkeneKoppelingen: {
              isBrpKoppelenEnabled: false,
              isKvkKoppelenEnabled: true,
            },
          },
        }),
      );
      expect(fixture.componentInstance["canAddInitiator"]()).toBe(true);
    });
  });
});
