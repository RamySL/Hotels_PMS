# 0006 — Double réservation impossible, garantie par la base

## Contexte

Deux réceptionnistes peuvent réserver la même chambre au même moment. Une vérification par le service, qui lit les réservations existantes puis enregistre la nouvelle, ne protège pas de cette concurrence : les deux lectures peuvent passer avant les deux écritures.

## Décision

Une contrainte d'exclusion PostgreSQL, `ex_reservations_no_overlap` (`EXCLUDE USING gist`, extension `btree_gist`), interdit qu'une même chambre (`room_id WITH =`) ait deux réservations actives (`CONFIRMED` ou `CHECKED_IN`) dont les périodes `daterange(check_in_date, check_out_date)` se chevauchent (`&&`).

Le service vérifie aussi avant d'enregistrer (`AvailabilityService`), pour donner un message clair. Si la base refuse malgré tout, la violation devient la même erreur 409.

## Conséquences

- La règle tient dans tous les cas, même en cas d'accès concurrent ou d'écriture faite hors de l'application.
- Les bornes `[)` du `daterange` permettent un départ et une arrivée le même jour dans la même chambre.
- Le projet dépend de PostgreSQL : les tests tournent sur un vrai PostgreSQL (Testcontainers), pas sur H2.

## Alternatives écartées

- La vérification par le service seule : elle ne suffit pas en cas d'accès concurrent.
- Un verrou pessimiste sur la chambre : il faudrait le poser dans chaque chemin de code qui réserve ou modifie, et il sérialiserait toutes les réservations de la chambre.
