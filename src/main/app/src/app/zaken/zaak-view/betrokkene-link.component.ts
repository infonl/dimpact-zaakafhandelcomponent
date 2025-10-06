import { Component, computed, input } from "@angular/core";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { lastValueFrom } from "rxjs";
import { GeneratedType } from "src/app/shared/utils/generated-types";
import { buildBedrijfRouteLink } from "../../klanten/klanten-routing.module";
import { KlantenService } from "../../klanten/klanten.service";
import { BetrokkeneIdentificatie } from "../model/betrokkeneIdentificatie";

@Component({
  selector: "betrokkene-link",
  templateUrl: "./betrokkene-link.html",
  styleUrls: [],
})
export class BetrokkeneLinkComponent {
  protected readonly betrokkeneQuery = injectQuery(() => ({
    enabled: !!this.betrokkene(),
    queryKey: [
      this.betrokkene()?.type,
      this.betrokkene()
        ? new BetrokkeneIdentificatie(this.betrokkene()!).uniqueKey
        : null,
    ],
    retry: false,
    queryFn: async () => {
      const betrokkene = this.betrokkene();
      if (!betrokkene) throw new Error("No betrokkene provided");

      if (betrokkene.type === "BSN") {
        const persoon = await lastValueFrom(
          this.klantService.readPersoon(betrokkene.identificatie, {
            context: "BetrokkeneLinkComponent",
            action: "view",
          }),
        );
        return new BetrokkeneIdentificatie(persoon);
      }

      const bedrijf = await lastValueFrom(
        this.klantService.readBedrijf(new BetrokkeneIdentificatie(betrokkene)),
      );
      return new BetrokkeneIdentificatie(bedrijf);
    },
  }));

  protected readonly betrokkene =
    input.required<GeneratedType<"RestZaakBetrokkene">>();

  protected readonly bedrijfRouteLink = computed(() =>
    buildBedrijfRouteLink(this.betrokkene()),
  );

  constructor(private readonly klantService: KlantenService) {}
}
