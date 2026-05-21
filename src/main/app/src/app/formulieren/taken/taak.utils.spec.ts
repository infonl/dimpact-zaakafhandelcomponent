/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { fromPartial } from "../../../test-helpers";
import { GeneratedType } from "../../shared/utils/generated-types";
import { mapTaskdataToTaskInformation } from "./taak.utils";

describe("mapTaskdataToTaskInformation", () => {
  it("maps uitkomst to the 'afhandeling' taakdata key for DEFAULT_TAAKFORMULIER", () => {
    const taak = fromPartial<GeneratedType<"RestTask">>({
      formulierDefinitieId: "DEFAULT_TAAKFORMULIER",
    });

    expect(
      mapTaskdataToTaskInformation(
        {
          afhandeling: "verwerkt door behandelaar",
          toelichting: "extra context",
          bijlagen: "doc-uuid-1",
        },
        taak,
      ),
    ).toEqual({
      uitkomst: "verwerkt door behandelaar",
      bijlagen: "doc-uuid-1",
      opmerking: "extra context",
    });
  });
});
