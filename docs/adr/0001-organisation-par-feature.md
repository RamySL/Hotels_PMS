# 0001 — Backend organisé par feature

## Contexte

Le backend couvre plusieurs domaines métier : chambres, clients, réservations, facturation et tableau de bord. Une organisation par couche technique (`controllers/`, `services/`, `repositories/`) disperse chaque notion dans plusieurs dossiers, et rien n'y montre quel code appartient à quel domaine.

## Décision

Un package par domaine sous `com.pms.hotel` : `room`, `client`, `reservation`, `invoice`, `dashboard`, `auth` et `user`. Chacun regroupe son contrôleur, son service, son repository, ses entités et ses DTO. Deux packages sont transverses : `common` (erreurs au format RFC 7807) et `config` (sécurité, horloge, documentation de l'API).

## Conséquences

- Une fonctionnalité se lit et se modifie dans un seul dossier.
- La visibilité de package redevient utile : ce qui ne sert qu'à un domaine n'est pas public, comme `ReservationSpecifications` ou le constructeur de `Payment`.
- Les domaines dépendent les uns des autres, puisqu'une réservation concerne une chambre et un client. Ces dépendances ne doivent pas former de cycle (voir [0007](0007-facturation-inversion-de-dependance.md)).
