/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { Component } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { ReactiveFormsModule } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import { MatCardModule } from "@angular/material/card";
import { MatSidenavModule } from "@angular/material/sidenav";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import {
  ActivatedRoute,
  Router,
  RouterModule,
  provideRouter,
} from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { of } from "rxjs";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { UtilService } from "../../core/service/util.service";
import { MaterialFormBuilderModule } from "../../shared/material-form-builder/material-form-builder.module";
import { SideNavComponent } from "../../shared/side-nav/side-nav.component";
import { MailtemplateBeheerService } from "../mailtemplate-beheer.service";
import { MailtemplateComponent } from "./mailtemplate.component";

@Component({
  templateUrl: "./mailtemplate.component.html",
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatSidenavModule,
    MatCardModule,
    MatButtonModule,
    RouterModule,
    TranslateModule,
    SideNavComponent,
    MaterialFormBuilderModule,
  ],
})
class TestMailtemplateComponent extends MailtemplateComponent {
  get testForm() {
    return this.form;
  }
  get testVariabelen() {
    return this.variabelen;
  }
}

describe(MailtemplateComponent.name, () => {
  let fixture: ComponentFixture<TestMailtemplateComponent>;
  let component: TestMailtemplateComponent;
  let mailtemplateBeheerService: MailtemplateBeheerService;
  let router: Router;
  let utilServiceMock: Pick<UtilService, "setTitle" | "openSnackbar">;

  beforeEach(async () => {
    utilServiceMock = {
      setTitle: jest.fn(),
      openSnackbar: jest.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [
        TestMailtemplateComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: UtilService, useValue: utilServiceMock },
        {
          provide: ConfiguratieService,
          useValue: {} satisfies Partial<ConfiguratieService>,
        },
        {
          provide: ActivatedRoute,
          useValue: { data: of({}) },
        },
      ],
    }).compileComponents();

    mailtemplateBeheerService = TestBed.inject(MailtemplateBeheerService);
    router = TestBed.inject(Router);

    jest
      .spyOn(mailtemplateBeheerService, "ophalenVariabelenVoorMail")
      .mockReturnValue(of([]));

    fixture = TestBed.createComponent(TestMailtemplateComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it("should call setTitle on init", () => {
    expect(utilServiceMock.setTitle).toHaveBeenCalledWith(
      "title.mailtemplate",
      undefined,
    );
  });

  it("should have an invalid form when required fields are empty", () => {
    expect(component.testForm.invalid).toBe(true);
  });

  it("should populate all form fields and load variables when loading an existing template", () => {
    jest
      .spyOn(mailtemplateBeheerService, "ophalenVariabelenVoorMail")
      .mockReturnValue(of(["GEMEENTE", "ZAAK_URL"]));

    TestBed.inject(ActivatedRoute).data = of({
      template: {
        id: 1,
        mailTemplateNaam: "Test template",
        mail: "TAAK_ONTVANGSTBEVESTIGING",
        onderwerp: "Test onderwerp",
        body: "Test body",
        defaultMailtemplate: true,
      },
    });

    fixture = TestBed.createComponent(TestMailtemplateComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    const raw = component.testForm.getRawValue();
    expect(raw.mailTemplateNaam).toBe("Test template");
    expect(raw.mail?.value).toBe("TAAK_ONTVANGSTBEVESTIGING");
    expect(raw.onderwerp).toBe("Test onderwerp");
    expect(raw.body).toBe("Test body");
    expect(raw.defaultMailtemplate).toBe(true);

    expect(
      mailtemplateBeheerService.ophalenVariabelenVoorMail,
    ).toHaveBeenCalledWith("TAAK_ONTVANGSTBEVESTIGING");
    expect(component.testVariabelen).toEqual(["GEMEENTE", "ZAAK_URL"]);
  });

  it("should navigate to /admin/mailtemplates on cancel", () => {
    jest.spyOn(router, "navigate").mockResolvedValue(true);

    component["cancel"]();

    expect(router.navigate).toHaveBeenCalledWith(["/admin/mailtemplates"]);
  });

  it("should save and navigate on valid form submit", () => {
    jest
      .spyOn(mailtemplateBeheerService, "createMailtemplate")
      .mockReturnValue(of({}));
    jest.spyOn(router, "navigate").mockResolvedValue(true);

    component.testForm.patchValue({
      mailTemplateNaam: "Nieuw template",
      mail: {
        label: "mail.TAAK_ONTVANGSTBEVESTIGING",
        value: "TAAK_ONTVANGSTBEVESTIGING",
      },
      onderwerp: "Onderwerp",
      body: "Body tekst",
      defaultMailtemplate: false,
    });

    component["saveMailtemplate"]();

    expect(mailtemplateBeheerService.createMailtemplate).toHaveBeenCalled();
    expect(utilServiceMock.openSnackbar).toHaveBeenCalledWith(
      "msg.mailtemplate.opgeslagen",
    );
    expect(router.navigate).toHaveBeenCalledWith(["/admin/mailtemplates"]);
  });

  it("should load variables when mail type is selected", () => {
    jest
      .spyOn(mailtemplateBeheerService, "ophalenVariabelenVoorMail")
      .mockReturnValue(of(["GEMEENTE", "ZAAK_URL"]));

    component.testForm.controls.mail.setValue({
      label: "mail.TAAK_ONTVANGSTBEVESTIGING",
      value: "TAAK_ONTVANGSTBEVESTIGING",
    });

    expect(
      mailtemplateBeheerService.ophalenVariabelenVoorMail,
    ).toHaveBeenCalledWith("TAAK_ONTVANGSTBEVESTIGING");
    expect(component.testVariabelen).toEqual(["GEMEENTE", "ZAAK_URL"]);
  });

  it("should disable the submit button when the form is invalid", () => {
    const submitButton = fixture.nativeElement.querySelector(
      "button[type='submit']",
    ) as HTMLButtonElement;
    expect(submitButton.disabled).toBe(true);
  });

  it("should disable the mail control when loading an existing template", () => {
    TestBed.inject(ActivatedRoute).data = of({
      template: {
        id: 1,
        mailTemplateNaam: "Test template",
        mail: "TAAK_ONTVANGSTBEVESTIGING",
        onderwerp: "Test onderwerp",
        body: "Test body",
        defaultMailtemplate: false,
      },
    });

    fixture = TestBed.createComponent(TestMailtemplateComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.testForm.controls.mail.disabled).toBe(true);
  });

  it("should call updateMailtemplate when saving an existing template", () => {
    jest
      .spyOn(mailtemplateBeheerService, "updateMailtemplate")
      .mockReturnValue(
        of({}) as ReturnType<
          typeof mailtemplateBeheerService.updateMailtemplate
        >,
      );
    jest.spyOn(router, "navigate").mockResolvedValue(true);

    TestBed.inject(ActivatedRoute).data = of({
      template: {
        id: 42,
        mailTemplateNaam: "Bestaand template",
        mail: "TAAK_ONTVANGSTBEVESTIGING",
        onderwerp: "Bestaand onderwerp",
        body: "Bestaand body",
        defaultMailtemplate: false,
      },
    });

    fixture = TestBed.createComponent(TestMailtemplateComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    component["saveMailtemplate"]();

    expect(mailtemplateBeheerService.updateMailtemplate).toHaveBeenCalledWith(
      42,
      expect.objectContaining({ mailTemplateNaam: "Bestaand template" }),
    );
  });
});
