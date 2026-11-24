## Objectif

Rendre la CI des deux services (catalog-service et order-service, Java / Spring Boot / Maven) observable, reproductible et bloquante.

Adaptation du TP : les deux services étant en Java, Checkstyle remplace Ruff, et Maven (`mvnw`) remplace Gradle. Les sept contrôles requis sont conservés.

## 0. Point de départ (checkpoint/00-starter)

Un workflow par service, avec un seul job `build` : tests Maven, puis construction de l'image Docker.

| | Job build (catalog-service et order-service) |
|---|---|
| Risque couvert | Régression du code (JUnit), Dockerfile cassé |
| Entrées | Code et pom.xml du service, Dockerfile, JDK 21, dépendances Maven Central, images de base |
| Sortie | Statut vert ou rouge uniquement, image jetée |
| Coût | Environ 36 s par service, sans cache, tests exécutés deux fois (runner puis docker build) |

## 1. Observation d'un échec

Assertion volontairement cassée dans `ProductControllerTest.listeDesProduits`, puis rétablie par `git revert` (commits c741eda et 53ff123).

1. Job en échec : `catalog-service / build`, étape Tests. L'étape Image Docker est sautée : l'état du Dockerfile reste inconnu. `order-service` passe.
2. Ligne utile : `ProductControllerTest.listeDesProduits:33 JSON path "$[0].name" expected:<Clavier-FAUX> but was:<Clavier>`. Elle doit être cherchée dans le log : l'annotation indique seulement `exit code 1`.
3. Rapport structuré : aucun. Les rapports Surefire restent sur le runner et disparaissent avec lui.
4. Délai de retour : 36 s pour le job (dont 23 s de tests), plus l'attente du runner.

## 2. Portes qualité

- Checkstyle 10.26.1 via `maven-checkstyle-plugin` 3.6.0 (la version embarquée par défaut, 9.3, est remplacée).
- Configuration partagée par les deux services : `config/checkstyle/checkstyle.xml`.
- Règles : imports inutilisés, redondants ou en étoile ; accolades obligatoires ; instructions vides ; longueur de ligne (120).
- Exceptions : aucune règle désactivée globalement. Une exception justifiée se déclare localement avec `@SuppressWarnings("checkstyle:NomDeLaRegle")` (SuppressWarningsFilter). Aucune exception n'a été nécessaire.
- Le plugin n'est pas rattaché au cycle de vie Maven : le `docker build` ne l'exécute pas et ne dépend donc pas du dossier `config/`, situé hors du contexte Docker.

Commande : `./mvnw -B checkstyle:check` dans chaque service.

## 3. Graphe CI

Pour chaque service :

```text
tests ────┐
          ├──> image Docker
qualité ──┘
```

| Contrainte | Mise en oeuvre |
|---|---|
| Timeout explicite | 10 min (tests, qualité), 15 min (image), 5 min (secrets) |
| Permissions minimales | `permissions: contents: read` au niveau du workflow |
| Dépendance avant construction | `needs: [tests, quality]` sur le job image |
| Cache | `setup-java` avec `cache: maven` ; cache Buildx `type=gha` avec un `scope` par service |
| Annulation des exécutions obsolètes | `concurrency` par workflow et par ref, `cancel-in-progress: true` |
| Pas de publication depuis une PR | connexion et `push` Docker Hub uniquement sur `push` vers `main` |

Les tests ne sont plus exécutés deux fois : le Dockerfile accepte `SKIP_TESTS`, passé à `true` en CI puisque le job tests les a déjà exécutés. En local, la valeur par défaut garde les tests dans le build.

Filtre `paths` : aucun filtre sur `pull_request`. Un workflow ignoré par `paths` ne publie aucun statut : un contrôle requis resterait « Expected » et la PR ne pourrait jamais être fusionnée. Le filtre n'est appliqué que sur `push` vers `main`, pour ne pas reconstruire ni republier un service non modifié.

## 4. Preuves

Rapports téléversés avec `actions/upload-artifact`, `if: always()` et `retention-days: 7` :

- `catalog-test-reports`, `order-test-reports` : XML Surefire (JUnit)
- `catalog-checkstyle-report`, `order-checkstyle-report` : XML Checkstyle
- `gitleaks-report` : SARIF

Vérification par un échec volontaire (import inutilisé dans `OrderController`, commits a154966 et d2f6916) : `Order quality` rouge, `Order tests` vert, `Order Docker image` sauté, et `order-checkstyle-report` téléchargeable avec la violation `Unused import - java.util.Set`.

## 5. Détection de secrets

Workflow `secret-detection`, job `Secret detection` :

- `fetch-depth: 0` : tout l'historique est analysé, pas seulement le dernier état du code.
- Binaire officiel Gitleaks 8.30.1, version épinglée, somme SHA-256 vérifiée.
- `--redact` : les valeurs détectées sont masquées dans le log et le rapport.
- Bornage : jeton en lecture seule, timeout de 5 min, rapport conservé 7 jours.

Le binaire a été préféré à `gitleaks-action` : il garde les permissions à `contents: read` et rend explicite ce qui est exécuté.

Test avec la valeur factice fournie par l'enseignant, sur une branche jetable et une PR en brouillon : `Secret detection` en échec, valeur masquée dans le log. La PR a été fermée sans fusion et la branche supprimée.

Distinction des réponses à une fuite :

- Supprimer la ligne fautive : corrige seulement la version actuelle. Le secret reste dans l'historique, les clones et les logs. Insuffisant.
- Révoquer le secret : seule action qui le rend inutilisable (désactivation chez le fournisseur, nouveau secret, mise à jour du secret GitHub). À faire en premier.
- Réécrire l'historique (`git filter-repo` puis `push --force`) : retire le secret des branches, mais pas des clones, des forks ni des références de PR conservées par GitHub. Oblige les autres développeurs à recloner. Complément de la révocation, jamais un remplacement.

## 6. Reproduction locale

```bash
python scripts/course.py verify
```

`verify` exécute `test` puis `lint` sur les deux services, s'arrête au premier échec et retourne 1. Vérifié : code 0 quand tout passe, code 1 avec un import inutilisé dans `OrderController`.

`verify` ne construit pas les images, ne lance pas Gitleaks et ne publie pas les rapports : ces responsabilités restent dans GitHub Actions.

## 7. Protection de main

Ruleset `protection-main` sur la branche par défaut, sans liste de contournement :

- pull request obligatoire avant fusion ;
- contrôles requis : `Catalog tests`, `Catalog quality`, `Catalog Docker image`, `Order tests`, `Order quality`, `Order Docker image`, `Secret detection` ;
- suppression et force push interdits.

Vérifié : une PR avec un test cassé est bloquée (« Merging is blocked due to failing merge requirements »), la PR verte est fusionnable.
