/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { render, screen, within } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../../setupJest";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { SmartDocumentsFormComponent } from "./smart-documents-form.component";

const ALL_TEMPLATE_GROUPS_URL =
  "/rest/zaakafhandelparameters/smartdocuments-templates";
const TEMPLATES_MAPPING_URL =
  "/rest/zaakafhandelparameters/fakeZaaktypeUuid/smartdocuments-templates-mapping";
const INFORMATIEOBJECTTYPES_URL =
  "/rest/informatieobjecten/informatieobjecttypes/fakeZaaktypeUuid";

const allTemplateGroups: GeneratedType<"RestSmartDocumentsTemplateGroup">[] = [
  {
    id: "fakeGroupId1",
    name: "fakeGroupName1",
    groups: null,
    templates: [
      { id: "fakeTemplateId1", name: "fakeTemplateName1" },
      { id: "fakeTemplateId2", name: "fakeTemplateName2" },
    ],
  },
  {
    id: "fakeGroupId2",
    name: "fakeGroupName2",
    groups: null,
    templates: [{ id: "fakeTemplateId3", name: "fakeTemplateName3" }],
  },
];

const templatesMapping: GeneratedType<"RestMappedSmartDocumentsTemplateGroup">[] =
  [
    {
      id: "fakeGroupId1",
      name: "fakeGroupName1",
      groups: null,
      templates: [
        {
          id: "fakeTemplateId1",
          name: "fakeTemplateName1",
          informatieObjectTypeUUID: "fakeInformatieobjecttypeUuid1",
        },
      ],
    },
  ];

const informatieobjecttypes: GeneratedType<"RestInformatieobjecttype">[] = [
  fromPartial({
    uuid: "fakeInformatieobjecttypeUuid1",
    omschrijving: "Type A",
    vertrouwelijkheidaanduiding: "OPENBAAR",
  }),
  fromPartial({
    uuid: "fakeInformatieobjecttypeUuid2",
    omschrijving: "Type B",
    vertrouwelijkheidaanduiding: "VERTROUWELIJK",
  }),
];

describe(SmartDocumentsFormComponent.name, () => {
  const user = userEvent.setup();

  async function setup({
    enabledGlobally,
    enabledForZaaktype,
  }: { enabledGlobally?: boolean; enabledForZaaktype?: boolean } = {}) {
    const rendered = await render(SmartDocumentsFormComponent, {
      imports: [TranslateModule.forRoot(), NoopAnimationsModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideQueryClient(testQueryClient),
      ],
      inputs: {
        zaakTypeUuid: "fakeZaaktypeUuid",
        enabledGlobally,
        enabledForZaaktype,
      },
    });

    const httpTestingController = TestBed.inject(HttpTestingController);
    await sleep();
    httpTestingController
      .expectOne(ALL_TEMPLATE_GROUPS_URL)
      .flush(allTemplateGroups);
    httpTestingController
      .expectOne(TEMPLATES_MAPPING_URL)
      .flush(templatesMapping);
    httpTestingController
      .expectOne(INFORMATIEOBJECTTYPES_URL)
      .flush(informatieobjecttypes);
    await sleep();
    await sleep();
    rendered.fixture.detectChanges();
    await rendered.fixture.whenStable();
    rendered.fixture.detectChanges();

    return { ...rendered, httpTestingController };
  }

  function enabledForZaaktypeSwitch() {
    return screen.getByRole("switch");
  }

  function groupNode(groupName: string) {
    return screen.getByRole("treeitem", { name: new RegExp(groupName) });
  }

  async function toggleGroup(groupName: string) {
    await user.click(
      screen.getByRole("button", { name: `Toggle ${groupName}` }),
    );
  }

  function templateNode(templateName: string) {
    return screen.getByRole("treeitem", { name: new RegExp(templateName) });
  }

  async function chooseInformatieobjecttype(
    templateName: string,
    omschrijving: string,
  ) {
    await user.click(within(templateNode(templateName)).getByRole("combobox"));
    await user.click(screen.getByRole("option", { name: omschrijving }));
  }

  it("fetches the template mapping and the informatieobjecttypen of the zaaktype", async () => {
    const { httpTestingController } = await setup();

    httpTestingController.verify();
  });

  it("does not render the card when enabledGlobally is false", async () => {
    await setup({ enabledGlobally: false });

    expect(
      screen.queryByRole("heading", { name: "title.smartdocuments.form" }),
    ).not.toBeInTheDocument();
  });

  it("does not count as enabled for the zaaktype when SmartDocuments is not enabled globally", async () => {
    const { fixture } = await setup({
      enabledGlobally: false,
      enabledForZaaktype: true,
    });

    expect(fixture.componentInstance.enabledForZaaktypeValue).toBe(false);
  });

  it("renders the card once enabledGlobally changes to true", async () => {
    const { fixture } = await setup({
      enabledGlobally: false,
      enabledForZaaktype: true,
    });

    fixture.componentRef.setInput("enabledGlobally", true);
    fixture.detectChanges();

    expect(
      screen.getByRole("heading", { name: "title.smartdocuments.form" }),
    ).toBeVisible();
    expect(fixture.componentInstance.enabledForZaaktypeValue).toBe(true);
  });

  describe("when enabledGlobally is true and enabledForZaaktype is false", () => {
    it("renders the card", async () => {
      await setup({ enabledGlobally: true, enabledForZaaktype: false });

      expect(
        screen.getByRole("heading", { name: "title.smartdocuments.form" }),
      ).toBeVisible();
    });

    it("shows the switch turned off", async () => {
      await setup({ enabledGlobally: true, enabledForZaaktype: false });

      expect(enabledForZaaktypeSwitch()).not.toBeChecked();
    });

    it("does not count as enabled for the zaaktype", async () => {
      const { fixture } = await setup({
        enabledGlobally: true,
        enabledForZaaktype: false,
      });

      expect(fixture.componentInstance.enabledForZaaktypeValue).toBe(false);
    });

    it("shows the disabled feedback", async () => {
      await setup({ enabledGlobally: true, enabledForZaaktype: false });

      expect(
        screen.getByText("msg.smartdocuments.form.disabled"),
      ).toBeVisible();
    });

    it("hides the tree", async () => {
      await setup({ enabledGlobally: true, enabledForZaaktype: false });

      expect(screen.queryByRole("tree")).not.toBeInTheDocument();
    });

    it("shows the tree and counts as enabled once the user turns the switch on", async () => {
      const { fixture } = await setup({
        enabledGlobally: true,
        enabledForZaaktype: false,
      });

      await user.click(enabledForZaaktypeSwitch());

      expect(enabledForZaaktypeSwitch()).toBeChecked();
      expect(screen.getByRole("tree")).toBeVisible();
      expect(fixture.componentInstance.enabledForZaaktypeValue).toBe(true);
    });

    it("turns the switch on and shows the tree once enabledForZaaktype changes to true", async () => {
      const { fixture } = await setup({
        enabledGlobally: true,
        enabledForZaaktype: false,
      });

      fixture.componentRef.setInput("enabledForZaaktype", true);
      fixture.detectChanges();

      expect(enabledForZaaktypeSwitch()).toBeChecked();
      expect(screen.getByRole("tree")).toBeVisible();
      expect(
        screen.queryByText("msg.smartdocuments.form.disabled"),
      ).not.toBeInTheDocument();
    });
  });

  describe("when enabledGlobally is true and enabledForZaaktype is true", () => {
    it("shows the switch turned on", async () => {
      await setup({ enabledGlobally: true, enabledForZaaktype: true });

      expect(enabledForZaaktypeSwitch()).toBeChecked();
    });

    it("counts as enabled for the zaaktype", async () => {
      const { fixture } = await setup({
        enabledGlobally: true,
        enabledForZaaktype: true,
      });

      expect(fixture.componentInstance.enabledForZaaktypeValue).toBe(true);
    });

    it("hides the disabled feedback", async () => {
      await setup({ enabledGlobally: true, enabledForZaaktype: true });

      expect(
        screen.queryByText("msg.smartdocuments.form.disabled"),
      ).not.toBeInTheDocument();
    });

    it("shows the tree", async () => {
      await setup({ enabledGlobally: true, enabledForZaaktype: true });

      expect(screen.getByRole("tree")).toBeVisible();
    });

    it("turns the switch off and hides the tree once enabledForZaaktype changes to false", async () => {
      const { fixture } = await setup({
        enabledGlobally: true,
        enabledForZaaktype: true,
      });

      fixture.componentRef.setInput("enabledForZaaktype", false);
      fixture.detectChanges();

      expect(enabledForZaaktypeSwitch()).not.toBeChecked();
      expect(screen.queryByRole("tree")).not.toBeInTheDocument();
      expect(fixture.componentInstance.enabledForZaaktypeValue).toBe(false);
    });

    it("marks a template group active when one of its templates is mapped", async () => {
      await setup({ enabledGlobally: true, enabledForZaaktype: true });

      expect(
        within(groupNode("fakeGroupName1")).getByText("actief"),
      ).toBeVisible();
      expect(
        within(groupNode("fakeGroupName2")).getByText("inactief"),
      ).toBeVisible();
    });

    it("shows the templates of a group with their mapping once the group is expanded", async () => {
      await setup({ enabledGlobally: true, enabledForZaaktype: true });

      await toggleGroup("fakeGroupName1");

      expect(
        within(templateNode("fakeTemplateName1")).getByRole("checkbox"),
      ).toBeChecked();
      expect(
        within(templateNode("fakeTemplateName2")).getByRole("checkbox"),
      ).not.toBeChecked();
    });

    it("marks a template group active once one of its templates is mapped", async () => {
      await setup({ enabledGlobally: true, enabledForZaaktype: true });
      await toggleGroup("fakeGroupName2");

      await chooseInformatieobjecttype("fakeTemplateName3", "Type B");

      expect(
        within(groupNode("fakeGroupName2")).getByText("actief"),
      ).toBeVisible();
    });

    it("keeps the mapping of a template when its group is collapsed and expanded again", async () => {
      await setup({ enabledGlobally: true, enabledForZaaktype: true });
      await toggleGroup("fakeGroupName1");
      await chooseInformatieobjecttype("fakeTemplateName2", "Type B");

      await toggleGroup("fakeGroupName1");
      await toggleGroup("fakeGroupName1");

      expect(
        within(templateNode("fakeTemplateName2")).getByRole("checkbox"),
      ).toBeChecked();
    });

    it("stores only the mapped templates for the zaaktype", async () => {
      const { fixture, httpTestingController } = await setup({
        enabledGlobally: true,
        enabledForZaaktype: true,
      });
      await toggleGroup("fakeGroupName1");
      await chooseInformatieobjecttype("fakeTemplateName2", "Type B");

      fixture.componentInstance.saveSmartDocumentsMapping().subscribe();

      const request = httpTestingController.expectOne(TEMPLATES_MAPPING_URL);
      expect(request.request.method).toBe("POST");
      expect(request.request.body).toEqual([
        {
          id: "fakeGroupId1",
          name: "fakeGroupName1",
          groups: [],
          templates: [
            expect.objectContaining({
              id: "fakeTemplateId1",
              informatieObjectTypeUUID: "fakeInformatieobjecttypeUuid1",
            }),
            expect.objectContaining({
              id: "fakeTemplateId2",
              informatieObjectTypeUUID: "fakeInformatieobjecttypeUuid2",
            }),
          ],
        },
      ]);
      request.flush(null);
      await sleep();
      httpTestingController
        .match(TEMPLATES_MAPPING_URL)
        .forEach((refetch) => refetch.flush(templatesMapping));
    });
  });
});
