/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  KNOWN_ZAC_FIELDS,
  ZAC_FIELD_ATTRIBUTE,
} from "../../taken/taak-view/formio/formio-setup-service";

/**
 * ZAC has no Form.io component types of its own: its fields are plain components carrying a
 * `ZAC_TYPE` attribute, which the stock builder can already produce by hand — at the cost of an
 * unusable field on a single typo.
 */
const zacSelectSchema = (zacFieldType: KNOWN_ZAC_FIELDS, label: string) => ({
  label,
  type: "select",
  input: true,
  widget: "html5",
  dataSrc: "custom",
  clearOnRefresh: true,
  attributes: { [ZAC_FIELD_ATTRIBUTE]: zacFieldType },
});

export const ZAC_BUILDER_PALETTE = {
  zac: {
    title: "ZAC",
    weight: 0,
    default: true,
    components: {
      zacGroep: {
        title: "Groep",
        key: "groep",
        icon: "people",
        schema: zacSelectSchema(KNOWN_ZAC_FIELDS.GROEP, "Groep"),
      },
      zacMedewerker: {
        title: "Medewerker",
        key: "medewerker",
        icon: "person",
        schema: {
          ...zacSelectSchema(KNOWN_ZAC_FIELDS.MEDEWERKER, "Medewerker"),
          // ZAC lists the employees of the group this field names, so it defaults to the Groep preset
          refreshOn: "groep",
        },
      },
      zacReferentieTabel: {
        title: "Referentietabel",
        key: "referentieTabel",
        icon: "th-list",
        schema: {
          ...zacSelectSchema(
            KNOWN_ZAC_FIELDS.REFERENTIE_TABEL,
            "Referentietabel",
          ),
          properties: { ReferenceTable_Code: "" },
        },
      },
      zacDocumenten: {
        title: "Documenten",
        key: "documenten",
        icon: "files-o",
        schema: {
          label: "Documenten",
          type: "select",
          input: true,
          multiple: true,
          widget: "html5",
          dataSrc: "custom",
          attributes: { [ZAC_FIELD_ATTRIBUTE]: KNOWN_ZAC_FIELDS.DOCUMENTEN },
        },
      },
      zacDocumentenNietOndertekend: {
        title: "Te ondertekenen documenten",
        key: "documentenNietOndertekend",
        icon: "table",
        schema: {
          label: "Documenten",
          type: "datagrid",
          input: true,
          disableAddingRemovingRows: true,
          attributes: {
            [ZAC_FIELD_ATTRIBUTE]: KNOWN_ZAC_FIELDS.DOCUMENTEN_NIET_ONDERTEKEND,
          },
          components: [
            { label: "", key: "selected", type: "checkbox", input: true },
            {
              label: "Titel",
              key: "titel",
              type: "textfield",
              input: true,
              disabled: true,
            },
            {
              key: "openen",
              type: "htmlelement",
              input: false,
              attributes: {
                [ZAC_FIELD_ATTRIBUTE]: KNOWN_ZAC_FIELDS.REGEL_LINK_VIEW_ICON,
              },
            },
          ],
        },
      },
    },
  },
  // Form.io merges its own groups in; naming one here replaces it, so only the unwanted one is named.
  premium: false,
};
