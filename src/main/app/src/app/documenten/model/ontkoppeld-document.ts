/*
 * SPDX-FileCopyrightText: 2022 Atosm 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {GeneratedType} from "../../shared/utils/generated-types";

export class OntkoppeldDocument {
  id: number;
  documentUUID: string;
  documentID: string;
  zaakID: string;
  creatiedatum: string;
  titel: string;
  bestandsnaam: string;
  ontkoppeldDoor: GeneratedType<'RestUser'>;
  ontkoppeldOp: string;
  reden: string;
}
