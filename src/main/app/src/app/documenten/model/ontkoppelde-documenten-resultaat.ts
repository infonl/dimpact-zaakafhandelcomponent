/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Resultaat } from "../../shared/model/resultaat";
import { OntkoppeldDocument } from "./ontkoppeld-document";
import {GeneratedType} from "../../shared/utils/generated-types";

export class OntkoppeldeDocumentenResultaat extends Resultaat<OntkoppeldDocument> {
  filterOntkoppeldDoor: GeneratedType<"RestUser">[];
}
