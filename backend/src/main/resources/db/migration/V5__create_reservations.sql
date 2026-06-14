-- Permet de combiner l'égalité sur room_id (uuid) et le chevauchement de dates dans une
-- même contrainte d'exclusion (index GiST)
CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE reservations (
    id               UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id        UUID          NOT NULL,
    room_id          UUID          NOT NULL,
    created_by       UUID          NOT NULL,
    check_in_date    DATE          NOT NULL,
    check_out_date   DATE          NOT NULL,
    number_of_guests INTEGER       NOT NULL,
    total_price      NUMERIC(10,2) NOT NULL,
    status           VARCHAR(20)   NOT NULL,
    created_at       TIMESTAMPTZ   NOT NULL,
    CONSTRAINT fk_reservations_client FOREIGN KEY (client_id) REFERENCES clients (id),
    CONSTRAINT fk_reservations_room FOREIGN KEY (room_id) REFERENCES rooms (id),
    CONSTRAINT fk_reservations_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT ck_reservations_dates CHECK (check_out_date > check_in_date),
    CONSTRAINT ck_reservations_number_of_guests CHECK (number_of_guests > 0),
    CONSTRAINT ck_reservations_total_price CHECK (total_price > 0),
    CONSTRAINT ck_reservations_status CHECK (status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT', 'CANCELLED')),
    -- Double réservation impossible (ADR-006) : pas deux réservations actives sur la même
    -- chambre pour des nuits communes. daterange est semi-ouvert [arrivée, départ) : un
    -- départ et une arrivée le même jour ne se chevauchent pas.
    CONSTRAINT ex_reservations_no_overlap EXCLUDE USING gist (
        room_id WITH =,
        daterange(check_in_date, check_out_date) WITH &&
    ) WHERE (status IN ('CONFIRMED', 'CHECKED_IN'))
);

-- room_id est déjà indexé par la contrainte d'exclusion (pour les réservations actives)
CREATE INDEX ix_reservations_client_id ON reservations (client_id);
