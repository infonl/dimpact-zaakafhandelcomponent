/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
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

  const mockAfzender = fromPartial<
    GeneratedType<"RestZaakAfzender"> & { key: string; value: string }
  >({
    mail: "afzender@example.com",
    defaultMail: false,
    replyTo: "reply@example.com",
  });

  const mockDefaultAfzender = fromPartial<
    GeneratedType<"RestZaakAfzender"> & { key: string; value: string }
  >({
    mail: "default@example.com",
    defaultMail: true,
    replyTo: "default-reply@example.com",
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
    describe("field structure", () => {
      it("should return exactly 10 fields for a non-suspendable zaak", async () => {
        const fields = await formulier.requestForm(mockZaak);

        expect(fields.length).toBe(10);
      });

      it("should return 11 fields for a suspendable zaak (adds zaakOpschorten)", async () => {
        const suspendableZaak = fromPartial<GeneratedType<"RestZaak">>({
          uuid: "zaak-uuid",
          zaaktype: { opschortingMogelijk: true },
          rechten: { behandelen: true },
          redenOpschorting: undefined,
          isHeropend: false,
          eerdereOpschorting: false,
        });

        const fields = await formulier.requestForm(suspendableZaak);

        expect(fields.length).toBe(11);
      });

      it("should return fields in the expected order", async () => {
        const fields = await formulier.requestForm(mockZaak);

        expect(fields.map((f) => f.key)).toEqual([
          "taakStuurGegevens.sendMail",
          "taakStuurGegevens.mail",
          "verzender",
          "replyTo",
          "emailadres",
          "body",
          "datumGevraagd",
          "bijlagen",
          "taakFataledatum",
          "messageField",
        ]);
      });

      it("should set sendMail hidden to true with value true", async () => {
        const fields = await formulier.requestForm(mockZaak);

        const field = fields.find(
          (f) => f.key === "taakStuurGegevens.sendMail",
        );
        expect(field?.hidden).toBe(true);
        expect(field?.control?.value).toBe(true);
      });

      it("should set mail hidden to true with value TAAK_AANVULLENDE_INFORMATIE", async () => {
        const fields = await formulier.requestForm(mockZaak);

        const field = fields.find((f) => f.key === "taakStuurGegevens.mail");
        expect(field?.hidden).toBe(true);
        expect(field?.control?.value).toBe("TAAK_AANVULLENDE_INFORMATIE");
      });

      it("should set replyTo as hidden", async () => {
        const fields = await formulier.requestForm(mockZaak);

        const field = fields.find((f) => f.key === "replyTo");
        expect(field?.hidden).toBe(true);
      });

      it("should set datumGevraagd as hidden with a moment value", async () => {
        const fields = await formulier.requestForm(mockZaak);

        const field = fields.find((f) => f.key === "datumGevraagd");
        expect(field?.hidden).toBe(true);
        expect(moment.isMoment(field?.control?.value)).toBe(true);
      });

      it("should set body control value to mail template body", async () => {
        const fields = await formulier.requestForm(mockZaak);

        const field = fields.find((f) => f.key === "body");
        expect(field?.control?.value).toBe("template body");
      });

      it("should call findMailtemplate with TAAK_AANVULLENDE_INFORMATIE and zaak uuid", async () => {
        await formulier.requestForm(mockZaak);

        expect(mailtemplateService.findMailtemplate).toHaveBeenCalledWith(
          "TAAK_AANVULLENDE_INFORMATIE",
          "zaak-uuid",
        );
      });

      it("should call listEnkelvoudigInformatieobjecten with zaak uuid for bijlagen", async () => {
        await formulier.requestForm(mockZaak);

        expect(
          informatieObjectenService.listEnkelvoudigInformatieobjecten,
        ).toHaveBeenCalledWith({ zaakUUID: "zaak-uuid" });
      });

      it("should use empty array for body variables when mailtemplate variabelen is null", async () => {
        mailtemplateService.findMailtemplate.mockReturnValue(
          of({ body: "template body", variabelen: null }),
        );

        const fields = await formulier.requestForm(mockZaak);

        const field = fields.find((f) => f.key === "body");
        expect("variables" in field! ? field.variables : undefined).toEqual([]);
      });
    });

    describe("verzender", () => {
      it("should set verzender options from listAfzendersVoorZaak", async () => {
        zakenService.listAfzendersVoorZaak.mockReturnValue(
          of([mockAfzender, mockDefaultAfzender]),
        );

        const fields = await formulier.requestForm(mockZaak);

        const field = fields.find((f) => f.key === "verzender");
        expect(
          ("options" in field! ? (field.options as unknown[]) : []).length,
        ).toBe(2);
      });

      it("should pre-select the default afzender", async () => {
        zakenService.listAfzendersVoorZaak.mockReturnValue(
          of([mockAfzender, mockDefaultAfzender]),
        );

        const fields = await formulier.requestForm(mockZaak);

        const field = fields.find((f) => f.key === "verzender");
        expect((field?.control?.value as { mail: string })?.mail).toBe(
          "default@example.com",
        );
      });

      it("should set verzender to null when no default afzender exists", async () => {
        zakenService.listAfzendersVoorZaak.mockReturnValue(of([mockAfzender]));

        const fields = await formulier.requestForm(mockZaak);

        const field = fields.find((f) => f.key === "verzender");
        expect(field?.control?.value).toBeNull();
      });

      it("should update replyTo when verzender changes", async () => {
        zakenService.listAfzendersVoorZaak.mockReturnValue(
          of([mockAfzender, mockDefaultAfzender]),
        );

        const fields = await formulier.requestForm(mockZaak);

        const verzenderField = fields.find((f) => f.key === "verzender");
        const replyToField = fields.find((f) => f.key === "replyTo");

        verzenderField?.control?.setValue({
          ...mockAfzender,
          key: mockAfzender.mail,
          value: mockAfzender.mail,
        });

        expect(replyToField?.control?.value).toBe("reply@example.com");
      });

      it("should set replyTo to null when verzender is cleared", async () => {
        zakenService.listAfzendersVoorZaak.mockReturnValue(
          of([mockDefaultAfzender]),
        );

        const fields = await formulier.requestForm(mockZaak);

        const verzenderField = fields.find((f) => f.key === "verzender");
        const replyToField = fields.find((f) => f.key === "replyTo");

        verzenderField?.control?.setValue(null);

        expect(replyToField?.control?.value).toBeNull();
      });
    });

    describe("taakFataleDatum", () => {
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
    });

    describe("messageField", () => {
      it("should return leeg message key when zaak has no uiterlijkeEinddatumAfdoening", async () => {
        const fields = await formulier.requestForm(mockZaak);

        const field = fields.find((f) => f.key === "messageField");
        expect(field?.control?.value).toBe(
          "msg.taak.aanvullendeInformatie.fataleDatumZaak.leeg",
        );
      });

      it("should return overig.opgeschort message key when zaak has fataleDatum but no taak fataleDatum and zaak is not suspendable", async () => {
        const zaakWithFatalDatum = fromPartial<GeneratedType<"RestZaak">>({
          ...mockZaak,
          uiterlijkeEinddatumAfdoening: "2026-12-31",
          zaaktype: { opschortingMogelijk: false },
        });

        const fields = await formulier.requestForm(zaakWithFatalDatum);

        const field = fields.find((f) => f.key === "messageField");
        expect(field?.control?.value).toBe(
          "msg.taak.aanvullendeInformatie.fataleDatumTaak.overig.opgeschort",
        );
      });

      it("should return overig message key when zaak has fataleDatum but no taak fataleDatum and zaak is suspendable", async () => {
        const zaakWithFatalDatum = fromPartial<GeneratedType<"RestZaak">>({
          ...mockZaak,
          uiterlijkeEinddatumAfdoening: "2026-12-31",
          zaaktype: { opschortingMogelijk: true },
          rechten: { behandelen: true },
        });

        const fields = await formulier.requestForm(zaakWithFatalDatum);

        const field = fields.find((f) => f.key === "messageField");
        expect(field?.control?.value).toBe(
          "msg.taak.aanvullendeInformatie.fataleDatumTaak.overig",
        );
      });

      it("should update messageField when taakFataleDatum changes to a date after zaak fataleDatum", async () => {
        const zaakWithFatalDatum = fromPartial<GeneratedType<"RestZaak">>({
          ...mockZaak,
          uiterlijkeEinddatumAfdoening: "2026-06-01",
          zaaktype: { opschortingMogelijk: false },
        });

        const fields = await formulier.requestForm(zaakWithFatalDatum);

        const taakFataledatumField = fields.find(
          (f) => f.key === "taakFataledatum",
        );
        const messageField = fields.find((f) => f.key === "messageField");

        taakFataledatumField?.control?.setValue(moment("2026-12-31"));

        expect(messageField?.control?.value).toBe(
          "msg.taak.aanvullendeInformatie.fataleDatumTaak.overschreden.opgeschort",
        );
      });

      it("should update messageField to overig when taakFataleDatum changes to a date before zaak fataleDatum", async () => {
        const zaakWithFatalDatum = fromPartial<GeneratedType<"RestZaak">>({
          ...mockZaak,
          uiterlijkeEinddatumAfdoening: "2026-12-31",
          zaaktype: { opschortingMogelijk: false },
        });

        const fields = await formulier.requestForm(zaakWithFatalDatum);

        const taakFataledatumField = fields.find(
          (f) => f.key === "taakFataledatum",
        );
        const messageField = fields.find((f) => f.key === "messageField");

        taakFataledatumField?.control?.setValue(moment("2026-06-01"));

        expect(messageField?.control?.value).toBe(
          "msg.taak.aanvullendeInformatie.fataleDatumTaak.overig.opgeschort",
        );
      });
    });

    describe("emailadres", () => {
      it("should pre-fill the email from zaakSpecificContactDetails when present", async () => {
        const zaakWithEmail = fromPartial<GeneratedType<"RestZaak">>({
          ...mockZaak,
          zaakSpecificContactDetails: { emailAddress: "specific@example.com" },
        });

        const fields = await formulier.requestForm(zaakWithEmail);

        const emailField = fields.find((f) => f.key === "emailadres");
        expect(emailField?.control?.value).toBe("specific@example.com");
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

        expect(
          klantenService.getContactDetailsForPerson,
        ).not.toHaveBeenCalled();
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

    describe("zaakOpschorten", () => {
      const suspendableZaak = fromPartial<GeneratedType<"RestZaak">>({
        uuid: "zaak-uuid",
        zaaktype: { opschortingMogelijk: true },
        rechten: { behandelen: true },
        redenOpschorting: undefined,
        isHeropend: false,
        eerdereOpschorting: false,
      });

      it("should not include zaakOpschorten field when zaak is not suspendable", async () => {
        const fields = await formulier.requestForm(mockZaak);

        expect(fields.find((f) => f.key === "zaakOpschorten")).toBeUndefined();
      });

      it("should not include zaakOpschorten when zaak has redenOpschorting", async () => {
        const zaak = fromPartial<GeneratedType<"RestZaak">>({
          ...suspendableZaak,
          redenOpschorting: "already suspended",
        });

        const fields = await formulier.requestForm(zaak);

        expect(fields.find((f) => f.key === "zaakOpschorten")).toBeUndefined();
      });

      it("should not include zaakOpschorten when zaak isHeropend", async () => {
        const zaak = fromPartial<GeneratedType<"RestZaak">>({
          ...suspendableZaak,
          isHeropend: true,
        });

        const fields = await formulier.requestForm(zaak);

        expect(fields.find((f) => f.key === "zaakOpschorten")).toBeUndefined();
      });

      it("should not include zaakOpschorten when zaak rechten.behandelen is false", async () => {
        const zaak = fromPartial<GeneratedType<"RestZaak">>({
          ...suspendableZaak,
          rechten: { behandelen: false },
        });

        const fields = await formulier.requestForm(zaak);

        expect(fields.find((f) => f.key === "zaakOpschorten")).toBeUndefined();
      });

      it("should not include zaakOpschorten when zaak has eerdereOpschorting", async () => {
        const zaak = fromPartial<GeneratedType<"RestZaak">>({
          ...suspendableZaak,
          eerdereOpschorting: true,
        });

        const fields = await formulier.requestForm(zaak);

        expect(fields.find((f) => f.key === "zaakOpschorten")).toBeUndefined();
      });

      it("should include zaakOpschorten field when zaak is suspendable", async () => {
        const fields = await formulier.requestForm(suspendableZaak);

        expect(fields.find((f) => f.key === "zaakOpschorten")).toBeDefined();
      });

      it("should initialize zaakOpschorten as false", async () => {
        const fields = await formulier.requestForm(suspendableZaak);

        const field = fields.find((f) => f.key === "zaakOpschorten");
        expect(field?.control?.value).toBe(false);
      });

      it("should add required validator to taakFataleDatum when zaakOpschorten is checked", async () => {
        const fields = await formulier.requestForm(suspendableZaak);

        const zaakOpschortenField = fields.find(
          (f) => f.key === "zaakOpschorten",
        );
        const taakFataledatumField = fields.find(
          (f) => f.key === "taakFataledatum",
        );

        zaakOpschortenField?.control?.setValue(true);

        expect(taakFataledatumField?.control?.hasValidator).toBeDefined();
        expect(
          taakFataledatumField?.control?.errors?.["required"],
        ).toBeDefined();
      });

      it("should remove required validator from taakFataleDatum when zaakOpschorten is unchecked", async () => {
        const fields = await formulier.requestForm(suspendableZaak);

        const zaakOpschortenField = fields.find(
          (f) => f.key === "zaakOpschorten",
        );
        const taakFataledatumField = fields.find(
          (f) => f.key === "taakFataledatum",
        );

        zaakOpschortenField?.control?.setValue(true);
        zaakOpschortenField?.control?.setValue(false);

        expect(
          taakFataledatumField?.control?.errors?.["required"],
        ).toBeUndefined();
      });
    });
  });

  describe("handleForm", () => {
    const mockTaak = fromPartial<GeneratedType<"RestTask">>({
      status: "TOEGEKEND",
      rechten: { wijzigen: true },
      taakdata: {},
    });

    describe("field structure", () => {
      it("should return exactly 6 base fields", async () => {
        const fields = await formulier.handleForm(mockTaak, mockZaak);

        expect(fields.length).toBe(6);
      });

      it("should return fields in the expected order", async () => {
        const fields = await formulier.handleForm(mockTaak, mockZaak);

        expect(fields.map((f) => f.key)).toEqual([
          "verzender",
          "emailadres",
          "body",
          "datumGevraagd",
          "datumGeleverd",
          "aanvullendeInformatie",
        ]);
      });

      it("should render verzender, emailadres and body as plain-text", async () => {
        const fields = await formulier.handleForm(mockTaak, mockZaak);

        expect(fields.find((f) => f.key === "verzender")?.type).toBe(
          "plain-text",
        );
        expect(fields.find((f) => f.key === "emailadres")?.type).toBe(
          "plain-text",
        );
        expect(fields.find((f) => f.key === "body")?.type).toBe("plain-text");
      });

      it("should render datumGevraagd as readonly date", async () => {
        const fields = await formulier.handleForm(mockTaak, mockZaak);

        const field = fields.find((f) => f.key === "datumGevraagd");
        expect(field?.type).toBe("date");
        expect(field?.readonly).toBe(true);
        expect(field?.control?.disabled).toBe(true);
      });

      it("should pre-fill datumGevraagd from taakdata", async () => {
        const taakWithData = fromPartial<GeneratedType<"RestTask">>({
          ...mockTaak,
          taakdata: { datumGevraagd: "2026-01-15" },
        });

        const fields = await formulier.handleForm(taakWithData, mockZaak);

        const field = fields.find((f) => f.key === "datumGevraagd");
        expect(field?.control?.value).toBe("2026-01-15");
      });

      it("should pre-fill datumGeleverd from taakdata", async () => {
        const taakWithData = fromPartial<GeneratedType<"RestTask">>({
          ...mockTaak,
          taakdata: { datumGeleverd: "2026-02-01" },
        });

        const fields = await formulier.handleForm(taakWithData, mockZaak);

        const field = fields.find((f) => f.key === "datumGeleverd");
        expect(field?.control?.value).toBe("2026-02-01");
      });

      it("should render aanvullendeInformatie as required radio with the 3 expected options", async () => {
        const fields = await formulier.handleForm(mockTaak, mockZaak);

        const field = fields.find((f) => f.key === "aanvullendeInformatie");
        expect(field?.type).toBe("radio");
        expect("options" in field! ? field.options : []).toEqual([
          "aanvullende-informatie.geleverd-akkoord",
          "aanvullende-informatie.geleverd-niet-akkoord",
          "aanvullende-informatie.niet-geleverd",
        ]);
        field?.control?.setValue(null);
        field?.control?.markAsTouched();
        expect(field?.control?.errors?.["required"]).toBeDefined();
      });

      it("should handle undefined taakdata gracefully", async () => {
        const taakWithoutData = fromPartial<GeneratedType<"RestTask">>({
          ...mockTaak,
          taakdata: undefined,
        });

        const fields = await formulier.handleForm(taakWithoutData, mockZaak);

        const datumGevraagd = fields.find((f) => f.key === "datumGevraagd");
        const datumGeleverd = fields.find((f) => f.key === "datumGeleverd");
        const aanvullendeInformatie = fields.find(
          (f) => f.key === "aanvullendeInformatie",
        );
        expect(datumGevraagd?.control?.value).toBeFalsy();
        expect(datumGeleverd?.control?.value).toBeFalsy();
        expect(aanvullendeInformatie?.control?.value).toBeFalsy();
      });

      it("should pre-fill aanvullendeInformatie from taakdata", async () => {
        const taakWithData = fromPartial<GeneratedType<"RestTask">>({
          ...mockTaak,
          taakdata: {
            aanvullendeInformatie: "aanvullende-informatie.geleverd-akkoord",
          },
        });

        const fields = await formulier.handleForm(taakWithData, mockZaak);

        const field = fields.find((f) => f.key === "aanvullendeInformatie");
        expect(field?.control?.value).toBe(
          "aanvullende-informatie.geleverd-akkoord",
        );
      });
    });

    describe("zaakHervatten", () => {
      it("should not include zaakHervatten when zaak is not opgeschort", async () => {
        const fields = await formulier.handleForm(mockTaak, mockZaak);

        expect(fields.find((f) => f.key === "zaakHervatten")).toBeUndefined();
      });

      it("should include zaakHervatten when zaak is opgeschort and taak can be changed", async () => {
        const opgeschortZaak = fromPartial<GeneratedType<"RestZaak">>({
          ...mockZaak,
          isOpgeschort: true,
          rechten: { behandelen: true },
        });

        const fields = await formulier.handleForm(mockTaak, opgeschortZaak);

        expect(fields.find((f) => f.key === "zaakHervatten")).toBeDefined();
      });

      it("should pre-fill zaakHervatten from taakdata", async () => {
        const opgeschortZaak = fromPartial<GeneratedType<"RestZaak">>({
          ...mockZaak,
          isOpgeschort: true,
          rechten: { behandelen: true },
        });
        const taakWithData = fromPartial<GeneratedType<"RestTask">>({
          ...mockTaak,
          taakdata: { zaakHervatten: "true" },
        });

        const fields = await formulier.handleForm(taakWithData, opgeschortZaak);

        const field = fields.find((f) => f.key === "zaakHervatten");
        expect((field?.control?.value as { value: string })?.value).toBe(
          "true",
        );
      });

      it("should show zaakHervatten on AFGEROND taak when taakdata zaakHervatten is true", async () => {
        const afgerondTaak = fromPartial<GeneratedType<"RestTask">>({
          status: "AFGEROND",
          rechten: { wijzigen: true },
          taakdata: { zaakHervatten: "true" },
        });

        const fields = await formulier.handleForm(afgerondTaak, mockZaak);

        expect(fields.find((f) => f.key === "zaakHervatten")).toBeDefined();
      });

      it("should set zaakHervatten control to null when taakdata value does not match any option", async () => {
        const opgeschortZaak = fromPartial<GeneratedType<"RestZaak">>({
          ...mockZaak,
          isOpgeschort: true,
          rechten: { behandelen: true },
        });
        const taakWithUnknownValue = fromPartial<GeneratedType<"RestTask">>({
          ...mockTaak,
          taakdata: { zaakHervatten: "unknown-value" },
        });

        const fields = await formulier.handleForm(
          taakWithUnknownValue,
          opgeschortZaak,
        );

        const field = fields.find((f) => f.key === "zaakHervatten");
        expect(field?.control?.value).toBeNull();
      });

      it("should show zaakHervatten when taak rechten.wijzigen is false and zaakHervatten taakdata is true", async () => {
        const taakZonderWijzigen = fromPartial<GeneratedType<"RestTask">>({
          status: "TOEGEKEND",
          rechten: { wijzigen: false },
          taakdata: { zaakHervatten: "true" },
        });

        const fields = await formulier.handleForm(taakZonderWijzigen, mockZaak);

        expect(fields.find((f) => f.key === "zaakHervatten")).toBeDefined();
      });

      it("should not show zaakHervatten when taak rechten.wijzigen is false and zaakHervatten taakdata is not true", async () => {
        const taakZonderWijzigen = fromPartial<GeneratedType<"RestTask">>({
          status: "TOEGEKEND",
          rechten: { wijzigen: false },
          taakdata: { zaakHervatten: "false" },
        });

        const fields = await formulier.handleForm(taakZonderWijzigen, mockZaak);

        expect(fields.find((f) => f.key === "zaakHervatten")).toBeUndefined();
      });

      it("should not show zaakHervatten on AFGEROND taak when taakdata zaakHervatten is not true", async () => {
        const afgerondTaak = fromPartial<GeneratedType<"RestTask">>({
          status: "AFGEROND",
          rechten: { wijzigen: true },
          taakdata: { zaakHervatten: "false" },
        });

        const fields = await formulier.handleForm(afgerondTaak, mockZaak);

        expect(fields.find((f) => f.key === "zaakHervatten")).toBeUndefined();
      });
    });
  });
});
