# 🛡️ Robot Security Dashboard - Backend (Spring Boot)

Această componentă reprezintă nucleul de control și monitorizare (Backend) pentru un sistem de securitate bazat pe un robot autonom, proiectat pentru supravegherea unei hale de producție. 

Aplicația realizează puntea de legătură între simularea robotului din **ROS2/Gazebo** (executată pe un sistem Linux separat) și interfața utilizatorului.

---

## 📋 Prezentare Proiect

Sistemul este împărțit în două ramuri principale pe GitHub:
1.  **Branch-ul de Backend (Acest cod):** Dezvoltat în Spring Boot, gestionează logica de business, securitatea, baza de date și comunicarea cu robotul.
2.  **Branch-ul de Robot (Linux):** Conține scripturile Python, nodurile ROS2 și mediul de simulare Gazebo.

### Fluxul de date:
* **Robot ➔ Backend:** Robotul trimite telemetrie (coordonate $x, y$, nivel baterie) și alerte de securitate (cu capturi de imagine Base64) prin cereri HTTP POST.
* **Backend ➔ Robot:** Comenzile de control (`START`, `STOP`, `DOCK`) sunt trimise de administrator din dashboard către API-ul robotului.
* **Backend ➔ Frontend:** Datele sunt împinse în timp real către interfața web folosind **WebSockets (STOMP)**.

---

## 🚀 Funcționalități Principale

### 1. Control și Administrare
* **Comenzi de la distanță:** Interfață securizată pentru trimiterea comenzilor de mișcare către robot.
* **Restricții de acces:** Doar utilizatorii cu rol de `ADMIN` pot trimite comenzi robotului.

### 2. Monitorizare Telemetrie
* **Tracking Live:** Procesarea fluxului de coordonate primite de la robot.
* **Management Baterie:** Dacă nivelul bateriei scade sub **15%**, sistemul generează automat o alertă internă de tip `LOW_BATTERY`.

### 3. Securitate și Alerte
* **Alerte de Intruziune:** Salvarea alertelor în baza de date MySQL, incluzând mesaje specifice și snapshot-uri (imagini) trimise de robot în format Base64.
* **Arhivare:** Toate evenimentele sunt salvate cu timestamp-ul original de pe robot pentru audit ulterior.

### 4. Autentificare JWT
* Implementare completă de securitate cu **JSON Web Tokens**.
* Suport pentru **Refresh Tokens** pentru a menține sesiunea activă fără login repetat.
* Roluri definite: `ADMIN` (Control total) și `USER` (Doar vizualizare).

---

## 🛠️ Tehnologii Utilizate

* **Framework:** Spring Boot 3
* **Security:** Spring Security & JWT
* **Bază de date:** MySQL (JPA/Hibernate)
* **Comunicare Real-time:** Spring WebSocket + STOMP
* **Limbaj:** Java 17
* **Utilitare:** Lombok (pentru reducerea codului boilerplate)

---

## 📂 Structura Proiectului

* `auth/` - Configurația securității, filtrarea JWT și logica de autentificare.
* `controller/` - Endpoint-urile API pentru Admin (comenzi) și Robot (telemetrie/alerte).
* `service/` - Logica de procesare a datelor și integrarea cu robotul prin `RestTemplate`.
* `model/` & `dto/` - Entitățile bazei de date și obiectele de transfer de date.
* `repository/` - Interfețele pentru persistența datelor în MySQL.
* `config/` - Configurări pentru WebSockets și Bean-uri de sistem.

---

## ⚙️ Configurare și Instalare

1.  **Baza de date:**
    Asigură-te că ai un server MySQL pornit și creează o bază de date numită `robot_security`. Configurează credențialele în `application.properties`.

2.  **Conexiunea cu Robotul:**
    În clasa `CommandService.java`, modifică variabila `ROBOT_API_URL` cu IP-ul laptopului pe care rulează Linux/ROS2:
    ```java
    private final String ROBOT_API_URL = "http://<IP_LINUX>:5000/api/command";
    ```

3.  **Rulare:**
    ```bash
    mvn clean install
    mvn spring-boot:run
    ```

---

## 📡 Endpoint-uri API (Sumar)

| Metodă | URL | Acces | Descriere |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/auth/**` | Public | Login / Register / Refresh |
| `POST` | `/api/robot/telemetry` | Robot | Primește date de la ROS2 |
| `POST` | `/api/robot/alert` | Robot | Primește alerte cu imagini |
| `POST` | `/api/admin/start` | ADMIN | Pornește patrula robotului |
| `POST` | `/api/admin/stop` | ADMIN | Oprire de urgență |

---
*Acest proiect a fost dezvoltat ca parte a sistemului integrat de monitorizare pentru TurtleBot.*
