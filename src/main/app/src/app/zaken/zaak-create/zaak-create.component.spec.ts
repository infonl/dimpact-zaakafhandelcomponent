/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { provideHttpClient } from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { MatSidenavModule } from "@angular/material/sidenav";
import { provideAnimations } from "@angular/platform-browser/animations";
import { RouterModule } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { of } from "rxjs";
import { CaseDefinition } from "../../admin/model/case-definition";
import { ReferentieTabelService } from "../../admin/referentie-tabel.service";
import { UtilService } from "../../core/service/util.service";
import { IdentityService } from "../../identity/identity.service";
import { KlantenService } from "../../klanten/klanten.service";
import { NavigationService } from "../../shared/navigation/navigation.service";
import { ZaakStatusmailOptie } from "../model/zaak-statusmail-optie";
import { Zaaktype } from "../model/zaaktype";
import { ZakenService } from "../zaken.service";
import { ZaakCreateComponent } from "./zaak-create.component";

describe(ZaakCreateComponent.name, () => {
  let component: ZaakCreateComponent;

  let identityService: IdentityService;

  const zaakType = new Zaaktype();
  zaakType.zaakafhandelparameters = {
    defaultBehandelaarId: "default-behandelaar",
    defaultGroepId: "default-group",
    einddatumGeplandWaarschuwing: 10,
    zaaktype: zaakType,
    afrondenMail: ZaakStatusmailOptie.BESCHIKBAAR_AAN,
    caseDefinition: new CaseDefinition(),
    creatiedatum: new Date().toJSON(),
    domein: "test",
    humanTaskParameters: [],
    intakeMail: ZaakStatusmailOptie.BESCHIKBAAR_AAN,
    mailtemplateKoppelingen: [],
    productaanvraagtype: "",
    uiterlijkeEinddatumAfdoeningWaarschuwing: 10,
    valide: true,
    userEventListenerParameters: [],
    zaakAfzenders: [],
    zaakbeeindigParameters: [],
    smartDocuments: {},
    zaakNietOntvankelijkResultaattype: {
      id: "1",
    },
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [ZaakCreateComponent],
      providers: [
        ZakenService,
        NavigationService,
        KlantenService,
        ReferentieTabelService,
        UtilService,
        IdentityService,
        provideAnimations(),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
      imports: [
        RouterModule.forRoot([]),
        TranslateModule.forRoot(),
        MatSidenavModule,
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(ZaakCreateComponent);
    component = fixture.componentInstance;

    identityService = TestBed.inject(IdentityService);
    jest
      .spyOn(identityService, "listGroups")
      .mockReturnValue(of([{ id: "test-group-id", naam: "test group" }]));
    jest
      .spyOn(identityService, "listUsersInGroup")
      .mockReturnValue(of([{ id: "test-user-id", naam: "test user" }]));
  });

  describe(ZaakCreateComponent.prototype.zaaktypeGeselecteerd.name, () => {
    it(`should call ${ZaakCreateComponent.prototype.getMedewerkerGroupFormField.name} with the default behandelaar and groep`, () => {
      const getMedewerkerGroupFormField = jest.spyOn(
        component,
        "getMedewerkerGroupFormField",
      );

      component.zaaktypeGeselecteerd(zaakType);

      expect(getMedewerkerGroupFormField).toHaveBeenCalledTimes(1);
      expect(getMedewerkerGroupFormField).toHaveBeenCalledWith(
        zaakType.zaakafhandelparameters.defaultGroepId,
        zaakType.zaakafhandelparameters.defaultBehandelaarId,
      );
    });
  });
});
