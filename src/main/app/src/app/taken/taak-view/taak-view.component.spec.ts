/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import {
  provideHttpClient,
  withInterceptorsFromDi,
} from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { ComponentRef } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { FormControl } from "@angular/forms";
import { MatFormFieldHarness } from "@angular/material/form-field/testing";
import { MatInputHarness } from "@angular/material/input/testing";
import { MatSidenav } from "@angular/material/sidenav";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { ActivatedRoute, RouterModule } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { fromPartial } from "@total-typescript/shoehorn";
import { of } from "rxjs";
import { ObjectType } from "../../core/websocket/model/object-type";
import { Opcode } from "../../core/websocket/model/opcode";
import { ScreenEventId } from "../../core/websocket/model/screen-event-id";
import { WebsocketService } from "../../core/websocket/websocket.service";
import { TaakFormulierenService } from "../../formulieren/taken/taak-formulieren.service";
import { EditGroepBehandelaarComponent } from "../../shared/edit/edit-groep-behandelaar/edit-groep-behandelaar.component";
import { MaterialFormBuilderModule } from "../../shared/material-form-builder/material-form-builder.module";
import { MaterialModule } from "../../shared/material/material.module";
import { PipesModule } from "../../shared/pipes/pipes.module";
import { SideNavComponent } from "../../shared/side-nav/side-nav.component";
import { StaticTextComponent } from "../../shared/static-text/static-text.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZaakVerkortComponent } from "../../zaken/zaak-verkort/zaak-verkort.component";
import { ZakenService } from "../../zaken/zaken.service";
import { TakenService } from "../taken.service";
import { TaakViewComponent } from "./taak-view.component";

describe(TaakViewComponent.name, () => {
  let fixture: ComponentFixture<TaakViewComponent>;
  let component: ComponentRef<TaakViewComponent>;
  let loader: HarnessLoader;

  let websocketService: WebsocketService;
  let takenService: TakenService;
  let zakenService: ZakenService;
  let taakFormulierenService: TaakFormulierenService;

  const taak: GeneratedType<"RestTask"> = {
    id: "test-id",
    zaakUuid: "test-zaakUuid",
    behandelaar: undefined,
    groep: undefined,
    naam: "test-taak",
    fataledatum: new Date().toISOString(),
    creatiedatumTijd: new Date().toISOString(),
    formioFormulier: {},
    rechten: {
      lezen: true,
      toekennen: true,
      wijzigen: true,
      toevoegenDocument: true,
    },
    status: "TOEGEKEND",
    taakdata: {},
    formulierDefinitie: undefined,
    formulierDefinitieId: "DEFAULT_TAAKFORMULIER",
    tabellen: {},
    taakdocumenten: [],
    taakinformatie: {},
    toelichting: undefined,
    toekenningsdatumTijd: new Date().toISOString(),
    zaaktypeOmschrijving: "test-zaaktypeOmschrijving",
    zaakIdentificatie: "test-zaakIdentificatie",
  };

  const zaak = fromPartial<GeneratedType<"RestZaak">>({
    uuid: "test-zaak-uuid",
    zaaktype: fromPartial<GeneratedType<"RestZaaktype">>({
      uuid: "test-zaaktype-uuid",
      omschrijving: "Test Zaaktype",
      zaakafhandelparameters: {
        afrondenMail: "BESCHIKBAAR_AAN",
      },
    }),
    initiatorIdentificatie: fromPartial<
      GeneratedType<"BetrokkeneIdentificatie">
    >({
      type: "BSN",
      bsnNummer: "123456789",
    }),
    resultaat: null,
    besluiten: [],
  });

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [
        TaakViewComponent,
        ZaakVerkortComponent,
        SideNavComponent,
        StaticTextComponent,
        EditGroepBehandelaarComponent,
      ],
      imports: [
        MatSidenav,
        RouterModule.forRoot([]),
        TranslateModule.forRoot(),
        PipesModule,
        MaterialModule,
        MaterialFormBuilderModule,
        NoopAnimationsModule,
      ],
      providers: [
        WebsocketService,
        TakenService,
        ZakenService,
        TaakFormulierenService,
        {
          provide: ActivatedRoute,
          useValue: { data: of({ taak }) },
        },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TaakViewComponent);
    fixture.detectChanges();

    component = fixture.componentRef;
    component.instance.actionsSidenav =
      TestBed.createComponent(MatSidenav).componentInstance;

    loader = TestbedHarnessEnvironment.loader(fixture);

    websocketService = TestBed.inject(WebsocketService);

    takenService = TestBed.inject(TakenService);
    jest.spyOn(takenService, "readTaak").mockReturnValue(of());

    taakFormulierenService = TestBed.inject(TaakFormulierenService);

    zakenService = TestBed.inject(ZakenService);
    jest.spyOn(zakenService, "readZaak").mockReturnValue(of(zaak));
  });

  describe(TaakViewComponent.prototype.documentCreated.name, () => {
    it(`should subscribe to ${Opcode.UPDATED} on ${ObjectType.ZAAK_INFORMATIEOBJECTEN}`, async () => {
      const addListener = jest.spyOn(websocketService, "addListener");

      component.instance.documentCreated();

      expect(addListener).toHaveBeenCalledTimes(1);
      expect(addListener).toHaveBeenCalledWith(
        Opcode.UPDATED,
        ObjectType.ZAAK_INFORMATIEOBJECTEN,
        taak.zaakUuid,
        expect.any(Function),
      );
    });

    it(`should reload the history when a ${Opcode.UPDATED} on ${ObjectType.ZAAK_INFORMATIEOBJECTEN} is received`, () => {
      const listHistorieVoorTaak = jest.spyOn(
        takenService,
        "listHistorieVoorTaak",
      );

      component.instance.documentCreated();

      websocketService["onMessage"]({
        opcode: Opcode.UPDATED,
        objectType: ObjectType.ZAAK_INFORMATIEOBJECTEN,
        objectId: new ScreenEventId(taak.zaakUuid),
      });

      expect(listHistorieVoorTaak).toHaveBeenCalledTimes(1);
      expect(listHistorieVoorTaak).toHaveBeenCalledWith(taak.id);
    });

    it("should read the task when a screen event is received", async () => {
      const readTaak = jest.spyOn(takenService, "readTaak");

      websocketService["onMessage"]({
        opcode: Opcode.ANY,
        objectType: ObjectType.TAAK,
        objectId: new ScreenEventId(taak.id!),
      });

      expect(readTaak).toHaveBeenCalledWith(taak.id!);
    });
  });

  describe("angular basic task form", () => {
    beforeEach(() => {
      jest
        .spyOn(taakFormulierenService, "getAngularHandleFormBuilder")
        .mockResolvedValue([]);
    });

    it("should create the form when the task is loaded", async () => {
      component.instance.ngOnInit();
      await fixture.whenStable();

      const fields = await loader.getAllHarnesses(MatFormFieldHarness);

      expect(fields.length).toBe(1); // `explanation`
    });
  });

  describe("angular task form with fields", () => {
    beforeEach(() => {
      jest
        .spyOn(taakFormulierenService, "getAngularHandleFormBuilder")
        .mockResolvedValue([
          [{ type: "input", key: "question" }, new FormControl("")],
        ]);
    });

    it("should create the form when the task is loaded", async () => {
      component.instance.ngOnInit();
      await fixture.whenStable();

      const input = await loader.getHarness(MatInputHarness);

      expect(input).not.toBeNull();
    });
  });

  describe("custom form builder", () => {
    beforeEach(() => {
      jest
        .spyOn(taakFormulierenService, "getAngularHandleFormBuilder")
        .mockImplementation(() => {
          throw new Error("Not implemented");
        });
      jest.spyOn(taakFormulierenService, "getFormulierBuilder").mockReturnValue(
        fromPartial({
          behandelForm: () =>
            fromPartial({
              build: () =>
                fromPartial({
                  form: [],
                }),
            }),
        }),
      );
    });

    it("should fallback to the old custom form builder", () => {
      const spy = jest.spyOn(taakFormulierenService, "getFormulierBuilder");
      component.instance.ngOnInit();
      expect(spy).toHaveBeenCalledWith(taak.formulierDefinitieId);
    });
  });
});
