# Construction : la même version de Node que frontend/.nvmrc
FROM node:24.21.0-alpine AS build
WORKDIR /app

# Les dépendances d'abord : cette couche reste en cache tant que le lockfile ne change pas
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# Service : nginx sert l'application et relaie l'API au backend, sur une seule origine
FROM nginx:1.30-alpine
COPY nginx/default.conf /etc/nginx/conf.d/default.conf
COPY nginx/security-headers.conf /etc/nginx/snippets/security-headers.conf
COPY --from=build /app/dist /usr/share/nginx/html
