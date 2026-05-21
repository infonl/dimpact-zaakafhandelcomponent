/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { fromPartial } from "../../../test-helpers";
import { GeneratedType } from "../../shared/utils/generated-types";
import { mapTaskdataToTaskInformation } from "./taak.utils";

describe("mapTaskdataToTaskInformation", () => {
  it("maps uitkomst to the 'verzonden' taakdata key for DOCUMENT_VERZENDEN_POST", () => {
    const taak = fromPartial<GeneratedType<"RestTask">>({
      formulierDefinitieId: "DOCUMENT_VERZENDEN_POST",
    });

    expect(
      mapTaskdataToTaskInformation(
        {
          verzonden: "doc-uuid-1;doc-uuid-2",
          toelichting: "verzonden per post",
          bijlagen: "doc-uuid-3",
        },
        taak,
      ),
    ).toEqual({
      uitkomst: "doc-uuid-1;doc-uuid-2",
      bijlagen: "doc-uuid-3",
      opmerking: "verzonden per post",
    });
  });
});
