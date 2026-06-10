CREATE TABLE clients (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name  VARCHAR(100) NOT NULL,
    last_name   VARCHAR(100) NOT NULL,
    email       VARCHAR(255),
    phone       VARCHAR(30),
    -- Champs recherchables en minuscules et sans accents, calculés par l'application
    search_text TEXT         NOT NULL,
    CONSTRAINT ck_clients_email_lowercase CHECK (email = lower(email))
);
