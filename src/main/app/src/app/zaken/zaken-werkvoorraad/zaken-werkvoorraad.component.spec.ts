/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ZakenWerkvoorraadComponent } from './zaken-werkvoorraad.component';
import { MatDialog } from '@angular/material/dialog';
import { UtilService } from '../../core/service/util.service';
import { IdentityService } from '../../identity/identity.service';
import { ZoekenService } from '../../zoeken/zoeken.service';
import { ZakenService } from '../zaken.service';
import { GebruikersvoorkeurenService } from '../../gebruikersvoorkeuren/gebruikersvoorkeuren.service';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { IndexingService } from 'src/app/indexing/indexing.service';
import { BatchProcessService } from 'src/app/shared/batch-progress/batch-process.service';
import { fromPartial } from "@total-typescript/shoehorn";
import {GeneratedType} from "../../shared/utils/generated-types";
import {ZaakZoekObject} from "../../zoeken/model/zaken/zaak-zoek-object";

describe('ZakenWerkvoorraadComponent', () => {
    let component: ZakenWerkvoorraadComponent;
    let fixture: ComponentFixture<ZakenWerkvoorraadComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [ZakenWerkvoorraadComponent],
            providers: [
                {provide: MatDialog, useValue: {}},
                {provide: UtilService, useValue: {}},
                {provide: IdentityService, useValue: {}},
                {provide: ZoekenService, useValue: {}},
                {provide: ZakenService, useValue: {}},
                {provide: GebruikersvoorkeurenService, useValue: {}},
                {provide: ActivatedRoute, useValue: {}},
                {provide: TranslateService, useValue: {instant: () => ''}},
                {provide: IndexingService, useValue: {}},
                {provide: BatchProcessService, useValue: {}},
            ],
        }).compileComponents();

        fixture = TestBed.createComponent(ZakenWerkvoorraadComponent);
        component = fixture.componentInstance;
    });

    describe('showAssignToMe Logic', () => {
        it('should return true from showAssignToMe if user is in group and not behandelaar', () => {
            component.ingelogdeMedewerker = {
                id: 'user1',
                naam: 'testuser-1',
                groupIds: ['groupA', 'groupB'],
            }

            const zaakZoekObject = fromPartial<ZaakZoekObject>({
                id: 'zaak1',
                rechten: { toekennen: true },
                groepId: 'groupA',
                behandelaarGebruikersnaam: 'user2',
            });

            expect(component.showAssignToMe(zaakZoekObject)).toBeTruthy();
        });

        it('should return false from showAssignToMe if user is behandelaar', () => {
            component.ingelogdeMedewerker = {
                id: 'user1',
                naam: 'testuser-1',
                groupIds: ['groupA', 'groupB'],
            }

            const zaakZoekObject = fromPartial<ZaakZoekObject>({
                id: 'zaak1',
                rechten: { toekennen: true },
                groepId: 'groupA',
                behandelaarGebruikersnaam: 'user1',
            });

            expect(component.showAssignToMe(zaakZoekObject)).toBeFalsy();
        });
    });
});
