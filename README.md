# Projet microservices Kubernetes

Application simple de gestion de commandes, composée de 2 microservices Java (Spring Boot).

## Architecture cible

```
                    Ingress (gateway)
                          |
         +----------------+----------------+
         |                                 |
   order-service  ------ REST ------>  catalog-service
   (commandes)    "le produit existe ?"   (produits, stock)
         |                                 ^
         |  événement "commande créée"     |
         +---------->  RabbitMQ  ----------+
                                 catalog-service diminue le stock
```

- Communication synchrone (REST) : order-service interroge catalog-service avant d'accepter une commande.
- Communication asynchrone (file d'événements) : order-service publie un événement dans RabbitMQ, catalog-service le consomme et met à jour le stock.

| Étape | Contenu | Statut |
|---|---|---|
| Étape 1 | catalog-service : Docker, Docker Hub, Deployment, Service | fait |
| Étape 2 | Gateway Ingress | à faire |
| Étape 3 | order-service : appel REST vers catalog-service | fait |
| Étape 3 bis | Événements RabbitMQ entre order-service et catalog-service | à faire |
| Étape 4 | Base de données | à faire |
| Étape 5 | Sécurité (RBAC, etc.) | à faire |

## catalog-service

API REST (port 8080) :

| Méthode | Route | Rôle |
|---|---|---|
| GET | / | nom du service, version, nom du Pod |
| GET | /products | liste des produits |
| GET | /products/{id} | un produit |
| POST | /products | créer un produit `{"name": "...", "price": 10, "stock": 5}` |
| GET | /actuator/health | état du service (sondes Kubernetes) |

### Docker

La compilation Maven et les tests se font dans le `docker build` : il n'est pas nécessaire d'installer Java ou Maven.

```
cd catalog-service
docker build -t herador/catalog-service:1 .
docker run -p 4000:8080 herador/catalog-service:1
docker push herador/catalog-service:1
```

### Kubernetes

```
kubectl apply -f k8s/catalog-deployment.yml
kubectl apply -f k8s/catalog-service.yml
kubectl get pods
minikube service catalog-service --url
```

## order-service

API REST (port 8080). Avant d'enregistrer une commande, order-service appelle `GET /products/{id}` sur catalog-service. L'adresse de catalog-service est fournie par la variable d'environnement `CATALOG_URL`.

| Méthode | Route | Rôle |
|---|---|---|
| GET | / | nom du service, version, nom du Pod |
| GET | /orders | liste des commandes |
| GET | /orders/{id} | une commande |
| POST | /orders | créer une commande `{"productId": 1, "quantity": 2}` |
| GET | /actuator/health | état du service (sondes Kubernetes) |

Réponses de `POST /orders` : `201` commande créée, `400` requête invalide, `404` produit inconnu, `409` stock insuffisant, `503` catalog-service injoignable.

### Docker et Kubernetes

```
cd order-service
docker build -t herador/order-service:1 .
docker push herador/order-service:1
cd ..
kubectl apply -f k8s/order-deployment.yml
kubectl apply -f k8s/order-service.yml
minikube service order-service --url
```
