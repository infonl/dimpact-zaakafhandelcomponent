import { PersoonZoekComponent } from "./persoon-zoek.component";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { KlantenService } from "../../klanten.service";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { TranslateModule } from "@ngx-translate/core";
import { provideHttpClient } from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { of } from "rxjs";
import { fromPartial } from "@total-typescript/shoehorn";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { ConfiguratieService } from "../../../configuratie/configuratie.service";
import { UtilService } from "../../../core/service/util.service";
import { FormCommunicatieService } from "../form-communicatie-service";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { MaterialFormBuilderModule } from "src/app/shared/material-form-builder/material-form-builder.module";
import { MatIconModule } from "@angular/material/icon";
import { MaterialModule } from "src/app/shared/material/material.module";
import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { MatInputHarness } from "@angular/material/input/testing";

describe(PersoonZoekComponent.name, () => {
  let component: PersoonZoekComponent;
  let fixture: ComponentFixture<typeof component>;
  let klantenService: KlantenService;
  let loader: HarnessLoader;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [PersoonZoekComponent],
      imports: [
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
      ])
    );
    jest
      .spyOn(klantenService, "listPersonen")
      .mockReturnValue(
        of(fromPartial<GeneratedType<"RESTResultaatRestPersoon">>({}))
      );

    const configuratieService = TestBed.inject(ConfiguratieService);
    jest
      .spyOn(configuratieService, "readGemeenteCode")
      .mockReturnValue(of("1234"));

    fixture = TestBed.createComponent(PersoonZoekComponent);
    component = fixture.componentInstance;

    fixture.componentRef.setInput("action", "test-action");
    fixture.componentRef.setInput("context", "test-context");

    loader = TestbedHarnessEnvironment.loader(fixture);

    fixture.detectChanges();
  });

  describe(PersoonZoekComponent.prototype.zoekPersonen.name, () => {
    it(`should call the ${KlantenService.prototype.listPersonen.name}`, () => {
      const spy = jest.spyOn(klantenService, "listPersonen");
      component.zoekPersonen();

      expect(spy).toHaveBeenCalledWith(expect.any(Object), {
        context: "test-context",
        action: "test-action",
      });
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
        {
          context: "test-context",
          action: "test-action",
        }
      );
    });

    it("should disabled all 'NON' fields when a 'REQ' field is filled", async () => {
      const inputs = await loader.getAllHarnesses(MatInputHarness);
      const [bsn, _geboortedatum, ...rest] = inputs;

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
  });
});
