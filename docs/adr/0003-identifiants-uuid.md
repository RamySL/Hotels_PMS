# 0003 — Identifiants UUID

## Contexte

Les identifiants apparaissent dans les URL de l'API, par exemple `/api/clients/{id}`. Avec un auto-incrément, ils se devinent, et ils révèlent le volume d'activité de l'hôtel.

## Décision

Toutes les clés primaires sont des UUID, générés par l'application (`GenerationType.UUID`). Les tables ont aussi `gen_random_uuid()` comme valeur par défaut, pour les données insérées par les migrations.

## Conséquences

- On ne peut pas parcourir les ressources en incrémentant un identifiant.
- Les identifiants ne révèlent ni l'ordre de création ni le volume d'activité.
- En contrepartie, ils occupent 16 octets au lieu de 8, se lisent mal dans les logs, et donnent des index moins compacts, car leurs valeurs sont aléatoires.
