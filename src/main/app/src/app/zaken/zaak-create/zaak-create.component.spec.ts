/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { HttpTestingController } from "@angular/common/http/testing";
import { provideZonelessChangeDetection } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatAutocompleteHarness } from "@angular/material/autocomplete/testing";
import { MatButtonHarness } from "@angular/material/button/testing";
import { MatHint, MatLabel } from "@angular/material/form-field";
import { MatIcon } from "@angular/material/icon";
import { MatInputHarness } from "@angular/material/input/testing";
import { MatSelectHarness } from "@angular/material/select/testing";
import { MatSidenavContainer } from "@angular/material/sidenav";
import { By } from "@angular/platform-browser";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { Router, RouterModule, Routes } from "@angular/router";
import { provideTranslateService } from "@ngx-translate/core";
import {
  provideQueryClient,
  QueryClient,
} from "@tanstack/angular-query-experimental";
import { notifyManager } from "@tanstack/query-core";
import { screen, waitFor } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { createQueryOptions, fromPartial } from "src/test-helpers";
import { sleep } from "../../../../setupJest";
import { ReferentieTabelService } from "../../admin/referentie-tabel.service";
import { UtilService } from "../../core/service/util.service";
import { IdentityService } from "../../identity/identity.service";
import { KlantenService } from "../../klanten/klanten.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenService } from "../zaken.service";
import { ZaakCreateKoppeling } from "./zaak-create-koppeling";
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

  beforeEach(async () => {
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
    jest.spyOn(zakenService, "listZaaktypesForCreationQuery").mockReturnValue(
      createQueryOptions([
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
      ]) as never,
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
    queryClient.setQueryData(
      referentieTabelService.listCommunicatiekanalen().queryKey,
      [
        "Boeman",
        "Koebel",
        "Telegram",
        "Tamtam",
        "PostDuif",
        "Rooksignalen",
        "Telefoon",
      ],
    );

    router = TestBed.inject(Router);
    jest.spyOn(router, "navigate").mockImplementation(async () => true);

    fixture = TestBed.createComponent(ZaakCreateComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);

    fixture.detectChanges();
    await waitFor(() =>
      expect(TestBed.inject(QueryClient).isFetching()).toBe(0),
    );
    await sleep(10);
    fixture.detectChanges();
  });

  describe(ZaakCreateComponent.prototype.caseTypeSelected.name, () => {
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
        expect(zakenService.listZaaktypesForCreationQuery).toHaveBeenCalled();

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
        expect(zakenService.listZaaktypesForCreationQuery).toHaveBeenCalled();

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

  const sidenavContainer = (): HTMLElement =>
    fixture.debugElement.query(By.directive(MatSidenavContainer)).nativeElement;

  it("insets the form as a page", () => {
    expect(sidenavContainer()).toHaveClass("p-2");
  });

  describe("communication channel", () => {
    const renderWithCommunicationChannels = async (channels: string[]) => {
      TestBed.inject(QueryClient).removeQueries({
        queryKey: referentieTabelService.listCommunicatiekanalen().queryKey,
      });
      fixture = TestBed.createComponent(ZaakCreateComponent);
      fixture.detectChanges();
      await sleep();
      TestBed.inject(HttpTestingController)
        .expectOne("/rest/referentietabellen/communicatiekanaal/false")
        .flush(channels);
      await sleep(10);
      fixture.detectChanges();
    };

    it("pre-selects the default channel when it is present in the list", async () => {
      await renderWithCommunicationChannels([
        "Telefoon",
        ZaakCreateComponent.DEFAULT_CHANNEL,
      ]);

      expect(
        fixture.componentInstance["form"].controls.communicatiekanaal.value,
      ).toBe(ZaakCreateComponent.DEFAULT_CHANNEL);
    });

    it("leaves the channel empty when the default channel is not in the list", async () => {
      await renderWithCommunicationChannels(["Telefoon"]);

      expect(
        fixture.componentInstance["form"].controls.communicatiekanaal.value,
      ).toBe("");
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

    it("renders zac-klant-koppel for initiator action", async () => {
      await fixture.componentInstance["openSideNav"](
        "actie.initiator.koppelen",
      );
      fixture.detectChanges();
      expect(screen.getByText("actie.initiator.koppelen")).toBeInTheDocument();
    });

    it("renders zac-bag-zoek for BAG action", async () => {
      await fixture.componentInstance["openSideNav"](
        "actie.bag-object.koppelen",
      );
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

  describe("creating a zaak that is linked to the current zaak", () => {
    const user = userEvent.setup({ delay: null });

    const deelzaaktype = fromPartial<GeneratedType<"RestZaaktype">>({
      uuid: "fake-deelzaaktype-uuid",
      omschrijving: "fakeDeelzaaktype",
      vertrouwelijkheidaanduiding: "OPENBAAR",
      zaakafhandelparameters: {
        defaultGroepId: "test-cmmn-group-id",
        betrokkeneKoppelingen: {
          isBrpKoppelenEnabled: true,
          isKvkKoppelenEnabled: false,
        },
      },
    });
    const otherZaaktype = fromPartial<GeneratedType<"RestZaaktype">>({
      uuid: "fake-other-zaaktype-uuid",
      omschrijving: "fakeOtherZaaktype",
      vertrouwelijkheidaanduiding: "OPENBAAR",
      zaakafhandelparameters: {
        defaultGroepId: "test-cmmn-group-id",
        betrokkeneKoppelingen: {
          isBrpKoppelenEnabled: true,
          isKvkKoppelenEnabled: false,
        },
      },
    });

    const createCurrentZaak = (
      fields: Partial<GeneratedType<"RestZaak">> = {},
    ) =>
      fromPartial<GeneratedType<"RestZaak">>({
        uuid: "fake-current-zaak-uuid",
        identificatie: "ZAAK-2026-001",
        zaaktype: fromPartial<GeneratedType<"RestZaaktype">>({
          uuid: "fake-current-zaaktype-uuid",
          zaaktypeRelaties: [
            { zaaktypeUuid: "fake-deelzaaktype-uuid", relatieType: "DEELZAAK" },
            {
              zaaktypeUuid: "fake-not-startable-zaaktype-uuid",
              relatieType: "DEELZAAK",
            },
          ],
        }),
        ...fields,
      });

    const personInitiator = fromPartial<
      GeneratedType<"BetrokkeneIdentificatie">
    >({
      type: "BSN",
      bsn: "999993896",
      temporaryPersonId: "fake-temporary-person-id",
    });

    const klantQueryOptions = <T,>(klant: T) =>
      ({
        ...createQueryOptions(klant),
        queryKey: ["fake-klant"],
      }) as never;

    const renderLinked = async (koppeling: ZaakCreateKoppeling) => {
      fixture.destroy();
      for (const omschrijving of ["fakeDeelzaaktype", "fakeOtherZaaktype"]) {
        TestBed.inject(QueryClient).setQueryData(
          identityService.listBehandelaarGroupsForZaaktype(omschrijving)
            .queryKey,
          [{ id: "test-cmmn-group-id", naam: "test group CMMN" }],
        );
      }
      jest
        .spyOn(zakenService, "listZaaktypesForCreationQuery")
        .mockReturnValue(
          createQueryOptions([deelzaaktype, otherZaaktype]) as never,
        );
      fixture = TestBed.createComponent(ZaakCreateComponent);
      fixture.autoDetectChanges();
      fixture.componentRef.setInput("koppeling", koppeling);
      const zaakCreated = jest.fn();
      const cancelled = jest.fn();
      fixture.componentInstance.zaakCreated.subscribe(zaakCreated);
      fixture.componentInstance.cancelled.subscribe(cancelled);
      await settle();
      return { zaakCreated, cancelled };
    };

    const settle = async () => {
      fixture.detectChanges();
      await waitFor(() =>
        expect(TestBed.inject(QueryClient).isFetching()).toBe(0),
      );
      await sleep(10);
      await fixture.whenStable();
      fixture.detectChanges();
    };

    const zaaktypeField = () =>
      screen.getByRole("combobox", { name: /zaaktype/i });

    const offeredZaaktypen = async () => {
      await user.click(zaaktypeField());
      return screen
        .queryAllByRole("option")
        .map((option) => option.textContent?.trim());
    };

    const chooseZaaktype = async (omschrijving: string) => {
      await user.click(zaaktypeField());
      await fixture.whenStable();
      await user.click(screen.getByRole("option", { name: omschrijving }));
      await settle();
      await waitFor(() => expect(zaaktypeField()).toHaveValue(omschrijving));
    };

    const fillRequiredFieldsAndSubmit = async () => {
      await user.type(
        screen.getByRole("textbox", { name: /omschrijving/i }),
        "fakeOmschrijving",
      );
      await user.click(
        screen.getByRole("combobox", { name: /communicatiekanaal/i }),
      );
      await fixture.whenStable();
      await user.click(screen.getByRole("option", { name: "Rooksignalen" }));
      await fixture.whenStable();
      await user.click(screen.getByRole("button", { name: "actie.aanmaken" }));
      await settle();
    };

    const mockCreateZaak = (
      mutationFn: jest.Mock = jest.fn().mockResolvedValue(
        fromPartial<GeneratedType<"CreateZaakResponse">>({
          identificatie: "ZAAK-2026-002",
        }),
      ),
    ) => {
      jest
        .spyOn(zakenService, "createZaak")
        .mockReturnValue({ mutationKey: [], mutationFn });
      return mutationFn;
    };

    it("shows the form as a section without the page title", async () => {
      await renderLinked({
        zaak: createCurrentZaak(),
        relatieType: "GERELATEERD",
      });

      expect(
        screen.getByRole("heading", { level: 2, name: "actie.zaak.aanmaken" }),
      ).toBeInTheDocument();
      expect(
        screen.queryByRole("heading", { level: 1 }),
      ).not.toBeInTheDocument();
    });

    it("does not inset the form, so that it lines up with the fields above it in the sidebar", async () => {
      await renderLinked({
        zaak: createCurrentZaak(),
        relatieType: "GERELATEERD",
      });

      expect(sidenavContainer()).not.toHaveClass("p-2");
    });

    it("offers every zaaktype the employee may start for a gerelateerde zaak", async () => {
      await renderLinked({
        zaak: createCurrentZaak(),
        relatieType: "GERELATEERD",
      });

      expect(await offeredZaaktypen()).toEqual([
        "fakeDeelzaaktype",
        "fakeOtherZaaktype",
      ]);
    });

    it("offers only the deelzaaktypen of the current zaaktype that the employee may start for a deelzaak", async () => {
      await renderLinked({
        zaak: createCurrentZaak(),
        relatieType: "DEELZAAK",
      });

      expect(await offeredZaaktypen()).toEqual(["fakeDeelzaaktype"]);
    });

    it("offers no zaaktypen for a deelzaak when the current zaaktype has no deelzaaktype the employee may start", async () => {
      await renderLinked({
        zaak: createCurrentZaak({
          zaaktype: fromPartial<GeneratedType<"RestZaaktype">>({
            uuid: "fake-current-zaaktype-uuid",
            zaaktypeRelaties: [
              {
                zaaktypeUuid: "fake-not-startable-zaaktype-uuid",
                relatieType: "DEELZAAK",
              },
            ],
          }),
        }),
        relatieType: "DEELZAAK",
      });

      expect(await offeredZaaktypen()).toEqual([]);
    });

    it("clears the chosen zaaktype when the relation changes to one that does not offer it", async () => {
      const zaak = createCurrentZaak();
      await renderLinked({ zaak, relatieType: "GERELATEERD" });
      await chooseZaaktype("fakeOtherZaaktype");
      expect(zaaktypeField()).toHaveValue("fakeOtherZaaktype");

      fixture.componentRef.setInput("koppeling", {
        zaak,
        relatieType: "DEELZAAK",
      });
      await settle();

      await waitFor(() => expect(zaaktypeField()).toHaveValue(""));
    });

    it("keeps the chosen zaaktype when the relation changes to one that still offers it", async () => {
      const zaak = createCurrentZaak();
      await renderLinked({ zaak, relatieType: "GERELATEERD" });
      await chooseZaaktype("fakeDeelzaaktype");

      fixture.componentRef.setInput("koppeling", {
        zaak,
        relatieType: "DEELZAAK",
      });
      await settle();

      await waitFor(() =>
        expect(zaaktypeField()).toHaveValue("fakeDeelzaaktype"),
      );
    });

    it("shows the initiator of the current zaak, which cannot be changed", async () => {
      const klantenService = TestBed.inject(KlantenService);
      jest.spyOn(klantenService, "readPersoon").mockReturnValue(
        klantQueryOptions(
          fromPartial<GeneratedType<"RestPersoon">>({
            naam: "fakeInitiatorNaam",
          }),
        ),
      );

      await renderLinked({
        zaak: createCurrentZaak({ initiatorIdentificatie: personInitiator }),
        relatieType: "GERELATEERD",
      });

      expect(screen.getByText("fakeInitiatorNaam")).toBeInTheDocument();
      expect(
        screen.getByRole("textbox", { name: /initiator/i }),
      ).toBeDisabled();
      expect(klantenService.readPersoon).toHaveBeenCalledWith(
        "fake-temporary-person-id",
        "fake-current-zaaktype-uuid",
      );
    });

    it("says that the initiator is copied from the current zaak on the initiator button, which cannot be clicked", async () => {
      jest.spyOn(TestBed.inject(KlantenService), "readPersoon").mockReturnValue(
        klantQueryOptions(
          fromPartial<GeneratedType<"RestPersoon">>({
            naam: "fakeInitiatorNaam",
          }),
        ),
      );
      await renderLinked({
        zaak: createCurrentZaak({ initiatorIdentificatie: personInitiator }),
        relatieType: "GERELATEERD",
      });

      expect(
        screen.getByRole("button", {
          name: "zaak.aanmaken.initiator.overgenomen",
        }),
      ).toBeDisabled();
    });

    it("shows an empty initiator when the current zaak has no initiator", async () => {
      await renderLinked({
        zaak: createCurrentZaak({ initiatorIdentificatie: null }),
        relatieType: "GERELATEERD",
      });

      expect(screen.getByRole("textbox", { name: /initiator/i })).toHaveValue(
        "",
      );
      expect(
        screen.queryByText("zaak.aanmaken.initiator.niet-overgenomen"),
      ).not.toBeInTheDocument();
    });

    it("says that the current zaak has no initiator on the initiator button, which cannot be clicked", async () => {
      await renderLinked({
        zaak: createCurrentZaak({ initiatorIdentificatie: null }),
        relatieType: "GERELATEERD",
      });

      expect(
        screen.getByRole("button", { name: "zaak.aanmaken.initiator.geen" }),
      ).toBeDisabled();
    });

    it("creates the zaak with the initiator of the current zaak and the relation, and reports it instead of opening it", async () => {
      jest.spyOn(TestBed.inject(KlantenService), "readPersoon").mockReturnValue(
        klantQueryOptions(
          fromPartial<GeneratedType<"RestPersoon">>({
            naam: "fakeInitiatorNaam",
          }),
        ),
      );
      const createZaak = mockCreateZaak();
      const { zaakCreated } = await renderLinked({
        zaak: createCurrentZaak({ initiatorIdentificatie: personInitiator }),
        relatieType: "DEELZAAK",
      });

      await chooseZaaktype("fakeDeelzaaktype");
      await fillRequiredFieldsAndSubmit();

      expect(createZaak).toHaveBeenCalledWith(
        expect.objectContaining({
          zaak: expect.objectContaining({
            initiatorIdentificatie: expect.objectContaining({
              type: "BSN",
              temporaryPersonId: "fake-temporary-person-id",
            }),
          }),
          koppeling: {
            zaakUuid: "fake-current-zaak-uuid",
            relatieType: "DEELZAAK",
          },
        }),
        expect.anything(),
      );
      expect(zaakCreated).toHaveBeenCalledWith("ZAAK-2026-002");
      expect(router.navigate).not.toHaveBeenCalled();
    });

    it("creates the zaak without the initiator and says so when the zaaktype does not allow that kind of initiator", async () => {
      jest.spyOn(TestBed.inject(KlantenService), "readBedrijf").mockReturnValue(
        klantQueryOptions(
          fromPartial<GeneratedType<"RestBedrijf">>({
            naam: "fakeBedrijfNaam",
          }),
        ),
      );
      const createZaak = mockCreateZaak();
      await renderLinked({
        zaak: createCurrentZaak({
          initiatorIdentificatie: fromPartial<
            GeneratedType<"BetrokkeneIdentificatie">
          >({
            type: "VN",
            kvkNummer: "12345678",
            vestigingsnummer: "000012345678",
          }),
        }),
        relatieType: "GERELATEERD",
      });
      expect(screen.getByText("fakeBedrijfNaam")).toBeInTheDocument();

      await chooseZaaktype("fakeOtherZaaktype");

      expect(
        screen.getByText("zaak.aanmaken.initiator.niet-overgenomen"),
      ).toBeInTheDocument();
      expect(screen.queryByText("fakeBedrijfNaam")).not.toBeInTheDocument();

      await fillRequiredFieldsAndSubmit();

      expect(createZaak).toHaveBeenCalledWith(
        expect.objectContaining({
          zaak: expect.objectContaining({ initiatorIdentificatie: null }),
          koppeling: {
            zaakUuid: "fake-current-zaak-uuid",
            relatieType: "GERELATEERD",
          },
        }),
        expect.anything(),
      );
    });

    it("keeps the entered data and reports nothing when the server refuses the zaak", async () => {
      mockCreateZaak(jest.fn().mockRejectedValue(new Error("fakeError")));
      const { zaakCreated } = await renderLinked({
        zaak: createCurrentZaak(),
        relatieType: "GERELATEERD",
      });

      await chooseZaaktype("fakeOtherZaaktype");
      await fillRequiredFieldsAndSubmit();

      expect(zaakCreated).not.toHaveBeenCalled();
      expect(
        screen.getByRole("textbox", { name: /omschrijving/i }),
      ).toHaveValue("fakeOmschrijving");
    });

    it("reports a cancel instead of going back", async () => {
      jest.spyOn(history, "back").mockImplementation(() => {});
      const { cancelled } = await renderLinked({
        zaak: createCurrentZaak(),
        relatieType: "GERELATEERD",
      });

      await user.click(screen.getByRole("button", { name: "actie.annuleren" }));

      expect(cancelled).toHaveBeenCalled();
      expect(history.back).not.toHaveBeenCalled();
    });
  });
});
