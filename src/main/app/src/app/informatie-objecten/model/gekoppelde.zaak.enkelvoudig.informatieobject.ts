/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { GeneratedType } from "../../shared/utils/generated-types";

/**
 * @deprecated - use the `GeneratedType`
 */
export type GekoppeldeZaakEnkelvoudigInformatieobject =
  GeneratedType<"RestEnkelvoudigInformatieobject"> & {
    relatieType?: GeneratedType<"RelatieType"> | null;
    zaakIdentificatie?: string | null;
    isZaakspecifiekGeautoriseerd?: boolean;
    zaakUUID?: string | null;
  };
