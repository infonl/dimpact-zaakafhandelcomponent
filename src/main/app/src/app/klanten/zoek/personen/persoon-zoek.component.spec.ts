/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { provideHttpClient } from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { ComponentFixture, fakeAsync, TestBed, tick } from "@angular/core/testing";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { MatIconModule } from "@angular/material/icon";
import { MatInputHarness } from "@angular/material/input/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { provideTanStackQuery } from "@tanstack/angular-query-experimental";
import { of } from "rxjs";
import { PolicyService } from "src/app/policy/policy.service";
import { MaterialFormBuilderModule } from "src/app/shared/material-form-builder/material-form-builder.module";
import { MaterialModule } from "src/app/shared/material/material.module";
import { fromPartial } from "src/test-helpers";
import { testQueryClient } from "../../../../../setupJest";
import { ConfiguratieService } from "../../../configuratie/configuratie.service";
import { UtilService } from "../../../core/service/util.service";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { KlantenService } from "../../klanten.service";
import { FormCommunicatieService } from "../form-communicatie-service";
import { PersoonZoekComponent } from "./persoon-zoek.component";

describe(PersoonZoekComponent.name, () => {
  let component: PersoonZoekComponent;
  let fixture: ComponentFixture<typeof component>;
  let klantenService: KlantenService;
  let loader: HarnessLoader;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        PersoonZoekComponent,
        FormsModule,
        ReactiveFormsModule,
        MaterialFormBuilderModule,
        MatIconModule,
        MaterialModule,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: UtilService,
          useValue: {
            setLoading: jest.fn(),
          },
        },
        {
          provide: FormCommunicatieService,
          useValue: {
            itemSelected$: of({ selected: false, uuid: "test" }),
            notifyItemSelected: jest.fn(),
          },
        },
        provideTanStackQuery(testQueryClient),
      ],
    }).compileComponents();

    // Mock the services before first change detection
    klantenService = TestBed.inject(KlantenService);
    jest.spyOn(klantenService, "getPersonenParameters").mockReturnValue(
      of([
        {
          bsn: "REQ",
          geboortedatum: "OPT",
          gemeenteVanInschrijving: "NON",
          geslachtsnaam: "NON",
          huisnummer: "NON",
          postcode: "NON",
          straat: "NON",
          voornamen: "NON",
          voorvoegsel: "NON",
        },
      ]),
    );
    jest
      .spyOn(klantenService, "listPersonen")
      .mockReturnValue(
        of(fromPartial<GeneratedType<"RESTResultaatRestPersoon">>({})),
      );

    const configuratieService = TestBed.inject(ConfiguratieService);
    jest
      .spyOn(configuratieService, "readGemeenteCode")
      .mockReturnValue(of("1234"));

    TestBed.inject(PolicyService);

    fixture = TestBed.createComponent(PersoonZoekComponent);
    component = fixture.componentInstance;

    fixture.componentRef.setInput("action", "test-action");
    fixture.componentRef.setInput("context", "test-context");
    fixture.componentRef.setInput("zaaktypeUUID", "test-zaaktype-uuid");

    loader = TestbedHarnessEnvironment.loader(fixture);

    fixture.detectChanges();
  });

  describe(PersoonZoekComponent.prototype.zoekPersonen.name, () => {
    it(`should call the ${KlantenService.prototype.listPersonen.name}`, () => {
      const spy = jest.spyOn(klantenService, "listPersonen");
      component.zoekPersonen();

      expect(spy).toHaveBeenCalledWith(
        expect.any(Object),
        "test-zaaktype-uuid",
      );
    });

    it("should pass the fields in the request when the form is valid", async () => {
      const spy = jest.spyOn(klantenService, "listPersonen");

      const inputs = await loader.getAllHarnesses(MatInputHarness);
      const [bsn] = inputs;
      await bsn.setValue("999990408");

      component.zoekPersonen();

      expect(spy).toHaveBeenCalledWith(
        expect.objectContaining({
          bsn: "999990408",
        }),
        "test-zaaktype-uuid",
      );
    });

    it("should disabled all 'NON' fields when a 'REQ' field is filled", async () => {
      const inputs = await loader.getAllHarnesses(MatInputHarness);
      const [bsn, , ...rest] = inputs;

      await bsn.setValue("999990408");

      for (const input of rest) {
        expect(await input.isDisabled()).toBe(true);
      }
    });

    it("should not disable all 'OPT' fields when a 'REQ' field is filled", async () => {
      const inputs = await loader.getAllHarnesses(MatInputHarness);
      const [bsn, geboortedatum] = inputs;

      await bsn.setValue("999990408");

      expect(await geboortedatum.isDisabled()).toBe(false);
    });

    it("should extract gemeenteVanInschrijving code when it is an object", () => {
      const spy = jest.spyOn(klantenService, "listPersonen");
      component.formGroup.controls.gemeenteVanInschrijving.setValue({
        code: "0344",
      });

      component.zoekPersonen();

      expect(spy).toHaveBeenCalledWith(
        expect.objectContaining({
          gemeenteVanInschrijving: "0344",
        }),
        "test-zaaktype-uuid",
      );
    });

    it("should pass gemeenteVanInschrijving as string when it is a string", () => {
      const spy = jest.spyOn(klantenService, "listPersonen");
      component.formGroup.controls.gemeenteVanInschrijving.setValue("1234");

      component.zoekPersonen();

      expect(spy).toHaveBeenCalledWith(
        expect.objectContaining({
          gemeenteVanInschrijving: "1234",
        }),
        "test-zaaktype-uuid",
      );
    });
  });

  describe("brpGemeenten effect", () => {
    it("should auto-set gemeenteVanInschrijving when exactly one gemeente is returned", fakeAsync(() => {
      testQueryClient.setQueryData(
        klantenService.listBrpGemeenten().queryKey,
        [{ code: "0344", naam: "Utrecht" }],
      );

      tick();
      fixture.detectChanges();

      expect(
        component.formGroup.controls.gemeenteVanInschrijving.value,
      ).toEqual({ code: "0344", naam: "Utrecht" });
    }));
  });
});
