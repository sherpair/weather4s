CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;

CREATE TYPE ROLE AS ENUM ('Master', 'Member');

CREATE TABLE IF NOT EXISTS members (
          id BIGSERIAL PRIMARY KEY,
  account_id      TEXT NOT NULL UNIQUE,
  first_name      TEXT NOT NULL,
   last_name      TEXT NOT NULL,
       email      TEXT NOT NULL UNIQUE,
      geo_id      TEXT NOT NULL,
     country      TEXT NOT NULL,
      secret      TEXT NOT NULL,
      active      BOOL DEFAULT false NOT NULL,
        role      ROLE DEFAULT 'Member' NOT NULL,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (now() AT TIME ZONE 'UTC')
);

CREATE TABLE IF NOT EXISTS tokens (
          id BIGSERIAL PRIMARY KEY,
    token_id      TEXT NOT NULL UNIQUE,
   member_id    BIGINT NOT NULL REFERENCES members(id) ON DELETE CASCADE,
 expiry_date TIMESTAMP NOT NULL,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (now() AT TIME ZONE 'UTC')
);
