/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { GeneratedType } from "../../shared/utils/generated-types";

export class GeometryGegevens {
  constructor(
    public geometry: GeneratedType<"RestGeometry">,
    public reden: string,
  ) {}
}
