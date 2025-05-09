/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Pipe, PipeTransform } from "@angular/core";
import { LocationUtil } from "../location/location-util";
import { GeneratedType } from "../utils/generated-types";

@Pipe({
  name: "location",
})
export class LocationPipe implements PipeTransform {
  constructor() {}

  transform(value: GeneratedType<"RestGeometry"> | string | null) {
    if (!value) return null;

    return LocationUtil.format(
      typeof value == "string" ? LocationUtil.wktToPoint(value) : value,
    );
  }
}
