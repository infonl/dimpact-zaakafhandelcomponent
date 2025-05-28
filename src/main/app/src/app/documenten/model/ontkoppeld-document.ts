/*
 * SPDX-FileCopyrightText: 2022 Atosm 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { GeneratedType } from "../../shared/utils/generated-types";

/**
 * @deprecated - use the `GeneratedType`
 */
export class OntkoppeldDocument {
  id: number;
  documentUUID: string;
  documentID: string;
  zaakID: string;
  creatiedatum: string;
  titel: string;
  bestandsnaam: string;
  ontkoppeldDoor: GeneratedType<"RestUser">;
  ontkoppeldOp: string;
  reden: string;
}
