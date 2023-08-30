-- ZAC database initialization script. To be run only once.
-- Creates the required schemas and grants the required permissions.
-- Assumes the database user is 'zac'.

CREATE SCHEMA flowable;
GRANT CREATE, USAGE ON SCHEMA flowable TO zac;

CREATE SCHEMA zaakafhandelcomponent;
GRANT CREATE, USAGE ON SCHEMA zaakafhandelcomponent to zac;

