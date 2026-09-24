/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { render, screen } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { fromPartial } from "src/test-helpers";
import { GeneratedType } from "../../../../shared/utils/generated-types";
import { SmartDocumentsFormItemComponent } from "./smart-documents-form-item.component";

const informationObjectTypes: GeneratedType<"RestInformatieobjecttype">[] = [
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

describe(SmartDocumentsFormItemComponent.name, () => {
  const user = userEvent.setup();

  async function setup(informatieObjectTypeUUID: string) {
    const node = fromPartial<GeneratedType<"RestMappedSmartDocumentsTemplate">>(
      {
        id: "fakeTemplateId",
        name: "fakeTemplateName",
        informatieObjectTypeUUID,
      },
    );
    const selectionChange = jest.fn();

    const rendered = await render(SmartDocumentsFormItemComponent, {
      imports: [TranslateModule.forRoot(), NoopAnimationsModule],
      inputs: { node, informationObjectTypes },
      on: { selectionChange },
    });
    await rendered.fixture.whenStable();
    rendered.fixture.detectChanges();

    return { ...rendered, node, selectionChange };
  }

  function mappedCheckbox() {
    return screen.getByRole("checkbox");
  }

  function informatieobjecttypeSelect() {
    return screen.getByRole("combobox", {
      name: /informatieobjectTypeOmschrijving/,
    });
  }

  function vertrouwelijkheidaanduiding() {
    return screen.getByLabelText("vertrouwelijkheidaanduiding");
  }

  async function chooseInformatieobjecttype(omschrijving: string) {
    await user.click(informatieobjecttypeSelect());
    await user.click(screen.getByRole("option", { name: omschrijving }));
  }

  describe("given a template without an informatieobjecttype", () => {
    it("shows the name of the template", async () => {
      await setup("");

      expect(screen.getByText("fakeTemplateName")).toBeVisible();
    });

    it("shows the template as not mapped, without a way to clear it", async () => {
      await setup("");

      expect(mappedCheckbox()).not.toBeChecked();
      expect(mappedCheckbox()).toBeDisabled();
    });

    it("shows no vertrouwelijkheidaanduiding", async () => {
      await setup("");

      expect(vertrouwelijkheidaanduiding()).toHaveValue("");
      expect(vertrouwelijkheidaanduiding()).toBeDisabled();
    });

    it("offers an empty choice and every informatieobjecttype", async () => {
      await setup("");

      await user.click(informatieobjecttypeSelect());

      expect(
        screen.getAllByRole("option").map((option) => option.textContent),
      ).toEqual(["informatieobjectType.-geen-", "Type A", "Type B"]);
    });

    it("does not announce a selection change when it is first shown", async () => {
      const { selectionChange } = await setup("");

      expect(selectionChange).not.toHaveBeenCalled();
    });

    describe("when an informatieobjecttype is chosen", () => {
      it("announces the template mapped to that informatieobjecttype", async () => {
        const { selectionChange } = await setup("");

        await chooseInformatieobjecttype("Type B");

        expect(selectionChange).toHaveBeenCalledTimes(1);
        expect(selectionChange).toHaveBeenCalledWith({
          id: "fakeTemplateId",
          name: "fakeTemplateName",
          informatieObjectTypeUUID: "fakeInformatieobjecttypeUuid2",
        });
      });

      it("shows the template as mapped, with a way to clear it", async () => {
        await setup("");

        await chooseInformatieobjecttype("Type B");

        expect(mappedCheckbox()).toBeChecked();
        expect(mappedCheckbox()).toBeEnabled();
      });

      it("shows the vertrouwelijkheidaanduiding of that informatieobjecttype", async () => {
        await setup("");

        await chooseInformatieobjecttype("Type B");

        expect(vertrouwelijkheidaanduiding()).toHaveValue(
          "vertrouwelijkheidaanduiding.VERTROUWELIJK",
        );
      });

      it("writes the choice into the template it was given, so that a re-rendered tree node keeps it", async () => {
        const { node } = await setup("");

        await chooseInformatieobjecttype("Type B");

        expect(node.informatieObjectTypeUUID).toBe(
          "fakeInformatieobjecttypeUuid2",
        );
      });
    });
  });

  describe("given a template mapped to an informatieobjecttype", () => {
    it("shows the chosen informatieobjecttype", async () => {
      await setup("fakeInformatieobjecttypeUuid1");

      expect(informatieobjecttypeSelect()).toHaveTextContent("Type A");
    });

    it("shows the template as mapped, with a way to clear it", async () => {
      await setup("fakeInformatieobjecttypeUuid1");

      expect(mappedCheckbox()).toBeChecked();
      expect(mappedCheckbox()).toBeEnabled();
    });

    it("shows the vertrouwelijkheidaanduiding of that informatieobjecttype", async () => {
      await setup("fakeInformatieobjecttypeUuid1");

      expect(vertrouwelijkheidaanduiding()).toHaveValue(
        "vertrouwelijkheidaanduiding.OPENBAAR",
      );
    });

    it("does not announce a selection change when it is first shown", async () => {
      const { selectionChange } = await setup("fakeInformatieobjecttypeUuid1");

      expect(selectionChange).not.toHaveBeenCalled();
    });

    describe("when the checkbox is unchecked", () => {
      it("announces the template without an informatieobjecttype", async () => {
        const { selectionChange } = await setup(
          "fakeInformatieobjecttypeUuid1",
        );

        await user.click(mappedCheckbox());

        expect(selectionChange).toHaveBeenCalledTimes(1);
        expect(selectionChange).toHaveBeenCalledWith({
          id: "fakeTemplateId",
          name: "fakeTemplateName",
          informatieObjectTypeUUID: "",
        });
      });

      it("shows the template as not mapped, without a way to clear it", async () => {
        await setup("fakeInformatieobjecttypeUuid1");

        await user.click(mappedCheckbox());

        expect(mappedCheckbox()).not.toBeChecked();
        expect(mappedCheckbox()).toBeDisabled();
      });

      it("clears the vertrouwelijkheidaanduiding and the chosen informatieobjecttype", async () => {
        await setup("fakeInformatieobjecttypeUuid1");

        await user.click(mappedCheckbox());

        expect(vertrouwelijkheidaanduiding()).toHaveValue("");
        expect(informatieobjecttypeSelect()).not.toHaveTextContent("Type A");
      });

      it("clears the informatieobjecttype of the template it was given", async () => {
        const { node } = await setup("fakeInformatieobjecttypeUuid1");

        await user.click(mappedCheckbox());

        expect(node.informatieObjectTypeUUID).toBe("");
      });
    });

    describe("when the empty choice is chosen", () => {
      it("announces the template without an informatieobjecttype", async () => {
        const { selectionChange } = await setup(
          "fakeInformatieobjecttypeUuid1",
        );

        await chooseInformatieobjecttype("informatieobjectType.-geen-");

        expect(selectionChange).toHaveBeenCalledTimes(1);
        expect(selectionChange).toHaveBeenCalledWith({
          id: "fakeTemplateId",
          name: "fakeTemplateName",
          informatieObjectTypeUUID: undefined,
        });
      });

      it("shows the template as not mapped, without a way to clear it", async () => {
        await setup("fakeInformatieobjecttypeUuid1");

        await chooseInformatieobjecttype("informatieobjectType.-geen-");

        expect(mappedCheckbox()).not.toBeChecked();
        expect(mappedCheckbox()).toBeDisabled();
        expect(informatieobjecttypeSelect()).toHaveTextContent(
          "informatieobjectType.-kies-",
        );
      });
    });

    describe("when another informatieobjecttype is chosen", () => {
      it("announces the template mapped to the other informatieobjecttype", async () => {
        const { selectionChange } = await setup(
          "fakeInformatieobjecttypeUuid1",
        );

        await chooseInformatieobjecttype("Type B");

        expect(selectionChange).toHaveBeenCalledTimes(1);
        expect(selectionChange).toHaveBeenCalledWith(
          expect.objectContaining({
            informatieObjectTypeUUID: "fakeInformatieobjecttypeUuid2",
          }),
        );
      });
    });
  });
});
