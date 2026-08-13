/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ExtendedComponentSchema, FormioForm } from "@formio/angular";
import { testQueryClient } from "../../../../../setupJest";
import { UtilService } from "../../../core/service/util.service";
import { FormioSetupService, KNOWN_ZAC_FIELDS } from "./formio-setup-service";
import {
  configureFormioSetupServiceTestBed,
  document1,
  regelLinkColumn,
  regelLinkViewIconColumn,
  selectedUnsignedDocumentsFieldset,
  taak,
  unsignedDocumentsFieldset,
} from "./formio-setup-service.test-fixtures";

describe(FormioSetupService.name, () => {
  let formioSetupService: FormioSetupService;
  let utilService: UtilService;

  beforeEach(() => {
    ({ formioSetupService, utilService } =
      configureFormioSetupServiceTestBed());
  });

  // `testQueryClient` is shared by every test here, and the global `clearAllMocks` leaves
  // spy implementations in place: without this, one test's mocked documents feed the next.
  afterEach(() => jest.restoreAllMocks());

  describe(
    (FormioSetupService.prototype as unknown as Record<string, () => unknown>)[
      "initializeRowLinkColumn"
    ].name,
    () => {
      /** A link column takes its route from the grid it sits in, so it is always tested nested. */
      const initializeLinkInGrid = async (
        grid: ExtendedComponentSchema,
        column: ExtendedComponentSchema,
      ) => {
        jest
          .spyOn(testQueryClient, "fetchQuery")
          .mockResolvedValue([document1]);

        await formioSetupService.createFormioForm(
          { components: [{ ...grid, components: [column] }] } as FormioForm,
          {
            ...taak,
            taakdata: {
              ZAAK_Documenten_Ondertekenen_Selectie: [
                {
                  selected: true,
                  titel: document1.titel,
                  uuid: document1.uuid,
                },
              ],
            },
          },
        );
      };

      it("should link to the document by its own uuid", async () => {
        const column: ExtendedComponentSchema = { ...regelLinkColumn };

        await initializeLinkInGrid(unsignedDocumentsFieldset, column);

        expect(column.attrs).toEqual([
          {
            attr: "href",
            // the row uuid stays a template: only Form.io can resolve it, per row
            value: `/informatie-objecten/{{ row.uuid }}`,
          },
          { attr: "target", value: "_blank" },
          { attr: "rel", value: "noopener noreferrer" },
        ]);
      });

      it("should take the route from the grid holding the column, whichever grid that is", async () => {
        const column: ExtendedComponentSchema = { ...regelLinkColumn };

        await initializeLinkInGrid(selectedUnsignedDocumentsFieldset, column);

        expect(column.attrs).toEqual(
          expect.arrayContaining([
            {
              attr: "href",
              value: `/informatie-objecten/{{ row.uuid }}`,
            },
          ]),
        );
      });

      it("should render an anchor with translated link text", async () => {
        const column: ExtendedComponentSchema = { ...regelLinkColumn };

        await initializeLinkInGrid(unsignedDocumentsFieldset, column);

        expect(column.tag).toBe("a");
        expect(column.content).toBe("actie.document.openen-nieuw-tabblad");
      });

      it("should leave the tag and content defined by the form author untouched", async () => {
        const column: ExtendedComponentSchema = {
          ...regelLinkColumn,
          tag: "button",
          content: "Set by the form author",
        };

        await initializeLinkInGrid(unsignedDocumentsFieldset, column);

        expect(column.tag).toBe("button");
        expect(column.content).toBe("Set by the form author");
      });

      it("should leave attrs defined by the form author untouched", async () => {
        const authorAttrs = [
          { attr: "href", value: "/somewhere/else/{{ row.uuid }}" },
        ];
        const column: ExtendedComponentSchema = {
          ...regelLinkColumn,
          attrs: authorAttrs,
        };

        await initializeLinkInGrid(unsignedDocumentsFieldset, column);

        expect(column.attrs).toEqual(authorAttrs);
      });

      it("should render the view icon instead of link text, with the text as accessible name", async () => {
        const column: ExtendedComponentSchema = {
          ...regelLinkViewIconColumn,
        };

        await initializeLinkInGrid(unsignedDocumentsFieldset, column);

        expect(column.tag).toBe("a");
        expect(column.content).toBe(
          '<span class="material-symbols-outlined">visibility</span>',
        );
        expect(column.attrs).toEqual([
          {
            attr: "href",
            value: `/informatie-objecten/{{ row.uuid }}`,
          },
          { attr: "target", value: "_blank" },
          { attr: "rel", value: "noopener noreferrer" },
          {
            attr: "aria-label",
            value: "actie.document.openen-nieuw-tabblad",
          },
          { attr: "title", value: "actie.document.openen-nieuw-tabblad" },
        ]);
      });

      it("should leave the icon content defined by the form author untouched", async () => {
        const column: ExtendedComponentSchema = {
          ...regelLinkViewIconColumn,
          content: "Set by the form author",
        };

        await initializeLinkInGrid(unsignedDocumentsFieldset, column);

        expect(column.content).toBe("Set by the form author");
      });

      it("should report an icon link column that sits outside a grid with a registered route", async () => {
        const handleFormIOInitErrorSpy = jest.spyOn(
          utilService,
          "handleFormIOInitError",
        );
        const column: ExtendedComponentSchema = {
          ...regelLinkViewIconColumn,
        };

        await formioSetupService.createFormioForm(
          { components: [column] } as FormioForm,
          taak,
        );

        expect(column.attrs).toBeUndefined();
        expect(handleFormIOInitErrorSpy).toHaveBeenCalledWith(
          KNOWN_ZAC_FIELDS.REGEL_LINK_VIEW_ICON,
          expect.stringContaining(
            `A ${KNOWN_ZAC_FIELDS.REGEL_LINK_VIEW_ICON} column takes its route`,
          ),
        );
      });

      it("should report a link column that sits outside a grid with a registered route", async () => {
        const handleFormIOInitErrorSpy = jest.spyOn(
          utilService,
          "handleFormIOInitError",
        );
        const column: ExtendedComponentSchema = { ...regelLinkColumn };

        await formioSetupService.createFormioForm(
          { components: [column] } as FormioForm,
          taak,
        );

        expect(column.attrs).toBeUndefined();
        expect(handleFormIOInitErrorSpy).toHaveBeenCalledWith(
          KNOWN_ZAC_FIELDS.REGEL_LINK,
          expect.stringContaining(
            'No row link registered for parent "undefined"',
          ),
        );
      });
    },
  );
});
