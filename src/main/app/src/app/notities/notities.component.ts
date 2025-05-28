/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, ElementRef, Input, OnInit, ViewChild } from "@angular/core";
import { IdentityService } from "../identity/identity.service";
import { GeneratedType } from "../shared/utils/generated-types";
import { Notitie } from "./model/notitie";
import { NotitieService } from "./notities.service";

@Component({
  selector: "zac-notities",
  templateUrl: "./notities.component.html",
  styleUrls: ["./notities.component.less"],
})
export class NotitiesComponent implements OnInit {
  @Input({ required: true }) zaakUuid!: string;
  @Input({ required: true }) notitieType!: string;
  @Input() notitieRechten?: GeneratedType<"RestNotitieRechten">;

  @ViewChild("scrollTarget") scrollTarget!: ElementRef;
  @ViewChild("notitieTekst") notitieTekst!: ElementRef;

  ingelogdeMedewerker?: GeneratedType<"RestLoggedInUser">;

  notities: Notitie[] = [];
  showNotes = true;
  geselecteerdeNotitieId: number | null = null;
  maxLengteTextArea = 1000;

  constructor(
    private identityService: IdentityService,
    private notitieService: NotitieService,
  ) {}

  ngOnInit(): void {
    this.identityService.readLoggedInUser().subscribe((ingelogdeMedewerker) => {
      this.ingelogdeMedewerker = ingelogdeMedewerker;
    });
    this.haalNotitiesOp();
  }

  toggleNotitieContainer() {
    this.showNotes = !this.showNotes;
  }

  pasNotitieAan(id: number) {
    this.geselecteerdeNotitieId = id;
  }

  haalNotitiesOp() {
    this.notitieService
      .listNotities(this.notitieType, this.zaakUuid)
      .subscribe((notities) => {
        this.notities = notities;
        this.notities
          .sort((a, b) =>
            a.tijdstipLaatsteWijziging.localeCompare(
              b.tijdstipLaatsteWijziging,
            ),
          )
          .reverse();
      });
  }

  maakNotitieAan(tekst: string) {
    if (!this.ingelogdeMedewerker?.id) return;

    if (tekst.length <= this.maxLengteTextArea) {
      const notitie: Notitie = new Notitie();
      notitie.zaakUUID = this.zaakUuid;
      notitie.tekst = tekst;
      notitie.gebruikersnaamMedewerker = this.ingelogdeMedewerker.id;

      this.notitieService.createNotitie(notitie).subscribe((notitie) => {
        this.notities.splice(0, 0, notitie);
        this.notitieTekst.nativeElement.value = "";
        this.scrollTarget.nativeElement.scrollIntoView({
          behavior: "smooth",
          block: "start",
        });
      });
    }
  }

  updateNotitie(notitie: Notitie, notitieTekst: string) {
    if (!this.ingelogdeMedewerker?.id) return;

    if (notitieTekst.length <= this.maxLengteTextArea) {
      notitie.tekst = notitieTekst;
      notitie.gebruikersnaamMedewerker = this.ingelogdeMedewerker.id;
      this.notitieService.updateNotitie(notitie).subscribe((updatedNotitie) => {
        Object.assign(notitie, updatedNotitie);
        this.geselecteerdeNotitieId = null;
      });
    }
  }

  annuleerUpdateNotitie() {
    this.notitieTekst.nativeElement.value = "";
    this.geselecteerdeNotitieId = null;
  }

  verwijderNotitie(id: number) {
    this.notitieService.deleteNotitie(id).subscribe(() => {
      this.notities.splice(
        this.notities.findIndex((n) => n.id === id),
        1,
      );
    });
  }
}
