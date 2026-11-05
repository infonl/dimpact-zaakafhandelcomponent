/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

INSERT INTO ${schema}.mail_template(id_mail_template, mail_template_naam, onderwerp, body, mail_template_enum, default_mailtemplate)
VALUES (nextval('${schema}.sq_mail_template'), 'Signalering taak afgerond', '<p>Een taak voor zaak {ZAAK_NUMMER} is afgerond</p>',
        '<p>Hoi!</p><p>{TAAK_BEHANDELAAR_MEDEWERKER} heeft een taak afgerond voor zaak {ZAAK_NUMMER} over {ZAAK_OMSCHRIJVING}.</p><p>{TAAK_LINK}</p>',
        'SIGNALERING_TAAK_AFGEROND', true);
