/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Resultaat } from "../../shared/model/resultaat";
import { GeneratedType } from "../../shared/utils/generated-types";
import { OntkoppeldDocument } from "./ontkoppeld-document";

/**
 * @deprecated - use the `GeneratedType`
 */
export class OntkoppeldeDocumentenResultaat extends Resultaat<OntkoppeldDocument> {
  filterOntkoppeldDoor: GeneratedType<"RestUser">[];
}
