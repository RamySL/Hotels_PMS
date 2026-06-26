CREATE TABLE invoices (
    id             UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    reservation_id UUID          NOT NULL,
    amount_total   NUMERIC(10,2) NOT NULL,
    amount_paid    NUMERIC(10,2) NOT NULL DEFAULT 0,
    status         VARCHAR(20)   NOT NULL,
    issued_at      TIMESTAMPTZ   NOT NULL,
    -- Verrouillage optimiste (@Version) : deux encaissements simultanés ne s'écrasent pas
    version        BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT fk_invoices_reservation FOREIGN KEY (reservation_id) REFERENCES reservations (id),
    CONSTRAINT uk_invoices_reservation UNIQUE (reservation_id),
    CONSTRAINT ck_invoices_amount_total CHECK (amount_total >= 0),
    CONSTRAINT ck_invoices_amount_paid CHECK (amount_paid >= 0),
    CONSTRAINT ck_invoices_status CHECK (status IN ('UNPAID', 'PARTIALLY_PAID', 'PAID', 'CANCELLED'))
);

CREATE TABLE payments (
    id             UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id     UUID          NOT NULL,
    recorded_by    UUID          NOT NULL,
    amount         NUMERIC(10,2) NOT NULL,
    payment_method VARCHAR(20)   NOT NULL,
    paid_at        TIMESTAMPTZ   NOT NULL,
    CONSTRAINT fk_payments_invoice FOREIGN KEY (invoice_id) REFERENCES invoices (id),
    CONSTRAINT fk_payments_recorded_by FOREIGN KEY (recorded_by) REFERENCES users (id),
    CONSTRAINT ck_payments_amount CHECK (amount > 0),
    CONSTRAINT ck_payments_method CHECK (payment_method IN ('CASH', 'CARD', 'TRANSFER'))
);

CREATE INDEX ix_payments_invoice_id ON payments (invoice_id);

-- Une facture par réservation : les réservations déjà enregistrées reçoivent la leur
INSERT INTO invoices (reservation_id, amount_total, status, issued_at)
SELECT id, total_price, CASE WHEN status = 'CANCELLED' THEN 'CANCELLED' ELSE 'UNPAID' END, created_at
FROM reservations;
