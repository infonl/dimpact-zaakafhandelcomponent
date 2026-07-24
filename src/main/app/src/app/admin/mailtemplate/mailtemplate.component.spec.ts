/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { Component } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { ReactiveFormsModule } from "@angular/forms";
import { MatCardModule } from "@angular/material/card";
import { MatExpansionModule } from "@angular/material/expansion";
import { MatSidenavModule } from "@angular/material/sidenav";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import {
  ActivatedRoute,
  provideRouter,
  Router,
  RouterModule,
} from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { of } from "rxjs";
import { sleep, testQueryClient } from "../../../../setupJest";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { UtilService } from "../../core/service/util.service";
import { ZacFormActions } from "../../shared/form/form-actions/form-actions.component";
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
    MatExpansionModule,
    RouterModule,
    TranslateModule,
    SideNavComponent,
    MaterialFormBuilderModule,
    ZacFormActions,
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

const fakeValidTemplate = {
  mailTemplateNaam: "Nieuw template",
  mail: {
    label: "mail.TAAK_ONTVANGSTBEVESTIGING",
    value: "TAAK_ONTVANGSTBEVESTIGING",
  },
  onderwerp: "Onderwerp",
  body: "Body tekst",
  defaultMailtemplate: false,
} as const;

describe(MailtemplateComponent.name, () => {
  let fixture: ComponentFixture<TestMailtemplateComponent>;
  let component: TestMailtemplateComponent;
  let mailtemplateBeheerService: MailtemplateBeheerService;
  let router: Router;
  let httpTestingController: HttpTestingController;
  let utilServiceMock: Pick<UtilService, "setTitle" | "openSnackbar">;

  afterEach(() => {
    httpTestingController.verify();
  });

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
        provideQueryClient(testQueryClient),
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
    httpTestingController = TestBed.inject(HttpTestingController);

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

  it("should POST a new template and navigate on valid form submit", async () => {
    jest.spyOn(router, "navigate").mockResolvedValue(true);

    component.testForm.patchValue(fakeValidTemplate);

    component["saveMailtemplate"]();
    await new Promise(requestAnimationFrame);

    const request = httpTestingController.expectOne(
      "/rest/beheer/mailtemplates",
    );
    expect(request.request.method).toBe("POST");
    expect(request.request.body).toEqual({
      mail: "TAAK_ONTVANGSTBEVESTIGING",
      mailTemplateNaam: "Nieuw template",
      onderwerp: "Onderwerp",
      // The rich-text editor normalises plain text into a paragraph.
      body: "<p>Body tekst</p>",
      defaultMailtemplate: false,
    });
    request.flush({});
    await sleep();

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

  it("should disable the submit button while a save is in progress", async () => {
    component.testForm.patchValue(fakeValidTemplate);
    component.testForm.markAsDirty();
    fixture.detectChanges();

    const submitButton = fixture.nativeElement.querySelector(
      "button[type='submit']",
    ) as HTMLButtonElement;
    expect(submitButton.disabled).toBe(false);

    component["saveMailtemplate"]();
    await new Promise(requestAnimationFrame);
    fixture.detectChanges();

    // The request is left pending, so the mutation stays in-flight. expectOne
    // also asserts a single request was fired (the double-submit guard).
    const request = httpTestingController.expectOne(
      "/rest/beheer/mailtemplates",
    );
    expect(submitButton.disabled).toBe(true);

    request.flush({});
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

  it("should PUT to the template id when saving an existing template", async () => {
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
    await new Promise(requestAnimationFrame);

    const request = httpTestingController.expectOne(
      "/rest/beheer/mailtemplates/42",
    );
    expect(request.request.method).toBe("PUT");
    expect(request.request.body).toEqual(
      expect.objectContaining({ mailTemplateNaam: "Bestaand template" }),
    );
    request.flush({});
  });
});
