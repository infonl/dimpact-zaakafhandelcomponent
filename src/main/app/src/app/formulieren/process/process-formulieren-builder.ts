/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { AbstractProcessFormulier } from "./abstract-process-formulier";

export class ProcessFormulierenBuilder {
  protected readonly _formulier: AbstractProcessFormulier;

  form(): ProcessFormulierenBuilder {
    return this;
  }

  build(): AbstractProcessFormulier {
    return this._formulier;
  }
}
