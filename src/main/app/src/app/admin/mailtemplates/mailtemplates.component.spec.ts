/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { provideHttpClient } from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatButtonHarness } from "@angular/material/button/testing";
import { MatDialog, MatDialogRef } from "@angular/material/dialog";
import { MatIconHarness } from "@angular/material/icon/testing";
import { MatInputHarness } from "@angular/material/input/testing";
import { MatTableHarness } from "@angular/material/table/testing";
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
  let loader: HarnessLoader;
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
    loader = TestbedHarnessEnvironment.loader(fixture);
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

  it("should render a row for each mailtemplate", async () => {
    const table = await loader.getHarness(MatTableHarness);
    const rows = await table.getRows({ selector: ".main-row" });
    expect(rows).toHaveLength(1);
  });

  it("should mark a mailtemplate as disabled when it has a koppeling", () => {
    jest
      .spyOn(mailtemplateKoppelingService, "listMailtemplateKoppelingen")
      .mockReturnValue(of([koppeling]));

    component["laadMailtemplates"]();
    fixture.detectChanges();

    expect(component["isDisabled"](mailtemplate)).toBe(true);
  });

  it("should open confirm dialog when verwijderMailtemplate is called", () => {
    jest.spyOn(dialog, "open").mockReturnValue({
      afterClosed: () => of(false),
    } as MatDialogRef<unknown>);

    component["verwijderMailtemplate"](mailtemplate);

    expect(dialog.open).toHaveBeenCalled();
  });

  it("should reload mailtemplates and show snackbar after confirmed delete", () => {
    jest.spyOn(dialog, "open").mockReturnValue({
      afterClosed: () => of(true),
    } as MatDialogRef<unknown>);

    component["verwijderMailtemplate"](mailtemplate);

    expect(utilServiceMock.openSnackbar).toHaveBeenCalledWith(
      "msg.mailtemplate.verwijderen.uitgevoerd",
    );
    expect(mailtemplateBeheerService.listMailtemplates).toHaveBeenCalledTimes(
      2,
    ); // once on init, once after delete
  });

  it("should show close icon for non-default mailtemplate", async () => {
    await loader.getHarness(MatIconHarness.with({ name: "close" }));
  });

  it("should show done icon and hide delete button for default mailtemplate", async () => {
    const defaultTemplate = { ...mailtemplate, defaultMailtemplate: true };
    jest
      .spyOn(mailtemplateBeheerService, "listMailtemplates")
      .mockReturnValue(of([defaultTemplate]));
    component["laadMailtemplates"]();
    fixture.detectChanges();

    await loader.getHarness(MatIconHarness.with({ name: "done" }));
    expect(
      await loader.getAllHarnesses(MatButtonHarness.with({ selector: "#verwijderen" })),
    ).toHaveLength(0);
  });

  it("should show expand arrow down when row is disabled and not expanded", async () => {
    jest
      .spyOn(mailtemplateKoppelingService, "listMailtemplateKoppelingen")
      .mockReturnValue(of([koppeling]));
    component["laadMailtemplates"]();
    fixture.detectChanges();

    await loader.getHarness(MatIconHarness.with({ name: "keyboard_arrow_down" }));
  });

  it("should expand row and show arrow up icon on main-row click", async () => {
    jest
      .spyOn(mailtemplateKoppelingService, "listMailtemplateKoppelingen")
      .mockReturnValue(of([koppeling]));
    component["laadMailtemplates"]();
    fixture.detectChanges();

    const table = await loader.getHarness(MatTableHarness);
    const rows = await table.getRows();
    await (await rows[0].host()).click();
    fixture.detectChanges();

    expect(component["expandedRow"]).toEqual(mailtemplate);

    await loader.getHarness(MatIconHarness.with({ name: "keyboard_arrow_up" }));
  });

  it("should collapse row when clicked again", async () => {
    jest
      .spyOn(mailtemplateKoppelingService, "listMailtemplateKoppelingen")
      .mockReturnValue(of([koppeling]));
    component["laadMailtemplates"]();
    fixture.detectChanges();

    const table = await loader.getHarness(MatTableHarness);
    const rows = await table.getRows();
    const firstRowHost = await rows[0].host();
    await firstRowHost.click();
    fixture.detectChanges();
    await firstRowHost.click();
    fixture.detectChanges();

    expect(component["expandedRow"]).toBeNull();
  });

  it("should return koppelingen for the matching mailtemplate", () => {
    jest
      .spyOn(mailtemplateKoppelingService, "listMailtemplateKoppelingen")
      .mockReturnValue(of([koppeling]));
    component["laadMailtemplates"]();
    fixture.detectChanges();

    const koppelingen = component["getKoppelingen"](mailtemplate);
    expect(koppelingen).toHaveLength(1);
    expect(koppelingen[0]).toEqual(koppeling);
  });

  it("should return empty koppelingen for unrelated mailtemplate", () => {
    const otherTemplate = { ...mailtemplate, id: 99 };
    jest
      .spyOn(mailtemplateKoppelingService, "listMailtemplateKoppelingen")
      .mockReturnValue(of([koppeling]));
    component["laadMailtemplates"]();
    fixture.detectChanges();

    expect(component["getKoppelingen"](otherTemplate)).toHaveLength(0);
  });

  it("should apply filter to data source on keyup", async () => {
    const input = await loader.getHarness(MatInputHarness);
    await input.setValue("Test");
    await (await input.host()).dispatchEvent("keyup");
    fixture.detectChanges();

    expect(component["dataSource"].filter).toBe("Test");
  });

  it("should sort data ascending by mailTemplateNaam", () => {
    const template2 = {
      ...mailtemplate,
      id: 2,
      mailTemplateNaam: "A Template",
    };
    jest
      .spyOn(mailtemplateBeheerService, "listMailtemplates")
      .mockReturnValue(of([mailtemplate, template2]));
    component["laadMailtemplates"]();
    fixture.detectChanges();

    component["sortData"]({ active: "mailTemplateNaam", direction: "asc" });
    fixture.detectChanges();

    expect(component["dataSource"].data[0].mailTemplateNaam).toBe("A Template");
  });

  it("should sort data descending by mail", () => {
    const template2 = {
      ...mailtemplate,
      id: 2,
      mail: "ZAAK_ALGEMEEN",
    } as GeneratedType<"RESTMailtemplate">;
    jest
      .spyOn(mailtemplateBeheerService, "listMailtemplates")
      .mockReturnValue(of([mailtemplate, template2]));
    component["laadMailtemplates"]();
    fixture.detectChanges();

    component["sortData"]({ active: "mail", direction: "desc" });
    fixture.detectChanges();

    expect(component["dataSource"].data[0].mail).toBe("ZAAK_ALGEMEEN");
  });

  it("should show empty state when data source is empty", async () => {
    jest
      .spyOn(mailtemplateBeheerService, "listMailtemplates")
      .mockReturnValue(of([]));
    component["laadMailtemplates"]();
    fixture.detectChanges();

    const table = await loader.getHarness(MatTableHarness);
    const rows = await table.getRows();
    expect(rows).toHaveLength(0);
  });
});
