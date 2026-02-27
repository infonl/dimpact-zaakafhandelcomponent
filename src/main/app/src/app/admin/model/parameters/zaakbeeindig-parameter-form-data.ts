/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { GeneratedType } from "../../../shared/utils/generated-types";

/**
 * Form data type for zaakbeeindig parameters during editing.
 * - zaakbeeindigReden is optional for the "zaak niet ontvankelijk" special case
 * - resultaattype is optional until user selects one from dropdown
 */
export type ZaakbeeindigParameterFormData = {
  id?: number | null;
  zaakbeeindigReden?: GeneratedType<"RestZaakbeeindigReden"> | null;
  resultaattype?: GeneratedType<"RestResultaattype"> | null;
};

/**
 * Converts form data to the API type for saving to backend.
 * Only called for regular zaakbeeindig parameters (not zaaknietontvankelijk).
 */
export function toRestZaakbeeindigParameter(
  formData: ZaakbeeindigParameterFormData,
): GeneratedType<"RestZaakbeeindigParameter"> {
  if (!formData.zaakbeeindigReden) {
    throw new Error("zaakbeeindigReden is required");
  }
  if (!formData.resultaattype) {
    throw new Error("resultaattype is required");
  }

  return {
    id: formData.id,
    zaakbeeindigReden: formData.zaakbeeindigReden,
    resultaattype: formData.resultaattype,
  };
}
