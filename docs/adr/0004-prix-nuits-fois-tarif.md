# 0004 — Prix d'un séjour : nuits × tarif du type de chambre

## Contexte

Dans un hôtel, les tarifs varient selon la saison, le jour de la semaine ou les promotions. Le MVP doit d'abord rendre fiables les réservations et la facturation.

## Décision

Le prix d'un séjour est le nombre de nuits multiplié par le prix de base du type de chambre (`Reservation.priceOf`). Il est recalculé à chaque modification de la réservation, et la facture suit ce prix.

## Conséquences

- La règle est simple et entièrement testable.
- Le prix est enregistré dans la réservation : changer le tarif d'un type de chambre ne touche que les réservations créées ou modifiées ensuite.
- La tarification dynamique (saisons, week-ends) est repoussée en V2.
