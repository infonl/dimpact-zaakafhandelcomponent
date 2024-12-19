/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { provideHttpClientTesting } from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { MatSidenav } from "@angular/material/sidenav";
import { provideAnimations } from "@angular/platform-browser/animations";
import { ActivatedRoute, RouterModule } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { of } from "rxjs";
import { ObjectType } from "../../core/websocket/model/object-type";
import { Opcode } from "../../core/websocket/model/opcode";
import { ScreenEventId } from "../../core/websocket/model/screen-event-id";
import { WebsocketService } from "../../core/websocket/websocket.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { TakenService } from "../taken.service";
import { TaakViewComponent } from "./taak-view.component";
import { provideHttpClient, withInterceptorsFromDi } from "@angular/common/http";

describe(TaakViewComponent.name, () => {
  let component: TaakViewComponent;
  let websocketService: WebsocketService;
  let takenService: TakenService;

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
    formulierDefinitieId: "test-formulierDefinitieId",
    tabellen: {},
    taakdocumenten: [],
    taakinformatie: {},
    toelichting: undefined,
    toekenningsdatumTijd: new Date().toISOString(),
    zaaktypeOmschrijving: "test-zaaktypeOmschrijving",
    zaakIdentificatie: "test-zaakIdentificatie",
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
    imports: [MatSidenav,
        RouterModule.forRoot([]),
        TranslateModule.forRoot()],
    providers: [
        TaakViewComponent,
        WebsocketService,
        TakenService,
        provideAnimations(),
        {
            provide: ActivatedRoute,
            useValue: { data: of({ taak }) },
        },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
    ]
}).compileComponents();

    component = TestBed.inject(TaakViewComponent);
    component.ngOnInit();
    component.actionsSidenav =
      TestBed.createComponent(MatSidenav).componentInstance;

    websocketService = TestBed.inject(WebsocketService);

    takenService = TestBed.inject(TakenService);
  });

  describe(TaakViewComponent.prototype.documentCreated.name, () => {
    it(`should subscribe to ${Opcode.UPDATED} on ${ObjectType.ZAAK_INFORMATIEOBJECTEN}`, () => {
      const addListener = jest.spyOn(websocketService, "addListener");

      component.documentCreated();

      expect(addListener).toHaveBeenCalledTimes(1);
      expect(addListener).toHaveBeenCalledWith(
        Opcode.UPDATED,
        ObjectType.ZAAK_INFORMATIEOBJECTEN,
        taak.zaakUuid,
        expect.any(Function),
      );
    });

    it(`should reload the "taak" when a ${Opcode.UPDATED} on ${ObjectType.ZAAK_INFORMATIEOBJECTEN} is received`, () => {
      const readTaak = jest.spyOn(takenService, "readTaak");

      component.documentCreated();

      websocketService["onMessage"]({
        opcode: Opcode.UPDATED,
        objectType: ObjectType.ZAAK_INFORMATIEOBJECTEN,
        objectId: new ScreenEventId(taak.zaakUuid),
      });

      expect(readTaak).toHaveBeenCalledTimes(1);
      expect(readTaak).toHaveBeenCalledWith(taak.id);
    });
  });
});
