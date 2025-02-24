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
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { SideNavComponent } from "../../shared/side-nav/side-nav.component";
import {StaticTextComponent} from "../../shared/static-text/static-text.component";
import {PipesModule} from "../../shared/pipes/pipes.module";
import {HarnessLoader} from "@angular/cdk/testing";
import {TestbedHarnessEnvironment} from "@angular/cdk/testing/testbed";
import {MatSelectHarness} from "@angular/material/select/testing";

describe(ParameterEditComponent.name, () => {
  let component: ParameterEditComponent;
  let fixture: ComponentFixture<typeof component>;
  let zaakafhandelParametersService: ZaakafhandelParametersService;
  let referentieTabelService: ReferentieTabelService;
  let identityService: IdentityService;
  let mailtemplateBeheerService: MailtemplateBeheerService;
  let loader: HarnessLoader;

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

  const users: GeneratedType<'RestUser'>[] = [
    { id: 'test-user-id', naam: 'test-user' },
    { id: 'test-user-id2', naam: 'test-user-2' },
  ]

  const groups: GeneratedType<'RestGroup'>[] = [
    { id: 'test-group-id', naam: 'test-group' },
    { id: 'test-group-id2', naam: 'test-group-2' },
  ]

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
      jest.spyOn(identityService, "listGroups").mockReturnValue(of(groups));
      jest.spyOn(identityService, "listUsersInGroup").mockReturnValue(of(users));

      mailtemplateBeheerService = TestBed.inject(
        MailtemplateBeheerService,
      );
      jest
        .spyOn(mailtemplateBeheerService, "listKoppelbareMailtemplates")
        .mockReturnValue(of([]));

      fixture = TestBed.createComponent(ParameterEditComponent);
      component = fixture.componentInstance;

      fixture.detectChanges();
      loader = TestbedHarnessEnvironment.loader(fixture)
    });

    it("should set the case handlers which are in the selected group", async () => {
      const listUsersInGroup = jest.spyOn(identityService, "listUsersInGroup")

      await fixture.whenStable();

      expect(listUsersInGroup).toHaveBeenCalledWith("test-group-id");
      expect(component.medewerkers).toBe(users);
    });

    it("should update the case handlers when the group changes", async () => {
      const listUsersInGroup = jest.spyOn(identityService, "listUsersInGroup")

      await fixture.whenStable();

      const select = await loader.getAllHarnesses(MatSelectHarness)
      console.log({select})
      // select.click();
      // fixture.detectChanges();
      // await fixture.whenStable();
      // const option = queryByText(fixture, 'mat-option', 'test-group-2');
      // console.log(option.nativeElement)
      // option.nativeElement.select();
      // fixture.detectChanges();
      // await fixture.whenStable();

      expect(listUsersInGroup).toHaveBeenCalledWith("test-group-id2");
    });
  });
});
