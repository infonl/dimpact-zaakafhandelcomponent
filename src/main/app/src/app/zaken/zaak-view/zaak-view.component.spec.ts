/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { TestBed, ComponentFixture } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ZaakViewComponent } from './zaak-view.component';
import { of } from 'rxjs';
import { ActivatedRoute } from '@angular/router';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { MatNavListItemHarness } from '@angular/material/list/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { TranslateModule } from '@ngx-translate/core';
import { PipesModule } from '../../shared/pipes/pipes.module';
import { MaterialModule } from '../../shared/material/material.module';
import { VertrouwelijkaanduidingToTranslationKeyPipe } from '../../shared/pipes/vertrouwelijkaanduiding-to-translation-key.pipe';
import { HarnessLoader } from '@angular/cdk/testing';
import { provideHttpClient } from '@angular/common/http';
import { zaakMock } from './zaak-mock';
import { UtilService } from 'src/app/core/service/util.service';


describe(ZaakViewComponent.name, () => {
  let component: ZaakViewComponent;
  let fixture: ComponentFixture<ZaakViewComponent>;
  let loader: HarnessLoader;

  let utilService: UtilService;

  const mockUtilService = {
    disableActionBar: jest.fn(),
  };
  
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ZaakViewComponent],
      imports: [
        TranslateModule.forRoot(),
        NoopAnimationsModule,
        PipesModule,
        MaterialModule,
        VertrouwelijkaanduidingToTranslationKeyPipe,
      ],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: {
            data: of({ zaak: zaakMock }),
          },
        },
        { provide: UtilService, useValue: mockUtilService }, // Mock UtilService
        VertrouwelijkaanduidingToTranslationKeyPipe,
      ],
    }).compileComponents();
  
    fixture = TestBed.createComponent(ZaakViewComponent);
    component = fixture.componentInstance;

    utilService = TestBed.inject(UtilService);

    jest.spyOn(utilService as any, 'disableActionBar').mockReturnValueOnce(undefined);
    jest.spyOn(component as any, 'loadHistorie').mockReturnValueOnce(undefined);
    jest.spyOn(component as any, 'loadBetrokkenen').mockReturnValueOnce(undefined);
    jest.spyOn(component as any, 'loadBagObjecten').mockReturnValueOnce(undefined);
    jest.spyOn(component as any, 'loadOpschorting').mockReturnValueOnce(undefined);
    jest.spyOn(component as any, 'setDateFieldIconSet').mockReturnValueOnce(undefined);
  

    loader = TestbedHarnessEnvironment.loader(fixture);
    component.init(zaakMock);
  });
  
  describe('actie.zaak.opschorten', () => {
    it('should not show the opschorten button when isEerderOpgeschort is true', async () => {

      component.zaak.isEerderOpgeschort = true;

      const button = await loader.getHarnessOrNull(
        MatNavListItemHarness.with({ title: 'actie.zaak.opschorten' })
      );
      expect(button).toBeUndefined();
    });
  });
});