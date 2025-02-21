/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { provideHttpClient } from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { ActivatedRoute, RouterModule } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { fromPartial } from "@total-typescript/shoehorn";
import { of } from "rxjs";
import { IdentityService } from "../../identity/identity.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { MailtemplateBeheerService } from "../mailtemplate-beheer.service";
import { ReferentieTabelService } from "../referentie-tabel.service";
import { ZaakafhandelParametersService } from "../zaakafhandel-parameters.service";
import { ParameterEditComponent } from "./parameter-edit.component";
import { MaterialModule } from "../../shared/material/material.module";
import { CommonModule } from "@angular/common";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { SideNavComponent } from "../../shared/side-nav/side-nav.component";
import {StaticTextComponent} from "../../shared/static-text/static-text.component";
import {EmptyPipe} from "../../shared/pipes/empty.pipe";
import {PipesModule} from "../../shared/pipes/pipes.module";

describe(ParameterEditComponent.name, () => {
  let component: ParameterEditComponent;
  let fixture: ComponentFixture<typeof component>;
  let zaakafhandelParametersService: ZaakafhandelParametersService;
  let referentieTabelService: ReferentieTabelService;
  let identityService: IdentityService;
  let mailtemplateBeheerService: MailtemplateBeheerService;

  const zaakAfhandelParamaters = fromPartial<
    GeneratedType<"RestZaakafhandelParameters">
  >({
    defaultGroepId: "test-group-id",
    zaaktype: {
        uuid: "test-uuid",
    },
    humanTaskParameters: [],
    userEventListenerParameters: [],
    mailtemplateKoppelingen: [],
    zaakAfzenders: [],
    smartDocuments: {}
  });

  describe("Case handler", () => {
    beforeEach(async () => {
      await TestBed.configureTestingModule({
        declarations: [ParameterEditComponent, SideNavComponent, StaticTextComponent],
        imports: [
          TranslateModule.forRoot(),
          MaterialModule,
          NoopAnimationsModule,
          RouterModule,
            PipesModule
        ],
        providers: [
          provideHttpClient(),
          provideHttpClientTesting(),
          {
            provide: ActivatedRoute,
            useValue: {
              data: of({ parameters: zaakAfhandelParamaters }),
            },
          },
        ],
      }).compileComponents();

       zaakafhandelParametersService = TestBed.inject(
        ZaakafhandelParametersService,
      );
      jest
        .spyOn(zaakafhandelParametersService, "listCaseDefinitions")
        .mockReturnValue(of([]));
      jest
        .spyOn(zaakafhandelParametersService, "listFormulierDefinities")
        .mockReturnValue(of([]));
      jest
        .spyOn(zaakafhandelParametersService, "listReplyTos")
        .mockReturnValue(of([]));
      jest
        .spyOn(zaakafhandelParametersService, "listZaakbeeindigRedenen")
        .mockReturnValue(of([]));
      jest.spyOn(zaakafhandelParametersService, "listResultaattypes").mockReturnValue(of([]));

      referentieTabelService = TestBed.inject(ReferentieTabelService);
      jest
        .spyOn(referentieTabelService, "listReferentieTabellen")
        .mockReturnValue(of([]));
      jest
        .spyOn(referentieTabelService, "listDomeinen")
        .mockReturnValue(of([]));
      jest
        .spyOn(referentieTabelService, "listAfzenders")
        .mockReturnValue(of([]));

       identityService = TestBed.inject(IdentityService);
      jest.spyOn(identityService, "listGroups").mockReturnValue(of([]));
      jest.spyOn(identityService, "listUsersInGroup").mockReturnValue(of([]));

      mailtemplateBeheerService = TestBed.inject(
        MailtemplateBeheerService,
      );
      jest
        .spyOn(mailtemplateBeheerService, "listKoppelbareMailtemplates")
        .mockReturnValue(of([]));

    });

    it("should not set a case handler when no group is selected", async () => {});

    it("should set the case handlers which are in the selected group", async () => {
      const users: GeneratedType<'RestUser'>[] = [{ id: 'test-user-id', naam: 'test-user' }]
      const listUsersInGroup = jest.spyOn(identityService, "listUsersInGroup")
      listUsersInGroup.mockReturnValue(of(users));

      fixture = TestBed.createComponent(ParameterEditComponent);
      component = fixture.componentInstance;

      fixture.detectChanges();
      await fixture.whenStable();

      expect(listUsersInGroup).toHaveBeenCalledWith("test-group-id");
      expect(component.medewerkers).toBe(users);
    });

    it("should update the case handlers when the group changes", async () => {
      await fixture.whenStable();
    });
  });
});
