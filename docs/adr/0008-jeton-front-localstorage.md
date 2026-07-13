# 0008 — Jeton de connexion conservé dans le localStorage du navigateur

## Contexte

L'API authentifie chaque requête par un jeton JWT, envoyé dans l'en-tête `Authorization` (voir [0002](0002-jwt-sans-refresh-token.md) et [0005](0005-validation-jwt-resource-server.md)). Le frontend doit garder ce jeton d'une requête à l'autre, et d'un rechargement de la page à l'autre. À la réception, un même poste sert à plusieurs personnes au fil des vacations, et chacune peut travailler dans plusieurs onglets.

## Décision

Le jeton, le profil de la personne connectée et l'heure d'expiration du jeton sont gardés dans le `localStorage`, sous la clé `pms.session`. Quatre règles encadrent ce stockage :

- la session se termine d'elle-même à l'expiration du jeton, au bout de 8 heures ;
- un refus du jeton par l'API (401) la termine aussi ;
- une connexion ou une déconnexion se répercute dans les autres onglets ;
- le nom de la personne connectée reste affiché, avec un bouton « Se déconnecter », et chacun se déconnecte à la relève.

C'est la solution la plus simple pour le MVP : elle ne demande aucun changement côté backend.

## Conséquences

- **Faille XSS (injection de script) : le risque principal.** Un script injecté dans la page pourrait lire le jeton et agir au nom de la personne jusqu'à son expiration, puisqu'un jeton ne peut pas être révoqué. Plusieurs mesures réduisent ce risque :
  - React échappe le texte affiché, et le code n'utilise pas `dangerouslySetInnerHTML` ;
  - aucun script tiers n'est chargé ;
  - les dépendances sont tenues à jour et contrôlées par `npm audit` ;
  - le jeton expire au bout de 8 heures ;
  - une politique `Content-Security-Policy` sera posée par le serveur web du front.
- **Poste partagé.** La session survit à la fermeture du navigateur. Une personne qui oublie de se déconnecter laisse la suivante agir sous son nom, et les paiements et réservations de celle-ci lui sont alors attribués.
- **Pas de révocation.** Se déconnecter efface le jeton du navigateur, mais un jeton déjà copié reste valable jusqu'à son expiration.
- **Accès au poste.** Toute personne qui utilise le navigateur ouvert peut lire le `localStorage`, dans les outils de développement ou par une extension.
- **Pas d'exposition au CSRF (requête forgée depuis un autre site).** Le jeton voyage dans un en-tête que le navigateur n'envoie jamais de lui-même, contrairement à un cookie.

## Alternatives écartées

- **`sessionStorage`** : fermer le navigateur déconnecte, mais chaque nouvel onglet redemande la connexion. L'exposition à une faille XSS est la même.
- **Jeton en mémoire seulement** : rien n'est stocké, mais chaque rechargement de la page déconnecte.
- **Cookie `HttpOnly`, `Secure` et `SameSite`, posé par le backend** : le jeton devient illisible par JavaScript, ce qui empêche son vol par XSS ; une XSS pourrait toutefois toujours agir depuis la page. Il faut alors une protection CSRF côté backend. C'est la piste pour une mise en production, éventuellement avec un jeton d'accès court gardé en mémoire et un refresh token en cookie `HttpOnly`.
