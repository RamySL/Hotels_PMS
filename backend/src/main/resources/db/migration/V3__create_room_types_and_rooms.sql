CREATE TABLE room_types (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100)  NOT NULL,
    description TEXT,
    base_price  NUMERIC(10,2) NOT NULL,
    capacity    INTEGER       NOT NULL,
    CONSTRAINT ck_room_types_base_price CHECK (base_price > 0),
    CONSTRAINT ck_room_types_capacity CHECK (capacity > 0)
);

-- Nom unique sans tenir compte de la casse : "Suite" et "suite" désignent le même type
CREATE UNIQUE INDEX uk_room_types_name ON room_types (lower(name));

CREATE TABLE rooms (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    room_type_id UUID        NOT NULL,
    room_number  VARCHAR(10) NOT NULL,
    floor        INTEGER     NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'READY',
    CONSTRAINT fk_rooms_room_type FOREIGN KEY (room_type_id) REFERENCES room_types (id),
    CONSTRAINT uk_rooms_room_number UNIQUE (room_number),
    CONSTRAINT ck_rooms_floor CHECK (floor >= 0),
    CONSTRAINT ck_rooms_status CHECK (status IN ('READY', 'OCCUPIED', 'CLEANING', 'OUT_OF_SERVICE'))
);

-- PostgreSQL n'indexe pas automatiquement les clés étrangères
CREATE INDEX ix_rooms_room_type_id ON rooms (room_type_id);
