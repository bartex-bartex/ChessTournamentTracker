# ChessTournamentTracker

## Instrukcja Uruchomienia

Projekt składa się z trzech aplikacji, które muszą być uruchomione jednocześnie w jednym środowisku sieciowym:
  - baza danych PostgreSQL
  - serwer backendowy
  - serwer frontendu (ten projekt zawiera się w podfolderze `frontend`)

Uruchomienie tych komponentów na własnej maszynie wymagałoby wielu kroków instalacyjnych, tak więc umożliwiamy uruchomienie tej całej konfiguracji jako kompozycję kontenerów Dockera. Jest to jedyny wspierany przez nas sposób na uruchomienie aplikacji podczas oceniania i tylko on zostanie opisany w niniejszej instrukcji.

Aby rozpocząć:

- Upewnij się, że na twojej maszynie jest zainstalowany i uruchomiony demon Dockera i jesteś w stanie uruchamiać kontenery oraz korzystać z polecenia `docker-compose` (w teorii projekt powinien działać też z podmanem, lecz projekt nie był z nim testowany, tak więc zalecamy użycie po prostu Dockera). Szczegółowe kroki w celu instalacji znajdziesz [tutaj](https://docs.docker.com/engine/install/).
- W głównym folderze repozytorium uruchom polecenie `docker-compose up` i zaczekaj aż wszystko się załaduje. Kontener z frontendem powinien wyświetlić komunikat "webpack compiled successfully", a kontener backendowy powinien pokazywać "Started ChessTournamentApplication in ... seconds".
- Otwórz interfejs webowy PGAdmin pod adresem `http://localhost:5433` i zaloguj się do bazy danych `postgres:5432` (jest to wewnętrzny adres bazy w sieci Dockera) i wszystkie parametry połączenia (login, hasło) ustaw na wartość "postgres".
- Skonfiguruj wstępną zawartość bazy danych uruchamiając w bazie danych kwerendę SQL zawartą w pliku `init-database.sql`. W ten sposób zostaną w bazie utworzone wszystkie podstawowe tabele wymagane do funkcjonowania aplikacji.
- Gdy baza danych jest już gotowa, możesz rozpocząć eksploatację strony. Odwiedź w przeglądarce stronę "http://localhost:8080" by ujrzeć stronę startową naszego projektu.


## Raporty z Testów:

- raport testów backendu: `./report.html`
- raport walidacji ESLint frontendu `eslint-report.html`
- raport testów jednostkowych Jest frontendu `jest-report.html`
