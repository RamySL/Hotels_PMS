# 0002 — JWT sans refresh token

## Contexte

L'API sert une application web et doit rester sans état côté serveur. Le personnel de réception travaille par vacations. Un refresh token oblige à stocker les jetons, à les renouveler et à pouvoir les révoquer.

## Décision

Un seul jeton d'accès JWT, valable 8 heures (`pms.jwt.expiration`), soit une vacation. Pas de refresh token dans le MVP.

## Conséquences

- Le mécanisme reste simple : à l'expiration du jeton, l'utilisateur se reconnecte.
- Un jeton ne peut pas être révoqué avant son expiration : un jeton volé reste utilisable jusqu'à 8 heures.
- Le refresh token et la double authentification sont repoussés en V2.
