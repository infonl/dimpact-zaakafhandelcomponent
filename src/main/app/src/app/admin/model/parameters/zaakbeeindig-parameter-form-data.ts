/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { GeneratedType } from "../../../shared/utils/generated-types";

export interface ZaakbeeindigParameterFormData {
  id?: number | null;
  zaakbeeindigReden?: GeneratedType<"RestZaakbeeindigReden"> | null;
  resultaattype?: GeneratedType<"RestResultaattype"> | null;
}

/**
 * Converts internal form data to the API type for sending to the backend.
 * Validates that required fields are present.
 *
 * @throws Error if required fields are missing
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

/**
 * Converts API type to internal form data for editing.
 */
export function fromRestZaakbeeindigParameter(
  apiData: GeneratedType<"RestZaakbeeindigParameter">,
): ZaakbeeindigParameterFormData {
  return {
    id: apiData.id,
    zaakbeeindigReden: apiData.zaakbeeindigReden,
    resultaattype: apiData.resultaattype,
  };
}
