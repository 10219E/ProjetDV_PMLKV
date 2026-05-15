### Projet DV2026 - EPHEC PAUL MALIOUKOV JUIN 2026

---
## Document d'exploitation du projet
## 1. Bienvenue

Bienvenue sur le projet **ProjetDV2026-PMALIOUKOV** !
Ce guide vous accompagne dans la configuration et le lancement de l'application en environnement de développement local sous **IntelliJ IDEA**.

#### Les ports utilisés par le projet sont ceux par défaut:
- **Backend:** 8080
- **Frontend:** 4200

---

## 2. Importation du projet

### **Via IntelliJ IDEA (Recommandé)**
1. Ouvrez **IntelliJ IDEA**
2. Sélectionnez **File > New > Project from Version Control...**
3. Entrez l'URL de mon dépôt GitHub :
   `https://github.com/10219E/ProjetDV2026-PMALIOUKOV`
4. Choisissez un répertoire de destination
5. Cliquez sur **Clone**

### **En ligne de commande**
```bash
git clone https://github.com/10219E/ProjetDV2026-PMALIOUKOV
```

---

## 3. Chargement du script Maven (Backend)

Naviguez vers le dossier backend :
```bash
cd ProjetDV2026-PMALIOUKOV/backend
```

Chargez les dépendances Maven :
```bash
.\mvnw.cmd clean install
```

**Alternative UI :** Dans IntelliJ, faites un clic droit sur le fichier `pom.xml` > **Maven** > **Reload Project**.

---

## 4. Démarrage de Docker Desktop
### Lancement de Docker Desktop

- **Via l'interface :** Ouvrez Docker Desktop depuis votre menu d'applications.
- **Via Windows Run** (Application **Executer**) **simplement tapez:**
```
docker desktop
```

⚠️ **Prérequis :** Docker Desktop doit être en cours d'exécution avant de continuer.

---

## 5. Initialisation Docker de la base de données (p3sgbd-prodv)
### Détails de la configuration

- **Base de données :** `p3sgbd-prodv` (Projet 2026 - EPHEC, PMALIOUKOV)
- **Fichier `docker-compose.yml` :** Doit être exécuté depuis la racine du projet (`ProjetDV2026-PMALIOUKOV\`)
- **Script SQL d'initialisation :** `ProjetDV2026-PMALIOUKOV\backend\docker-db-init\init-sql-server.sql`

### Commandes Docker
**Initialiser la base de données et les conteneurs**
```bash
docker compose up -d
```
**Temps d'attente :** La base de données sera prête après 
**± 120 secondes**.

**Vérifier les logs en temps réel**
```bash
docker compose logs -f
```

**Messages attendus en fin d'exécution :**
```text
Changed database context to 'p3sgbd-prodv'.
p3sgbd-prodv  | Changed database context to 'master'.
p3sgbd-prodv  | Date Time spid53      Setting database option READ_WRITE to ON for database 'p3sgbd-prodv'.
```

**Remarque importante :**
Cette commande doit initialiser les 3 modules conteneurs.
Si ce n'est pas le cas :
1. Exécutez `docker compose down -v` pour tout supprimer.
2. Relancez `docker compose up -d`.
3. Attendez que la base de données soit prête en vérifiant les logs avec `docker compose logs -f`.

**Supprimer complètement les conteneurs et volumes**
```bash
docker compose down -v
```

### La base de données est également accessible via SQL Server Management Studio (SSMS) sous :
⚠️ **Attention :** Le container docker doit être en cours d'exécution pour que la base de données soit accessible.

**Nom de serveur** : `localhost:1433`

**Utilisateurs**:
- *Administrateur (utilisé par l'application)* :
  - **Username** : `PRO`
  - **Password** : `Ephec@2026EPS!`
- *Lecteur* :
  - **Username** : `readerX`
  - **Password** : `Reader@2026EPS!`

- *Gestionnaire de sauvegarde* :
  - **Username** : `ephec_bu`
  - **Password** : `Bops@2026EPS!`

**Base de données** : `p3sgbd-prodv`

---

## 6. Démarrage du Backend
### Via la ligne de commande

Naviguez vers le dossier backend :
```bash
cd ProjetDV2026-PMALIOUKOV/backend
```

Depuis le dossier `backend` :
```bash
.\mvnw.cmd spring-boot:run
```

### Via IntelliJ IDEA
1. Vérifiez que `ProjectApplication.java` n'est pas déjà disponible en mode **Run**.
2. Si il ne l'est pas, naviguez vers le dossier `backend/src/main/java/lu/ephec/backend_projetdv2026/`
3. Click droit sur `ProjectApplication.java` - classe principale (annotée avec `@SpringBootApplication`)
4. Cliquez sur **Run 'ProjectApplication'**.

---

### ⚠️ En cas d'échec / erreur

Il se peut que sur certaines machines **JASYPT (encryption de mots de passe)** ne soit pas correctement configuré, ou que les variables d'environnement ne soient pas prises en compte.

Pour rapidement lancer le projet, modifiez le fichier `backend/src/main/resources/application.properties` et remplacez la variable d'environnement `spring.datasource.password` par le mot de passe en clair :
```
spring.datasource.password=Ephec@2026EPS!
```

## 7. Configuration du Frontend

Naviguez vers le dossier frontend :
```bash
cd ProjetDV2026-PMALIOUKOV/frontend
```

Installez les dépendances npm :
```bash
npm install
```

Vérifiez que le build fonctionne :
```bash
ng build
```

---

## 8. Démarrage du Frontend
### Via la ligne de commande
```bash
npm start
```

### Via IntelliJ IDEA
Via le mode **Run > Edit Configurations...** :
   - Cliquez sur `+` > `npm`.
   - Sélectionnez le script `start`.
   - Cliquez sur **OK**, puis **Run**.

**Génération de l'API OpenAPI :**
Si vous modifiez l'API backend, régénérez le client frontend avec :
```bash
cd frontend
npm run generate-api
```

---

## 9. Accès aux applications

| Application | URL |
|---|---|
| **Frontend** | [http://localhost:4200/](http://localhost:4200/) |
| **Backend (Swagger UI)** | [http://localhost:8080/swagger-ui/index.html#/](http://localhost:8080/swagger-ui/index.html#/) |
| **OpenAPI** | Disponible et rafraîchissable via `npm run generate-api` |

---

## 10. Exécution des tests unitaires

### Backend (Tests unitaires)
Depuis le dossier `backend` :
```bash
.\mvnw.cmd test
```
**Via IntelliJ :** Naviguez vers `backend/src/test` dans le dossier de projet, clic droit sur le dossier `java` : **Run All Tests**.

### Frontend (Tests Cypress)
Depuis le dossier `frontend` :

**Pour ouvrir l'interface Cypress (Mode interactif) :**
```bash
npx cypress open
```

**Pour exécuter les tests en ligne de commande (Mode headless) :**
```bash
npx cypress run
```
## 11. Utilisateurs de test applicatifs

Voici la liste des utilisateurs de test disponibles dans l'application, avec leurs rôles, emails et mots de passe :

### Super Administrateurs (A)
- **A001** : pmlkv@ephec.be / S@dminPML1!
- **A002** : rhardenne@ephec.be / S@dminRHA1!

### Administrateurs site unique (M)
- **M001** : vfievez@ephec.be / M@dminFVZ1!
- **M002** : ogues@ephec.be / M@dminOGG1!

### Utilisateurs VIP - tous les sites (G)
- **G0001** : mchlo@ephec.be / VIP@ccess1!
- **G0002** : jdupont@ephec.be / VIP@ccess2! (A une dette)
- **G0003** : gguy@ephec.be / VIP@ccess3!
- **G0004** : clambert@ephec.be / Norm@lS!te5 (Inscrit via formulaire et mis à niveau VIP)

### Abonnés site unique (S)
- **S0001** : cmartin@ephec.be / Norm@lS!te1
- **S0002** : adubois@ephec.be / Norm@lS!te2 (Avait une dette)
- **S0003** : lvandriesche@ephec.be / Norm@lS!te3
- **S0004** : hmoret@ephec.be / Norm@lS!te4 (Inscrit via formulaire)

### Invités (L)
- **L0001** : sbernard@ephec.be / Invite@Usr1!
- **L0002** : tmara@ephec.be / Invite@Usr2!
- **L0003** : amariane@ephec.be / Invite@Usr3!

PS: vous pouvez également consulter la liste des utilisateurs et essayer le hacheur de mots de passe BCrypt de l'application sous:
`backend/src/main/java/lu/ephec/backend_projetdv2026/services/security/`

--> fichier service `ManualHashGen.java`