/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { render, screen } from "@testing-library/angular";
import { of } from "rxjs";
import { InformatieObjectenService } from "src/app/informatie-objecten/informatie-objecten.service";
import { createQueryOptions, fromPartial } from "src/test-helpers";
import { testQueryClient } from "../../../../../setupJest";
import { SmartDocumentsService } from "../../smart-documents.service";
import { SmartDocumentsFormComponent } from "./smart-documents-form.component";

describe(SmartDocumentsFormComponent.name, () => {
  async function setup({
    enabledGlobally,
    enabledForZaaktype,
  }: { enabledGlobally?: boolean; enabledForZaaktype?: boolean } = {}) {
    const getAllSmartDocumentsTemplateGroups = jest
      .fn()
      .mockReturnValue(of([]));
    const getTemplatesMappingQuery = jest
      .fn()
      .mockReturnValue(
        fromPartial(
          createQueryOptions<
            ReturnType<SmartDocumentsService["flattenGroups"]>
          >([]),
        ),
      );
    const listInformatieobjecttypes = jest.fn().mockReturnValue(of([]));

    const rendered = await render(SmartDocumentsFormComponent, {
      imports: [TranslateModule.forRoot(), NoopAnimationsModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideQueryClient(testQueryClient),
        {
          provide: SmartDocumentsService,
          useValue: fromPartial<SmartDocumentsService>({
            getAllSmartDocumentsTemplateGroups,
            getTemplatesMappingQuery,
            addParentIdsToTemplates: () => [],
            addTemplateMappings: () => [],
            flattenGroups: () => [],
            getTemplateMappings: () => [],
          }),
        },
        {
          provide: InformatieObjectenService,
          useValue: fromPartial<InformatieObjectenService>({
            listInformatieobjecttypes,
          }),
        },
      ],
      inputs: {
        zaakTypeUuid: "test-zaaktype-uuid",
        enabledGlobally,
        enabledForZaaktype,
      },
    });

    await rendered.fixture.whenStable();
    rendered.fixture.detectChanges();

    return {
      ...rendered,
      getAllSmartDocumentsTemplateGroups,
      getTemplatesMappingQuery,
      listInformatieobjecttypes,
    };
  }

  it("should call getAllSmartDocumentsTemplateGroups on init", async () => {
    const { getAllSmartDocumentsTemplateGroups } = await setup();

    expect(getAllSmartDocumentsTemplateGroups).toHaveBeenCalled();
  });

  it("should call getTemplatesMappingQuery with zaakTypeUuid on init", async () => {
    const { getTemplatesMappingQuery } = await setup();

    expect(getTemplatesMappingQuery).toHaveBeenCalledWith("test-zaaktype-uuid");
  });

  it("should call listInformatieobjecttypes with zaakTypeUuid on init", async () => {
    const { listInformatieobjecttypes } = await setup();

    expect(listInformatieobjecttypes).toHaveBeenCalledWith(
      "test-zaaktype-uuid",
    );
  });

  it("should not render the card when enabledGlobally is false", async () => {
    await setup({ enabledGlobally: false });

    expect(
      screen.queryByRole("heading", { name: "title.smartdocuments.form" }),
    ).not.toBeInTheDocument();
  });

  describe("when enabledGlobally is true and enabledForZaaktype is false", () => {
    it("should render the card", async () => {
      await setup({ enabledGlobally: true, enabledForZaaktype: false });

      expect(
        screen.getByRole("heading", { name: "title.smartdocuments.form" }),
      ).toBeVisible();
    });

    it("should initialize enabledForZaaktypeForm with false", async () => {
      const { fixture } = await setup({
        enabledGlobally: true,
        enabledForZaaktype: false,
      });

      expect(
        fixture.componentInstance.enabledForZaaktypeForm.value
          .enabledForZaaktype,
      ).toBe(false);
    });

    it("enabledForZaaktypeValue should return false", async () => {
      const { fixture } = await setup({
        enabledGlobally: true,
        enabledForZaaktype: false,
      });

      expect(fixture.componentInstance.enabledForZaaktypeValue).toBe(false);
    });

    it("should show disabled feedback", async () => {
      await setup({ enabledGlobally: true, enabledForZaaktype: false });

      expect(
        screen.getByText("msg.smartdocuments.form.disabled"),
      ).toBeVisible();
    });

    it("should hide the tree form", async () => {
      await setup({ enabledGlobally: true, enabledForZaaktype: false });

      expect(screen.queryByRole("tree")).not.toBeInTheDocument();
    });
  });

  describe("when enabledGlobally is true and enabledForZaaktype is true", () => {
    it("should initialize enabledForZaaktypeForm with true", async () => {
      const { fixture } = await setup({
        enabledGlobally: true,
        enabledForZaaktype: true,
      });

      expect(
        fixture.componentInstance.enabledForZaaktypeForm.value
          .enabledForZaaktype,
      ).toBe(true);
    });

    it("enabledForZaaktypeValue should return true", async () => {
      const { fixture } = await setup({
        enabledGlobally: true,
        enabledForZaaktype: true,
      });

      expect(fixture.componentInstance.enabledForZaaktypeValue).toBe(true);
    });

    it("should hide disabled feedback", async () => {
      await setup({ enabledGlobally: true, enabledForZaaktype: true });

      expect(
        screen.queryByText("msg.smartdocuments.form.disabled"),
      ).not.toBeInTheDocument();
    });

    it("should show the tree form", async () => {
      await setup({ enabledGlobally: true, enabledForZaaktype: true });

      expect(screen.getByRole("tree")).toBeVisible();
    });
  });
});
