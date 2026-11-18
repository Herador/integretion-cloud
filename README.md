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
| 1 (10/20) | catalog-service : Docker, Docker Hub, Deployment, Service | fait |
| 2 (12/20) | Gateway Ingress | à faire |
| 3 (14/20) | order-service : appel REST vers catalog-service + événements RabbitMQ | à faire |
| 4 (16/20) | Base de données | à faire |
| 5 (18/20) | Sécurité (RBAC, etc.) | à faire |

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
