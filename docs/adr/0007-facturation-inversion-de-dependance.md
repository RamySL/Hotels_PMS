# 0007 — Facturation découplée des réservations par inversion de dépendance

## Contexte

La facturation dépend des réservations : une facture appartient à une réservation, dont elle affiche le client et la chambre. Les réservations, elles, doivent déclencher la facturation :

- émettre la facture à la création ;
- mettre son montant à jour à chaque modification ;
- l'annuler avec la réservation ;
- exiger qu'elle soit soldée au check-out.

Si `ReservationService` appelait directement `InvoiceService`, les deux packages dépendraient l'un de l'autre.

## Décision

Le package `reservation` définit l'interface dont il a besoin, `ReservationBilling`, avec quatre méthodes : `reservationBooked`, `reservationRescheduled`, `reservationCancelled` et `ensureSettled`. Le package `invoice` l'implémente (`InvoiceService`), et Spring l'injecte dans `ReservationService`. Les appels restent synchrones et se font dans la même transaction.

## Conséquences

- La dépendance ne va que dans un sens : `invoice` → `reservation`.
- Les réservations se testent avec un simple mock de l'interface.
- Un échec côté facture annule toute l'opération : il n'existe pas de réservation sans facture.
- `ArchitectureTest` (ArchUnit) vérifie à chaque build qu'aucun cycle n'existe entre les packages de feature.

## Alternative écartée

Les événements Spring (`ApplicationEventPublisher`) découplent davantage, mais le flux devient implicite. De plus, le refus du check-out quand la facture n'est pas soldée se prête mal à un événement.
