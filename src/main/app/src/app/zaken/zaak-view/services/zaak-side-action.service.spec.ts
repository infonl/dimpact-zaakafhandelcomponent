/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { MatSidenav } from "@angular/material/sidenav";
import { fromPartial } from "../../../../test-helpers";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { ZaakSideActionService } from "./zaak-side-action.service";

describe(ZaakSideActionService.name, () => {
  let service: ZaakSideActionService;
  let sidenav: { open: jest.Mock; close: jest.Mock };

  beforeEach(() => {
    service = new ZaakSideActionService();
    sidenav = { open: jest.fn(), close: jest.fn() };
    service.register(fromPartial<MatSidenav>(sidenav));
  });

  describe("before a sidenav is registered", () => {
    let consoleError: jest.SpyInstance;

    beforeEach(() => {
      consoleError = jest.spyOn(console, "error").mockImplementation(() => {});
    });

    afterEach(() => {
      consoleError.mockRestore();
    });

    it("still records the active action instead of throwing", () => {
      const unregistered = new ZaakSideActionService();

      unregistered.open("actie.zaak.wijzigen");

      expect(unregistered.activeAction()).toBe("actie.zaak.wijzigen");
    });

    it("reports opening without a sidenav instead of silently doing nothing", () => {
      const unregistered = new ZaakSideActionService();

      unregistered.open("actie.zaak.wijzigen");

      expect(consoleError).toHaveBeenCalledWith(
        "Cannot open the zaak side action panel: no sidenav registered",
      );
    });

    it("reports closing without a sidenav instead of silently doing nothing", () => {
      const unregistered = new ZaakSideActionService();

      unregistered.close();

      expect(consoleError).toHaveBeenCalledWith(
        "Cannot close the zaak side action panel: no sidenav registered",
      );
    });
  });

  describe("open", () => {
    it("switches to the requested panel and opens the sidenav", () => {
      service.open("actie.mail.versturen");

      expect(service.activeAction()).toBe("actie.mail.versturen");
      expect(sidenav.open).toHaveBeenCalled();
    });

    it("leaves the active panel alone when called without one, because the side nav picks it", () => {
      service.open("actie.mail.versturen");
      sidenav.open.mockClear();

      service.open();

      expect(service.activeAction()).toBe("actie.mail.versturen");
      expect(sidenav.open).toHaveBeenCalled();
    });
  });

  describe("close", () => {
    it("closes the sidenav but remembers which panel was showing", () => {
      service.open("actie.mail.versturen");

      service.close();

      expect(sidenav.close).toHaveBeenCalled();
      expect(service.activeAction()).toBe("actie.mail.versturen");
    });
  });

  describe("clear", () => {
    it("forgets the active panel without touching the sidenav", () => {
      service.open("actie.mail.versturen");
      sidenav.close.mockClear();

      service.clear();

      expect(service.activeAction()).toBeNull();
      expect(sidenav.close).not.toHaveBeenCalled();
    });
  });

  describe("reset", () => {
    it("closes the sidenav and forgets both the panel and the plan item", () => {
      service.open("Advies");
      service.actiefPlanItem.set(
        fromPartial<GeneratedType<"RESTPlanItem">>({ id: "fakePlanItemId" }),
      );

      service.reset();

      expect(service.activeAction()).toBeNull();
      expect(service.actiefPlanItem()).toBeNull();
      expect(sidenav.close).toHaveBeenCalled();
    });
  });
});
