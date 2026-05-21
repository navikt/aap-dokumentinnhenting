## aap-dokumentinnhenting
Modul for å bestille og hente ut diverse dokumenter fra andre tjenester.

#### P.d.d:
- Bestilling av legeerklæring og dialogmeldinger med tilhørende status via SYFO
- Uthenting av legeerklæring fra JOARK
- Behandleroppslag via SYFO

## Komme i gang
Bruker gradle wrapper, så bare klon og kjør `./gradlew build`

### Github package registry
Miljøvariabelen `GITHUB_TOKEN` må være satt for å hente dependencies fra Github Package Registry.

Den skal være satt til din github personal access token.
Denne opprettes på Github ved å gå til settings -> developer settings. 
Husk `read:packages`-rettighet og enable SSO.

### Kjøre lokalt

Appen har ulike run-konfigurasjoner i IntelliJ.

Konfigurasjonsfilene finner du i mappen `.run/`

#### TestApp

I IntelliJ skal det være plug-n-play med to ulike run-konfigurasjoner for TestApp.

**Med docker-compose** (beholder data mellom app restarts)

Kjør `docker-compose up -d`

Deretter velg konfigurasjonen `TestApp` i Run/Debug-menyen.

**Med Testcontainers** (databasen resettes ved hver app restart)

Velg konfigurasjonen `TestApp (med Testcontainers)` i Run/Debug-menyen.

## Henvendelser
Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub

# For NAV-ansatte
Interne henvendelser kan sendes via Slack i kanalen #ytelse-aap-værsågod.
