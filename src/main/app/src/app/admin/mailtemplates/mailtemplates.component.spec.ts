/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatDialog, MatDialogRef } from "@angular/material/dialog";
import { By } from "@angular/platform-browser";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { RouterModule } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { EMPTY, of } from "rxjs";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { UtilService } from "../../core/service/util.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { MailtemplateBeheerService } from "../mailtemplate-beheer.service";
import { MailtemplateKoppelingService } from "../mailtemplate-koppeling.service";
import { MailtemplatesComponent } from "./mailtemplates.component";

describe(MailtemplatesComponent.name, () => {
  let fixture: ComponentFixture<MailtemplatesComponent>;
  let component: MailtemplatesComponent;
  let mailtemplateBeheerService: MailtemplateBeheerService;
  let mailtemplateKoppelingService: MailtemplateKoppelingService;
  let dialog: MatDialog;
  let utilServiceMock: Pick<UtilService, "setTitle" | "openSnackbar">;

  const mailtemplate: GeneratedType<"RESTMailtemplate"> = {
    id: 1,
    mailTemplateNaam: "Test Template",
    mail: "TAAK_ONTVANGSTBEVESTIGING",
    onderwerp: "Onderwerp",
    body: "Body",
    defaultMailtemplate: false,
  };

  const koppeling: GeneratedType<"RESTMailtemplateKoppeling"> = {
    id: 1,
    mailtemplate: { id: 1 } as GeneratedType<"RESTMailtemplate">,
    zaakafhandelParameters: {
      zaaktype: {
        omschrijving: "Test zaaktype",
        uuid: "uuid-1",
      },
    } as GeneratedType<"RestZaakafhandelParameters">,
  };

  beforeEach(async () => {
    utilServiceMock = {
      setTitle: jest.fn(),
      openSnackbar: jest.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [
        MailtemplatesComponent,
        NoopAnimationsModule,
        RouterModule.forRoot([]),
        TranslateModule.forRoot(),
      ],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: UtilService, useValue: utilServiceMock },
        {
          provide: ConfiguratieService,
          useValue: {} satisfies Partial<ConfiguratieService>,
        },
      ],
    }).compileComponents();

    mailtemplateBeheerService = TestBed.inject(MailtemplateBeheerService);
    mailtemplateKoppelingService = TestBed.inject(MailtemplateKoppelingService);

    fixture = TestBed.createComponent(MailtemplatesComponent);
    component = fixture.componentInstance;

    dialog = fixture.debugElement.injector.get(MatDialog);

    jest
      .spyOn(mailtemplateBeheerService, "listMailtemplates")
      .mockReturnValue(of([mailtemplate]));
    jest
      .spyOn(mailtemplateBeheerService, "deleteMailtemplate")
      .mockReturnValue(EMPTY);
    jest
      .spyOn(mailtemplateKoppelingService, "listMailtemplateKoppelingen")
      .mockReturnValue(of([]));

    fixture.detectChanges();
  });

  it("should call setTitle and load mailtemplates on init", () => {
    expect(utilServiceMock.setTitle).toHaveBeenCalledWith(
      "title.mailtemplates",
      undefined,
    );
    expect(mailtemplateBeheerService.listMailtemplates).toHaveBeenCalled();
    expect(
      mailtemplateKoppelingService.listMailtemplateKoppelingen,
    ).toHaveBeenCalled();
  });

  it("should render a row for each mailtemplate", () => {
    const rows = fixture.debugElement.queryAll(By.css("tr.main-row"));
    expect(rows).toHaveLength(1);
  });

  it("should mark a mailtemplate as disabled when it has a koppeling", () => {
    jest
      .spyOn(mailtemplateKoppelingService, "listMailtemplateKoppelingen")
      .mockReturnValue(of([koppeling]));

    component.laadMailtemplates();
    fixture.detectChanges();

    expect(component.isDisabled(mailtemplate)).toBe(true);
  });

  it("should open confirm dialog when verwijderMailtemplate is called", () => {
    jest.spyOn(dialog, "open").mockReturnValue({
      afterClosed: () => of(false),
    } as MatDialogRef<unknown>);

    component.verwijderMailtemplate(mailtemplate);

    expect(dialog.open).toHaveBeenCalled();
  });

  it("should reload mailtemplates and show snackbar after confirmed delete", () => {
    jest.spyOn(dialog, "open").mockReturnValue({
      afterClosed: () => of(true),
    } as MatDialogRef<unknown>);

    component.verwijderMailtemplate(mailtemplate);

    expect(utilServiceMock.openSnackbar).toHaveBeenCalledWith(
      "msg.mailtemplate.verwijderen.uitgevoerd",
    );
    expect(mailtemplateBeheerService.listMailtemplates).toHaveBeenCalledTimes(
      2,
    ); // once on init, once after delete
  });
});
