/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { ActivatedRoute, RouterModule } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { render, screen, within } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { of } from "rxjs";
import { createMutationOptions, fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../setupJest";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { IdentityService } from "../../identity/identity.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { MailtemplateBeheerService } from "../mailtemplate-beheer.service";
import { ReferentieTabelService } from "../referentie-tabel.service";
import { ZaakafhandelParametersService } from "../zaakafhandel-parameters.service";
import { ParametersEditCmmnComponent } from "./parameters-edit-cmmn.component";

// rendering this seven step form once per test needs more room than the default timeout
describe("Algemeen form step", () => {
  const user = userEvent.setup();

  const caseDefinition = fromPartial<GeneratedType<"RESTCaseDefinition">>({
    key: "case-1",
    naam: "Case Definition 1",
  });

  const groups = [
    { id: "test-group-id", naam: "test-group" },
    { id: "test-group-id-2", naam: "test-group-2" },
  ];

  const usersOfDefaultGroup = [
    { id: "test-user-id", naam: "test-user" },
    { id: "test-user-id-2", naam: "test-user-2" },
  ];

  function createParameters(
    overrides: Partial<GeneratedType<"RestZaaktypeConfiguration">> = {},
  ) {
    return fromPartial<GeneratedType<"RestZaaktypeConfiguration">>({
      caseDefinition,
      defaultGroepId: "test-group-id",
      defaultBehandelaarId: "test-user-id",
      zaaktype: { uuid: "test-uuid" },
      zaakNietOntvankelijkResultaattype: {
        id: "resultaat-1",
        naam: "Afgehandeld",
      },
      zaakAfzenders: [
        {
          speciaal: false,
          defaultMail: true,
          mail: "test@example.com",
          replyTo: undefined,
        },
      ],
      humanTaskParameters: [],
      mailtemplateKoppelingen: [],
      zaakbeeindigParameters: [],
      smartDocuments: { enabledGlobally: false, enabledForZaaktype: false },
      userEventListenerParameters: [],
      betrokkeneKoppelingen: { brpKoppelen: false, kvkKoppelen: false },
      brpDoelbindingen: {
        zoekWaarde: "",
        raadpleegWaarde: "",
        verwerkingregisterWaarde: "",
      },
      productaanvraagtype: null,
      automaticEmailConfirmation: {
        enabled: false,
        templateName: null,
        emailSender: null,
        emailReply: null,
      },
      ...overrides,
    });
  }

  async function setup(parameters = createParameters()) {
    const { fixture } = await render(ParametersEditCmmnComponent, {
      imports: [TranslateModule.forRoot(), RouterModule, NoopAnimationsModule],
      providers: [
        provideQueryClient(testQueryClient),
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: fromPartial<ActivatedRoute>({
            data: of({
              parameters: {
                zaakafhandelParameters: parameters,
                isSavedZaakafhandelParameters: true,
              },
            }),
          }),
        },
        {
          provide: ZaakafhandelParametersService,
          useValue: fromPartial<ZaakafhandelParametersService>({
            listCaseDefinitions: () => of([caseDefinition]),
            listFormulierDefinities: () => of([]),
            listReplyTos: () => of([{ mail: "reply1@example.com" }]),
            listZaakbeeindigRedenen: () => of([]),
            listResultaattypes: () => of([]),
            updateZaakafhandelparameters: () =>
              createMutationOptions(parameters),
          }),
        },
        {
          provide: ReferentieTabelService,
          useValue: fromPartial<ReferentieTabelService>({
            listReferentieTabellen: () => of([]),
            listAfzenders: () => of(["other@example.com"]),
            listBrpSearchValues: () => of([]),
            listBrpViewValues: () => of([]),
            listBrpProcessingValues: () => of([]),
          }),
        },
        {
          provide: IdentityService,
          useValue: fromPartial<IdentityService>({
            listGroups: () => of(groups),
            listUsersInGroup: (groupId: string) =>
              of(groupId === "test-group-id" ? usersOfDefaultGroup : []),
          }),
        },
        {
          provide: MailtemplateBeheerService,
          useValue: fromPartial<MailtemplateBeheerService>({
            listKoppelbareMailtemplates: () => of([]),
          }),
        },
        {
          provide: ConfiguratieService,
          useValue: fromPartial<ConfiguratieService>({
            readBrpDoelbindingSetupEnabled: () => of(false),
          }),
        },
      ],
    });

    await fixture.whenStable();
    fixture.detectChanges();

    await user.click(screen.getByRole("tab", { name: /gegevens.algemeen/ }));
    // the selects resolve their selected option in a microtask after the step is opened
    await sleep();
    fixture.detectChanges();

    return screen.getByRole("tabpanel", { name: /gegevens.algemeen/ });
  }

  async function chooseOption(combobox: HTMLElement, option: string) {
    await user.click(combobox);
    await user.click(screen.getByRole("option", { name: option }));
  }

  it("refuses to save until a zaakmodel and a groep are chosen", async () => {
    const algemeen = await setup(
      createParameters({ caseDefinition: null, defaultGroepId: null }),
    );
    const opslaan = within(algemeen).getByRole("button", {
      name: "actie.opslaan",
    });

    expect(opslaan).toBeDisabled();

    await chooseOption(
      within(algemeen).getByRole("combobox", { name: /zaak-model/i }),
      "Case Definition 1",
    );
    await chooseOption(
      within(algemeen).getByRole("combobox", { name: /^groep$/i }),
      "test-group",
    );

    expect(opslaan).toBeEnabled();
  });

  it("shows a zaaktype without zaakspecifieke autorisatie as such", async () => {
    const algemeen = await setup();

    expect(
      within(algemeen).getByText("zaakspecifiekAutoriseerbaar"),
    ).toBeVisible();
    expect(within(algemeen).getByText("actie.nee")).toBeVisible();
  });

  it("shows a zaaktype with zaakspecifieke autorisatie as such", async () => {
    const algemeen = await setup(
      createParameters({ zaakspecifiekAutoriseerbaar: true }),
    );

    expect(within(algemeen).getByText("actie.ja")).toBeVisible();
  });

  it("preselects the behandelaar of the default groep", async () => {
    const algemeen = await setup();

    expect(
      within(algemeen).getByRole("combobox", { name: /behandelaar/i }),
    ).toHaveTextContent("test-user");
  });

  it("offers the medewerkers of the default groep as behandelaar", async () => {
    const algemeen = await setup();

    await user.click(
      within(algemeen).getByRole("combobox", { name: /behandelaar/i }),
    );

    expect(
      screen.getAllByRole("option").map((option) => option.textContent?.trim()),
    ).toEqual(["-geen.generiek-", "test-user", "test-user-2"]);
  });

  it("forgets the behandelaar when another groep is chosen", async () => {
    const algemeen = await setup();

    await chooseOption(
      within(algemeen).getByRole("combobox", { name: /^groep$/i }),
      "test-group-2",
    );

    const behandelaar = within(algemeen).getByRole("combobox", {
      name: /behandelaar/i,
    });
    expect(behandelaar).not.toHaveTextContent("test-user");

    await user.click(behandelaar);

    expect(
      screen.getAllByRole("option").map((option) => option.textContent?.trim()),
    ).toEqual(["-geen.generiek-"]);
  });
});
