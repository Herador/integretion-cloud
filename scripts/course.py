"""Commandes locales portables du projet (Windows, Linux, macOS).

Utilisation :
    python scripts/course.py <commande>

Le script choisit automatiquement mvnw.cmd sous Windows et ./mvnw ailleurs,
puis exécute Maven dans le dossier du service concerné.
Il s'arrête au premier échec et retourne le code 1.
"""

import json
import os
import shutil
import subprocess
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SERVICES = {
    "catalog": ROOT / "catalog-service",
    "order": ROOT / "order-service",
}

# Ports exposés par docker-compose.yml (modifiables avec CATALOG_PORT et ORDER_PORT)
CATALOG_URL = f"http://localhost:{os.environ.get('CATALOG_PORT', '18081')}"
ORDER_URL = f"http://localhost:{os.environ.get('ORDER_PORT', '18082')}"


class CommandError(Exception):
    """Une étape a échoué : le script s'arrête avec le code 1."""


def run(args, cwd=ROOT):
    print(f"\n>>> {' '.join(str(a) for a in args)}  (dans {Path(cwd).name})", flush=True)
    result = subprocess.run([str(a) for a in args], cwd=cwd)
    if result.returncode != 0:
        raise CommandError(f"échec de : {' '.join(str(a) for a in args)}")


def mvnw(service, *goals):
    service_dir = SERVICES[service]
    wrapper = service_dir / ("mvnw.cmd" if os.name == "nt" else "mvnw")
    run([wrapper, "-B", *goals], cwd=service_dir)


# ---------- Prérequis ----------

def prerequisites():
    missing = []
    for tool, args in (("python", [sys.executable, "--version"]),
                       ("java", ["java", "-version"]),
                       ("docker", ["docker", "--version"])):
        if tool != "python" and shutil.which(tool) is None:
            missing.append(tool)
            continue
        subprocess.run(args)
    if missing:
        raise CommandError(f"outils manquants : {', '.join(missing)}")
    print("Prérequis OK")


# ---------- Tests ----------

def test_catalog():
    mvnw("catalog", "clean", "test")


def test_order():
    mvnw("order", "clean", "test")


def test():
    test_catalog()
    test_order()


# ---------- Qualité ----------

def lint_catalog():
    mvnw("catalog", "checkstyle:check")


def lint_order():
    mvnw("order", "checkstyle:check")


def lint():
    lint_catalog()
    lint_order()


# ---------- Portes locales : tests puis qualité ----------

def verify():
    """Reproduit les portes tests et qualité de la CI.

    Ne construit pas les images, ne lance pas Gitleaks et ne publie aucun rapport :
    ces responsabilités restent dans GitHub Actions.
    """
    test()
    lint()
    print("\nverify : tests et qualité OK")


# ---------- Docker Compose ----------

def compose_up():
    run(["docker", "compose", "up", "--build", "-d"])


def compose_down():
    run(["docker", "compose", "down"])


# ---------- Smoke test ----------

# Les services tournent en local : on ignore un éventuel proxy configuré sur le poste
NO_PROXY_OPENER = urllib.request.build_opener(urllib.request.ProxyHandler({}))


def http(method, url, body=None):
    data = json.dumps(body).encode() if body is not None else None
    request = urllib.request.Request(url, data=data, method=method,
                                     headers={"Content-Type": "application/json"})
    try:
        with NO_PROXY_OPENER.open(request, timeout=5) as response:
            return response.status, json.loads(response.read() or "null")
    except urllib.error.HTTPError as error:
        return error.code, None


def wait_until_up(name, url, timeout=120):
    print(f"Attente de {name} ({url}) ...", flush=True)
    deadline = time.time() + timeout
    while time.time() < deadline:
        try:
            status, body = http("GET", f"{url}/actuator/health")
            if status == 200 and body.get("status") == "UP":
                print(f"  {name} : UP")
                return
        except (urllib.error.URLError, ConnectionError, TimeoutError):
            pass
        time.sleep(2)
    raise CommandError(f"{name} n'a pas démarré en {timeout} s")


def check(condition, message):
    if not condition:
        raise CommandError(f"smoke : {message}")
    print(f"  OK  {message}")


def smoke():
    """Vérifie que les services démarrés répondent et que le scénario principal fonctionne."""
    wait_until_up("catalog-service", CATALOG_URL)
    wait_until_up("order-service", ORDER_URL)

    status, products = http("GET", f"{CATALOG_URL}/products")
    check(status == 200 and any(p["id"] == 1 for p in products),
          "GET /products renvoie le produit 1")

    # Appel REST entre services : order-service interroge catalog-service
    status, order = http("POST", f"{ORDER_URL}/orders", {"productId": 1, "quantity": 2})
    check(status == 201, "POST /orders renvoie 201")
    check(abs(order["totalPrice"] - 99.8) < 0.001,
          f"2 claviers à 49,9 EUR coûtent 99,8 EUR (reçu : {order['totalPrice']})")

    status, _ = http("POST", f"{ORDER_URL}/orders", {"productId": 999, "quantity": 1})
    check(status == 404, "un produit inconnu renvoie 404")
    print("\nSmoke test réussi")


COMMANDS = {
    "prerequisites": prerequisites,
    "test-catalog": test_catalog,
    "test-order": test_order,
    "test": test,
    "lint-catalog": lint_catalog,
    "lint-order": lint_order,
    "lint": lint,
    "verify": verify,
    "compose-up": compose_up,
    "compose-down": compose_down,
    "smoke": smoke,
}


def main():
    if len(sys.argv) != 2 or sys.argv[1] not in COMMANDS:
        print(__doc__)
        print("Commandes : " + ", ".join(COMMANDS))
        return 2
    try:
        COMMANDS[sys.argv[1]]()
    except CommandError as error:
        print(f"\nERREUR : {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
