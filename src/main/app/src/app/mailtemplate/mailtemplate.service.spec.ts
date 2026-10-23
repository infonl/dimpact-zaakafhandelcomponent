/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TestBed } from "@angular/core/testing";
import { provideTranslateService } from "@ngx-translate/core";
import { testQueryClient } from "../../../setupJest";
import { fromPartial } from "../../test-helpers";
import { GeneratedType } from "../shared/utils/generated-types";
import { MailtemplateService } from "./mailtemplate.service";

describe(MailtemplateService.name, () => {
  let service: MailtemplateService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideTranslateService()],
    });
    service = TestBed.inject(MailtemplateService);
  });

  describe("invalidateFoundMailtemplates", () => {
    it("makes every zaak fetch its mailtemplates again, whatever the mail", async () => {
      const queryKeys = [
        service.findMailtemplate("ZAAK_ALGEMEEN", "zaak-uuid").queryKey,
        service.findMailtemplate("ZAAK_ONTVANKELIJK", "other-zaak-uuid")
          .queryKey,
      ];
      queryKeys.forEach((queryKey) =>
        testQueryClient.setQueryData(
          queryKey,
          fromPartial<GeneratedType<"RestMailtemplate">>({
            body: "cached body",
          }),
        ),
      );

      await service.invalidateFoundMailtemplates();

      queryKeys.forEach((queryKey) =>
        expect(testQueryClient.getQueryState(queryKey)?.isInvalidated).toBe(
          true,
        ),
      );
    });
  });
});
