import { TestBed } from "@angular/core/testing";
import { TranslateService } from "@ngx-translate/core";
import { TranslateStore } from "@ngx-translate/core";
import { of } from "rxjs";
import { AanvullendeInformatie } from "./aanvullende-informatie";
import { TakenService } from "../../../taken/taken.service";
import { InformatieObjectenService } from "../../../informatie-objecten/informatie-objecten.service";
import { MailtemplateService } from "../../../mailtemplate/mailtemplate.service";
import { KlantenService } from "../../../klanten/klanten.service";
import { ZakenService } from "../../../zaken/zaken.service";

/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

describe("AanvullendeInformatie", () => {
  let aanvullendeInformatie: AanvullendeInformatie;
  let translateService: TranslateService;
  let takenService: TakenService;
  let informatieObjectenService: InformatieObjectenService;
  let mailtemplateService: MailtemplateService;
  let klantenService: KlantenService;
  let zakenService: ZakenService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        TranslateService,
        TranslateStore,
        {
          provide: TakenService,
          useValue: {
            listAfzendersVoorZaak: jest.fn(),
            readDefaultAfzenderVoorZaak: jest.fn(),
          },
        },
        {
          provide: TranslateService,
          useValue: {
            get: jest.fn().mockReturnValue(of("")),
            instant: jest.fn().mockReturnValue(""),
          },
        },
        {
          provide: TakenService,
          useValue: {
            listAfzendersVoorZaak: jest.fn(),
            readDefaultAfzenderVoorZaak: jest.fn(),
          },
        },
        {
          provide: InformatieObjectenService,
          useValue: { listEnkelvoudigInformatieobjecten: jest.fn() },
        },
        {
          provide: MailtemplateService,
          useValue: { findMailtemplate: jest.fn() },
        },
        {
          provide: KlantenService,
          useValue: { ophalenContactGegevens: jest.fn() },
        },
        {
          provide: ZakenService,
          useValue: {
            listAfzendersVoorZaak: jest.fn(),
            readDefaultAfzenderVoorZaak: jest.fn(),
          },
        },
      ],
    });

    translateService = TestBed.inject(TranslateService);
    takenService = TestBed.inject(TakenService);
    informatieObjectenService = TestBed.inject(InformatieObjectenService);
    mailtemplateService = TestBed.inject(MailtemplateService);
    klantenService = TestBed.inject(KlantenService);
    zakenService = TestBed.inject(ZakenService);

    aanvullendeInformatie = new AanvullendeInformatie(
      translateService,
      takenService,
      informatieObjectenService,
      mailtemplateService,
      klantenService,
      zakenService,
    );
  });

  describe("isZaakSuspendable", () => {
    it("should return true if the zaak is suspendable", () => {
      aanvullendeInformatie.zaak = {
        zaaktype: { opschortingMogelijk: true },
        redenOpschorting: null,
        isHeropend: false,
        rechten: { behandelen: true },
        isEerderOpgeschort: false,
      } as any;

      expect(aanvullendeInformatie["isZaakSuspendable"]()).toBe(true);
    });

    it("should return false if the zaak is not suspendable", () => {
      aanvullendeInformatie.zaak = {
        zaaktype: { opschortingMogelijk: false },
        redenOpschorting: null,
        isHeropend: false,
        rechten: { behandelen: true },
        isEerderOpgeschort: false,
      } as any;

      expect(aanvullendeInformatie["isZaakSuspendable"]()).toBe(false);
    });
  });

  describe("toonHervatten", () => {
    it("should return true if zaak is suspended and has behandel rights", () => {
      aanvullendeInformatie.zaak = {
        isOpgeschort: true,
        rechten: { behandelen: true },
      } as any;

      expect(aanvullendeInformatie["toonHervatten"]()).toBe(true);
    });

    it("should return false if zaak is not suspended", () => {
      aanvullendeInformatie.zaak = {
        isOpgeschort: false,
        rechten: { behandelen: true },
      } as any;

      expect(aanvullendeInformatie["toonHervatten"]()).toBe(false);
    });

    it("should return false if zaak does not have behandel rights", () => {
      aanvullendeInformatie.zaak = {
        isOpgeschort: true,
        rechten: { behandelen: false },
      } as any;

      expect(aanvullendeInformatie["toonHervatten"]()).toBe(false);
    });
  });
});
