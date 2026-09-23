# ZAC Release 5.6 🚀
Deze release bevat belangrijke verbeteringen op het gebied van gebruikerservaring, met name door het direct bijwerken van het zaakscherm na wijzigingen. Daarnaast zijn er voorbereidingen getroffen voor zaakspecifieke autorisaties en zijn diverse BPMN-functionaliteiten verbeterd. 

### Verbeteringen ✨ 
* **Directe scherm-updates**: Na het opslaan van een wijziging op een zaak (zoals afsluiten, heropenen of details wijzigen) wordt het zaakscherm nu direct bijgewerkt zonder te wachten op een notificatie. De "zaak is gewijzigd" melding verschijnt niet meer voor eigen wijzigingen. 
* **Zaakspecifieke autorisatie**: Voorbereidingen in de backend en frontend om aan te geven of een zaak zaakspecifiek geautoriseerd is (via de `ZAAK_GEAUTORISEERD` eigenschap). 
* **BPMN E2E testen**: De opzet voor end-to-end testen van BPMN-processen is verbeterd en gestabiliseerd. 
* **Zoekfunctionaliteit**: Verbeteringen in de zoekresultaten bij gerelateerde zaken en ondersteuning voor paging bij het ophalen van zaaktypen uit Open Zaak. 
* **Beheer**: Nieuwe Python-scripts voor het toevoegen van meerdere zaaktypen voor lokale performance-testen en het aanmaken van zaken met documenten. 

### Bug fixes 🐞
* **BPMN Datumwijzigingen**: Fix voor een probleem waarbij start-, streef- en fatale datums niet gewijzigd konden worden in BPMN-zaken. 
* **BPMN Read-only status**: Fix waarbij BPMN-taken na afronden niet direct op read-only sprongen zonder pagina-refresh. 
* **Inbox Documenten**: Verbeterde afhandeling van documenten die al aan een zaak gekoppeld zijn, zodat deze niet onterecht in de inbox blijven staan. 
* **Datumformattering**: Datumvelden voor zoekopdrachten gebruiken nu de Nederlandse formattering (dd-mm-jjjj) in plaats van de Amerikaanse. 
* **Dubbele verzending**: Extra beveiliging toegevoegd op diverse formulieren om te voorkomen dat acties dubbel worden uitgevoerd door herhaaldelijk klikken. 
* **Document metadata**: Bij het toevoegen van meerdere documenten achter elkaar wordt de metadata van het vorige document nu bewaard. 

### Security 🛡️
* **JavaScript in Form.io**: Documentatie toegevoegd over het veilig gebruik van JavaScript in Form.io formulieren, met het advies om waar mogelijk JSON Logic te gebruiken. 
* **Vulnerability patches**: Diverse library-overrides toegevoegd om bekende kwetsbaarheden te verhelpen. 
* **Keycloak**: Upgrade naar versie 26.6.4/26.7.2 voor verbeterde beveiliging en stabiliteit.

### Documentatie 📘
* **Gebruikershandleiding**: Bijgewerkt met instructies voor het zetten van de brondatum door de recordmanager. 
* **BPMN Gids**: Uitgebreid met informatie over JavaScript-gebruik en nieuwe E2E testopzet. 
* **Afbeeldingen**: Diverse afbeeldingen in de handleiding zijn verduidelijkt met gemarkeerde kaders. 

### Library updates 📦 
* **Angular**: v20.3.33
* **Tanstack Query**: v5.102.8 
* **Keycloak Admin Client**: v26.0.12 
* **Flyway**: v13.4.0 
* **Open Zaak**: v1.29 (ondersteuning) 
* **Open Object**: v4.1.1 
* **Open Notificaties**: v1.16.2 
* **Typescript**: v5.9.3 
* **Node.js**: v24.20.0 

### Platform compatibaliteit 😎 
* **Open Zaak**: 1.29.x 
* **Open Object**: 4.1.x 
* **Open Notificaties**: 1.16.x 
* **Keycloak**: 26.7.x
