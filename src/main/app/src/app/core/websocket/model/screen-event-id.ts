/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { GeneratedType } from "../../../shared/utils/generated-types";

export class ScreenEventId implements GeneratedType<"ScreenEventId"> {
  resource: string;
  detail: string | null = null;

  constructor(objectId: string) {
    this.resource = objectId;
  }
}
