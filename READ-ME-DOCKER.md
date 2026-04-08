## Docker initialization of p3sgbd-prodv data base for dev project 2026 - EPHEC. PMALIOUKOV

### Docker Desktop must be running and the docker-compose.yml file must be run from project source:
#### path > ProjetDV2026-PMALIOUKOV\ 

### Database creation SQL is located in the file:
#### path > ProjetDV2026-PMALIOUKOV\backend\docker-db-init\init-sql-server.sql

--------------------

### To init the db and its tables, run in the console:

#### _docker compose up -d_

--------------------

### Database will be initialized and ready after +- 100 seconds. You can check the logs to see the progress:

#### _docker compose logs -f_

--------------------

### Execution will end with:

#### _Changed database context to 'p3sgbd-prodv'._

#### _p3sgbd-prodv  | Changed database context to 'master'._

#### _p3sgbd-prodv  | Date Time spid53      Setting database option READ_WRITE to ON for database 'p3sgbd-prodv'._

--------------------

### To wipe off completely, run:

#### _docker compose down -v_

--------------------