/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, ElementRef, Input, OnInit, ViewChild } from "@angular/core";
import { IdentityService } from "../identity/identity.service";
import { GeneratedType } from "../shared/utils/generated-types";
import { NotitieService } from "./notities.service";

@Component({
  selector: "zac-notities",
  templateUrl: "./notities.component.html",
  styleUrls: ["./notities.component.less"],
})
export class NotitiesComponent implements OnInit {
  @Input({ required: true }) zaakUuid!: string;
  @Input() notitieRechten?: GeneratedType<"RestNotitieRechten">;

  @ViewChild("notitieTekst") notitieTekst!: {
    nativeElement: HTMLTextAreaElement;
  };
  @ViewChild("scrollTarget") scrollTarget!: ElementRef;

  ingelogdeMedewerker?: GeneratedType<"RestLoggedInUser">;

  notities: GeneratedType<"RestNote">[] = [];
  showNotes = false;

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
    console.log("Notitie container toggled:", this.showNotes);
  }

  pasNotitieAan(id: number) {
    this.geselecteerdeNotitieId = id;
  }

  haalNotitiesOp() {
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

  maakNotitieAan(tekst: string) {
    if (!this.ingelogdeMedewerker?.id) return;
    if (tekst.length === 0) return;
    if (tekst.length > this.maxLengteTextArea) return;

    this.notitieService
      .createNotitie({
        zaakUUID: this.zaakUuid,
        tekst,
        gebruikersnaamMedewerker: this.ingelogdeMedewerker.id,
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

  updateNotitie(notitie: GeneratedType<"RestNote">, tekst: string) {
    if (!this.ingelogdeMedewerker?.id) return;

    if (tekst.length === 0) return;
    if (tekst.length > this.maxLengteTextArea) return;

    this.notitieService
      .updateNotitie({
        ...notitie,
        tekst,
        gebruikersnaamMedewerker: this.ingelogdeMedewerker.id,
      })
      .subscribe((updatedNotitie) => {
        Object.assign(notitie, updatedNotitie);
        this.geselecteerdeNotitieId = null;
      });
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
