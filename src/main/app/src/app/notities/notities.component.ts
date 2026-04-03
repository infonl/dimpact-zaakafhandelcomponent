/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgFor, NgIf } from "@angular/common";
import { Component, ElementRef, Input, OnInit, ViewChild } from "@angular/core";
import { MatBadgeModule } from "@angular/material/badge";
import { MatButtonModule } from "@angular/material/button";
import { MatCardModule } from "@angular/material/card";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatIconModule } from "@angular/material/icon";
import { MatInputModule } from "@angular/material/input";
import { TranslateModule } from "@ngx-translate/core";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { CdkTextareaAutosize } from "@angular/cdk/text-field";
import { IdentityService } from "../identity/identity.service";
import { DatumPipe } from "../shared/pipes/datum.pipe";
import { GeneratedType } from "../shared/utils/generated-types";
import { NotitieService } from "./notities.service";

@Component({
  selector: "zac-notities",
  templateUrl: "./notities.component.html",
  styleUrls: ["./notities.component.less"],
  standalone: true,
  imports: [
    NgIf,
    NgFor,
    MatButtonModule,
    MatIconModule,
    MatBadgeModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    CdkTextareaAutosize,
    TranslateModule,
    DatumPipe,
  ],
})
export class NotitiesComponent implements OnInit {
  @Input({ required: true }) zaakUuid!: string;
  @Input() notitieRechten?: GeneratedType<"RestNotitieRechten">;

  @ViewChild("notitieTekst") notitieTekst!: {
    nativeElement: HTMLTextAreaElement;
  };
  @ViewChild("scrollTarget") scrollTarget!: ElementRef;

  private readonly loggedInUserQuery = injectQuery(() =>
    this.identityService.readLoggedInUser(),
  );

  protected notities: GeneratedType<"RestNote">[] = [];
  protected showNotes = false;
  protected geselecteerdeNotitieId: number | null = null;
  protected maxLengteTextArea = 1000;

  constructor(
    private identityService: IdentityService,
    private notitieService: NotitieService,
  ) {}

  ngOnInit(): void {
    this.haalNotitiesOp();
  }

  protected toggleNotitieContainer() {
    this.showNotes = !this.showNotes;
  }

  protected pasNotitieAan(id: number) {
    this.geselecteerdeNotitieId = id;
  }

  private haalNotitiesOp() {
    this.notitieService.listNotities(this.zaakUuid).subscribe((notities) => {
      this.notities = notities;
      this.notities.sort((a, b) => {
        if (!a.tijdstipLaatsteWijziging) return -1;
        if (!b.tijdstipLaatsteWijziging) return 1;

        return b.tijdstipLaatsteWijziging.localeCompare(
          a.tijdstipLaatsteWijziging,
        );
      });
    });
  }

  protected maakNotitieAan(tekst: string) {
    const loggedInUser = this.loggedInUserQuery.data();
    if (!loggedInUser?.id) return;
    if (tekst.length === 0) return;
    if (tekst.length > this.maxLengteTextArea) return;

    this.notitieService
      .createNotitie({
        zaakUUID: this.zaakUuid,
        tekst: tekst,
        gebruikersnaamMedewerker: loggedInUser.id,
      })
      .subscribe((notitie) => {
        this.notities.splice(0, 0, notitie);
        this.notitieTekst.nativeElement.value = "";
        this.scrollTarget.nativeElement.scrollIntoView({
          behavior: "smooth",
          block: "start",
        });
      });
  }

  protected updateNotitie(notitie: GeneratedType<"RestNote">, tekst: string) {
    const loggedInUser = this.loggedInUserQuery.data();
    if (!loggedInUser?.id) return;

    if (tekst.length === 0) return;
    if (tekst.length > this.maxLengteTextArea) return;

    this.notitieService
      .updateNotitie({
        ...notitie,
        tekst,
        gebruikersnaamMedewerker: loggedInUser.id,
      })
      .subscribe((updatedNotitie) => {
        Object.assign(notitie, updatedNotitie);
        this.geselecteerdeNotitieId = null;
      });
  }

  protected annuleerUpdateNotitie() {
    this.notitieTekst.nativeElement.value = "";
    this.geselecteerdeNotitieId = null;
  }

  protected verwijderNotitie(id: number) {
    this.notitieService.deleteNotitie(id).subscribe(() => {
      this.notities.splice(
        this.notities.findIndex((n) => n.id === id),
        1,
      );
    });
  }
}
