/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import { inputBinding } from "@angular/core";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import {
  provideQueryClient,
  queryOptions,
} from "@tanstack/angular-query-experimental";
import { notifyManager } from "@tanstack/query-core";
import { render, screen } from "@testing-library/angular";
import { fromPartial } from "src/test-helpers";
import { testQueryClient } from "../../../../setupJest";
import { KlantenService } from "../../klanten/klanten.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { BetrokkeneLinkComponent } from "./betrokkene-link.component";

const makePersoonBetrokkene = (
  fields: Partial<GeneratedType<"RestZaakBetrokkene">> = {},
) =>
  fromPartial<GeneratedType<"RestZaakBetrokkene">>({
    type: "BSN",
    identificatieType: "BSN",
    temporaryPersonId: "temp-person-123",
    ...fields,
  });

const makeBedrijfBetrokkene = (
  fields: Partial<GeneratedType<"RestZaakBetrokkene">> = {},
) =>
  fromPartial<GeneratedType<"RestZaakBetrokkene">>({
    type: "RSIN",
    identificatieType: "RSIN",
    kvkNummer: "12345678",
    ...fields,
  });

const mockPersoon = fromPartial<GeneratedType<"RestPersoon">>({
  temporaryPersonId: "temp-person-123",
});

const mockBedrijf = fromPartial<GeneratedType<"RestBedrijf">>({
  kvkNummer: "12345678",
  rsin: "123456789",
});

const persoonQueryKey = ["betrokkene-link-spec-persoon"];
const bedrijfQueryKey = ["betrokkene-link-spec-bedrijf"];

const setup = (
  betrokkene: GeneratedType<"RestZaakBetrokkene">,
  zaaktypeUuid = "zaaktype-uuid",
) =>
  render(BetrokkeneLinkComponent, {
    bindings: [
      inputBinding("betrokkene", () => betrokkene),
      inputBinding("zaaktypeUuid", () => zaaktypeUuid),
    ],
    imports: [NoopAnimationsModule, TranslateModule.forRoot()],
    providers: [
      provideHttpClient(),
      provideRouter([]),
      provideQueryClient(testQueryClient),
    ],
  });

describe(BetrokkeneLinkComponent.name, () => {
  beforeEach(() => {
    notifyManager.setScheduler((fn) => fn());

    jest.spyOn(KlantenService.prototype, "readPersoon").mockReturnValue(
      queryOptions({
        queryKey: persoonQueryKey,
        queryFn: async () => mockPersoon,
      }) as ReturnType<KlantenService["readPersoon"]>,
    );
    jest.spyOn(KlantenService.prototype, "readBedrijf").mockReturnValue(
      queryOptions({
        queryKey: bedrijfQueryKey,
        queryFn: async () => mockBedrijf,
      }) as ReturnType<KlantenService["readBedrijf"]>,
    );
  });

  afterEach(() => {
    notifyManager.setScheduler((fn) => setTimeout(fn, 0));
  });

  it("links to the persoon of a BSN betrokkene", async () => {
    testQueryClient.setQueryData(persoonQueryKey, mockPersoon);

    await setup(makePersoonBetrokkene({ temporaryPersonId: "temp-456" }));

    expect(
      screen.getByRole("link", { name: "actie.persoon.bekijken" }),
    ).toHaveAttribute("href", expect.stringContaining("persoon"));
  });

  it("links to the bedrijf of a betrokkene with a kvkNummer", async () => {
    testQueryClient.setQueryData(bedrijfQueryKey, mockBedrijf);

    await setup(makeBedrijfBetrokkene({ kvkNummer: "12345678" }));

    expect(
      screen.getByRole("link", { name: "actie.bedrijf.bekijken" }),
    ).toHaveAttribute("href", expect.stringContaining("12345678"));
  });

  it("warns instead of linking when the bedrijf has no kvkNummer", async () => {
    testQueryClient.setQueryData(bedrijfQueryKey, mockBedrijf);

    await setup(makeBedrijfBetrokkene({ kvkNummer: undefined }));

    expect(screen.getByText("warning")).toBeVisible();
    expect(screen.queryByRole("link")).toBeNull();
  });

  it("shows no link for a BSN betrokkene without a temporaryPersonId", async () => {
    await setup(makePersoonBetrokkene({ temporaryPersonId: undefined }));

    expect(screen.queryByRole("link")).toBeNull();
  });
});
