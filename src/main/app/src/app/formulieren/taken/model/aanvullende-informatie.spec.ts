/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TestBed } from "@angular/core/testing";
import { TranslateService } from "@ngx-translate/core";
import moment from "moment";
import { of } from "rxjs";
import { FoutAfhandelingService } from "src/app/fout-afhandeling/fout-afhandeling.service";
import { fromPartial } from "../../../../test-helpers";
import { InformatieObjectenService } from "../../../informatie-objecten/informatie-objecten.service";
import { KlantenService } from "../../../klanten/klanten.service";
import { MailtemplateService } from "../../../mailtemplate/mailtemplate.service";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { ZakenService } from "../../../zaken/zaken.service";
import { AanvullendeInformatieFormulier } from "./aanvullende-informatie";

describe("AanvullendeInformatieFormulier", () => {
  let formulier: AanvullendeInformatieFormulier;
  let zakenService: { listAfzendersVoorZaak: jest.Mock };
  let mailtemplateService: { findMailtemplate: jest.Mock };
  let informatieObjectenService: {
    listEnkelvoudigInformatieobjecten: jest.Mock;
  };
  let klantenService: { getContactDetailsForPerson: jest.Mock };

  const mockZaak = fromPartial<GeneratedType<"RestZaak">>({
    uuid: "zaak-uuid",
    zaaktype: { opschortingMogelijk: false },
    rechten: { behandelen: true },
  });

  beforeEach(() => {
    zakenService = {
      listAfzendersVoorZaak: jest.fn().mockReturnValue(of([])),
    };
    mailtemplateService = {
      findMailtemplate: jest
        .fn()
        .mockReturnValue(of({ body: "template body", variabelen: [] })),
    };
    informatieObjectenService = {
      listEnkelvoudigInformatieobjecten: jest.fn().mockReturnValue(of([])),
    };
    klantenService = {
      getContactDetailsForPerson: jest.fn().mockReturnValue(of({})),
    };

    TestBed.configureTestingModule({
      providers: [
        { provide: FoutAfhandelingService, useValue: {} },
        { provide: TranslateService, useValue: { instant: jest.fn() } },
        { provide: ZakenService, useValue: zakenService },
        { provide: MailtemplateService, useValue: mailtemplateService },
        {
          provide: InformatieObjectenService,
          useValue: informatieObjectenService,
        },
        { provide: KlantenService, useValue: klantenService },
      ],
    });

    formulier = TestBed.inject(AanvullendeInformatieFormulier);
  });

  describe("requestForm", () => {
    it("should not pre-fill taakFataleDatum when planItem has no fataleDatum", async () => {
      const planItem = fromPartial<GeneratedType<"RESTPlanItem">>({});

      const fields = await formulier.requestForm(mockZaak, planItem);

      const datumField = fields.find((f) => f.key === "taakFataledatum");
      expect(datumField?.control?.value).toBeNull();
    });

    it("should not pre-fill taakFataleDatum when planItem is undefined", async () => {
      const fields = await formulier.requestForm(mockZaak, undefined);

      const datumField = fields.find((f) => f.key === "taakFataledatum");
      expect(datumField?.control?.value).toBeNull();
    });

    it("should pre-fill taakFataleDatum from planItem.fataleDatum", async () => {
      const fataleDatum = "2026-06-01";
      const planItem = fromPartial<GeneratedType<"RESTPlanItem">>({
        fataleDatum,
      });

      const fields = await formulier.requestForm(mockZaak, planItem);

      const datumField = fields.find((f) => f.key === "taakFataledatum");
      expect(moment.isMoment(datumField?.control?.value)).toBe(true);
      expect(
        (datumField?.control?.value as moment.Moment).isSame(
          moment(fataleDatum),
          "day",
        ),
      ).toBe(true);
    });

    it("should pre-fill the email from initiatorIdentificatie when both type and temporaryPersonId are set", async () => {
      klantenService.getContactDetailsForPerson.mockReturnValue(
        of(
          fromPartial<GeneratedType<"RestContactDetails">>({
            emailadres: "test@example.com",
          }),
        ),
      );
      const zaakWithInitiator = fromPartial<GeneratedType<"RestZaak">>({
        ...mockZaak,
        initiatorIdentificatie: {
          type: "BSN",
          temporaryPersonId: "person-123",
        },
      });

      const fields = await formulier.requestForm(zaakWithInitiator);

      expect(klantenService.getContactDetailsForPerson).toHaveBeenCalledWith(
        "person-123",
      );
      const emailField = fields.find((f) => f.key === "emailadres");
      expect(emailField?.control?.value).toBe("test@example.com");
    });

    it("should not call getContactDetailsForPerson when initiatorIdentificatie is absent", async () => {
      await formulier.requestForm(mockZaak);

      expect(klantenService.getContactDetailsForPerson).not.toHaveBeenCalled();
    });

    it("should not set email when getContactDetailsForPerson returns no emailadres", async () => {
      klantenService.getContactDetailsForPerson.mockReturnValue(
        of(fromPartial<GeneratedType<"RestContactDetails">>({})),
      );
      const zaakWithInitiator = fromPartial<GeneratedType<"RestZaak">>({
        ...mockZaak,
        initiatorIdentificatie: {
          type: "BSN",
          temporaryPersonId: "person-123",
        },
      });

      const fields = await formulier.requestForm(zaakWithInitiator);

      const emailField = fields.find((f) => f.key === "emailadres");
      expect(emailField?.control?.value).toBeNull();
    });
  });
});
