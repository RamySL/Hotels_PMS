# 0005 — Validation des JWT par le Resource Server de Spring Security

## Contexte

Chaque requête doit présenter un JWT valide. La conception prévoyait au départ un filtre écrit à la main avec la bibliothèque jjwt. Or jjwt-jackson s'appuie sur Jackson 2, alors que Spring Boot 4 utilise Jackson 3.

## Décision

Le projet utilise `spring-boot-starter-oauth2-resource-server` :

- `JwtService` émet les jetons avec `NimbusJwtEncoder`, et `SecurityConfig` les valide avec `NimbusJwtDecoder`, en HS256 et en vérifiant l'émetteur.
- Le rôle est porté par le claim `role`, converti en autorité `ROLE_…` pour `@PreAuthorize`.
- Le secret `JWT_SECRET` (256 bits au moins) est obligatoire : sans lui, l'application refuse de démarrer.

## Conséquences

- Moins de code de sécurité écrit à la main, et aucune bibliothèque tierce.
- Les erreurs d'authentification passent par le `GlobalExceptionHandler`, au même format RFC 7807 que les autres erreurs.
- Passer plus tard à une clé asymétrique ou à un serveur d'autorisation ne changera que la configuration.

## Alternative écartée

Un filtre `OncePerRequestFilter` écrit à la main avec jjwt : plus de code de sécurité à maintenir, et Jackson 2 à côté de Jackson 3.
