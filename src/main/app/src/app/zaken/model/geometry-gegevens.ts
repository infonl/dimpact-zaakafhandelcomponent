/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Api } from "../../shared/utils/generated-types";

export class GeometryGegevens {
  constructor(
    public geometry: Api<"RestGeometry">,
    public reden: string,
  ) {}
}
