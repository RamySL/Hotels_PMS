# Décisions d'architecture (ADR)

Chaque décision structurante du projet est décrite dans un fichier court : son contexte, la décision prise et ses conséquences. Une décision n'est pas réécrite après coup : si elle change, une nouvelle ADR la remplace.

| N° | Décision | Étape du projet |
|---|---|---|
| [0001](0001-organisation-par-feature.md) | Backend organisé par feature | Conception |
| [0002](0002-jwt-sans-refresh-token.md) | JWT sans refresh token | Conception |
| [0003](0003-identifiants-uuid.md) | Identifiants UUID | Conception |
| [0004](0004-prix-nuits-fois-tarif.md) | Prix d'un séjour : nuits × tarif du type de chambre | Conception |
| [0005](0005-validation-jwt-resource-server.md) | Validation des JWT par le Resource Server de Spring Security | Authentification |
| [0006](0006-double-reservation-contrainte-exclusion.md) | Double réservation impossible, garantie par la base | Réservations |
| [0007](0007-facturation-inversion-de-dependance.md) | Facturation découplée des réservations par inversion de dépendance | Facturation |
