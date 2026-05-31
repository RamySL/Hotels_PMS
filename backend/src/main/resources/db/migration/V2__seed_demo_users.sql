-- Comptes de démonstration, publics par nature :
--   admin@pms.local     / Admin123!      (ADMIN)
--   reception@pms.local / Reception123!  (RECEPTIONIST)
-- UUID fixes : les données de démo des phases suivantes pourront y faire référence.
INSERT INTO users (id, email, password_hash, first_name, last_name, role) VALUES
    ('1cfd0fb0-2d26-4835-bec8-55f9317b0b14', 'admin@pms.local',
     '$2a$10$CTuPRLxmr6cG/bRAVavvoOP.JSqrMq63bQ2B3/95WuJJKrKSXEKba', 'Camille', 'Roux', 'ADMIN'),
    ('df376895-62fc-4376-a4bb-4de71365aa54', 'reception@pms.local',
     '$2a$10$cG37f./0278cYK3jfx25EeIbnBwOvlc9wblUV0wYG5euSUfcJ9MTC', 'Lucas', 'Bernard', 'RECEPTIONIST');
