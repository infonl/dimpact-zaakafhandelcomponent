/*
 * SPDX-FileCopyrightText: 2023 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { FormGroup } from "@angular/forms";
import { GeneratedType } from "../../shared/utils/generated-types";

export abstract class AbstractProcessFormulier {
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  getData(formGroup: FormGroup): GeneratedType<"RESTProcessTaskData"> {
    return null as unknown as GeneratedType<"RESTProcessTaskData">;
  }
}
