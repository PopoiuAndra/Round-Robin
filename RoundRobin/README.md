# 🖥️ Simulator de Planificare a Proceselor și Gestiune a Memoriei (OS Simulator)

## 📖 1. Introducere și Cerințe
Obiectivul acestui proiect este dezvoltarea unui simulator de nivel scăzut pentru componentele de bază ale unui sistem de operare: **Planificatorul de procese (Scheduler)** și **Managerul de Memorie (Memory Manager)**.

### Specificații Cheie:
* **Multi-procesor:** Simularea rulării pe un număr variabil de unități CPU.
* **Algoritm Round-Robin cu Afinitate:** Procesele sunt planificate în cuante de timp egale (Time Slice), cu prioritate pe ultimul procesor utilizat.
* **Proces de Sistem:** Un proces special cu prioritate maximă, lansat periodic pentru a rezolva apelurile de sistem (I/O).
* **Memorie Virtuală (LRU):** Gestiunea RAM-ului prin politica "Least Recently Used" și simularea transferului constant Disk <-> RAM.

---

## 🏗️ 2. Arhitectura Sistemului
Proiectul adoptă o arhitectură stratificată și decuplată, utilizând mai multe **Design Patterns** pentru a asigura calitatea și mentenabilitatea codului:

1.  **Strategy Pattern:** Interfețele `SchedulingStrategy` și `MemoryReplacementStrategy` permit schimbarea algoritmilor fără a modifica motorul de bază al simulării.
2.  **Observer Pattern:** Motorul (`SimulationEngine`) notifică independent "ascultătorii" (`Logger`, `GanttChartGUI`) despre evenimentele de sistem prin intermediul interfeței `SimulationEventListener`.
3.  **Model-View-Controller (MVC):** Logica de business (Modelele și Core-ul) este complet izolată de reprezentarea grafică și de funcțiile de parsare I/O.

---

## 🛠️ 3. Componentele Unitare (Module)

### A. Motorul de Simulare (`SimulationEngine`)
Reprezintă ceasul (The Clock) sistemului. Într-o buclă continuă, acesta avansează timpul global (`globalTime`) și orchestrează fluxul de operațiuni:
1. Verifică și lansează procesele noi care apar în sistem.
2. Controlează și actualizează transferurile de pe Disk.
3. Execută instrucțiunile proceselor aflate pe unitățile CPU.
4. Gestionează preempțiunea și blocajele survenite în urma apelurilor de sistem.

### B. Planificatorul (`Scheduler`)
Implementează algoritmul **Round-Robin cu Afinitate**. Sistemul utilizează o **Coadă Circulară (Circular Buffer)** implementată manual prin vectori nativi.
* **Regula de afinitate:** Când un procesor devine liber, planificatorul caută cu prioritate în coadă un proces care a rulat cel mai recent pe acel procesor specific.

### C. Managerul de Memorie (`MemoryManager`)
Simulează limitările memoriei RAM fizice și mecanismul de Swapping.
* **Politica LRU:** Este implementată printr-un vector nativ, unde la fiecare accesare (CPU tick), procesul devine MRU (mutat la capătul vectorului), lăsând procesele vechi să alunece natural către indexul 0 pentru evacuare.
* **Swapping:** Timpul necesar pentru ca un proces să devină activ este calculat strict matematic pe baza `MemoryRequired / DiskTransferRate`.

### D. Procesul de Sistem (`SystemProcess`)
Acesta se trezește periodic, obținând controlul absolut asupra procesorului pentru a prelua și rezolva coada de apeluri de sistem (I/O Requests) blocate ale proceselor utilizator.

### E. Parserul de Intrare (`InputParser`)
Construit pentru extragerea secvențială a datelor. Analizează textul caracter cu caracter (prin coduri ASCII) pentru a asambla numerele.

---
## 🔄 3. Ciclul de Viata al Proceselor (Process State)
In timpul simularii, atat procesele de utilizator cat si procesul de sistem tranziteaza printr-o serie de stari predefinite.

| ProcessState   | Descriere pentru **User Process** | Descriere pentru **System Process**                                                              |
|:---------------| :--- |:-------------------------------------------------------------------------------------------------|
| **NEW**        | Procesul a fost citit din fisier si asteapta momentul de lansare in sistem (`releaseTime`). | *N/A* (Sistemul este initializat direct in asteptare).                                           |
| **SWAPPING**   | Procesul nu are loc sau nu a fost inca adus in RAM. Asteapta transferul de pe Disk. | *N/A* (Procesul de sistem se afla permanent in RAM).                                             |
| **READY**      | Procesul este incarcat in RAM si asteapta in coada Scheduler-ului eliberarea unui CPU. | S-a trezit (perioada a expirat) si asteapta preluarea prioritara a unui CPU liber.               |
| **RUNNING**    | Procesul se afla pe un CPU si executa calcule efective (scade din CPU burst-ul curent). | Se afla pe CPU si executa (rezolva) apelurile de I/O pentru procesele utilizator blocate.        |
| **WAITING_IO** | A fost oprit de pe CPU pentru a cere o operatiune I/O. Asteapta ca VIP-ul sa il rezolve. | Coada de cereri I/O este goala. VIP-ul "doarme" si elibereaza CPU-ul pana la urmatoarea trezire. |
| **TERMINATED** | Secventa de executie s-a incheiat complet. Procesul este evacuat definitiv. | *N/A* (Procesul de sistem ruleaza in bucla continua pana la finalul simularii).                  |

---

## 📊 4. Diagrame Secventiale
Diagrama Initializarea Resurselor
![Diagrama Initializarea_Resurselor](initializare.png)

Diagrama de lansare a proceselor in CPU
![Diagrama Scheduler](Scheduler.png)

## 📈 5. Detalii de Implementare Vizuală
Am utilizat **Java Swing** cu un panou de desenare pentru generarea diagramei Gantt.

---

## 🚀 6. Rularea Simulatorului

1. Asigurați-vă că fișierul `input.txt` se află în rădăcina proiectului (lângă fișierul `pom.xml`).
2. Structura fișierului de intrare trebuie să respecte formatul:
   ```text
   [Nr_Procesoare] [Total_RAM] [Cuanta_Timp] [Perioada_Proces_Sistem] [Rata_Transfer_Disk]
   [Timp_Lansare] [Memorie_Necesara] [CPU_Burst] [IO_Burst] [CPU_Burst] ...
   ```
3. Executați clasa principală `Main.java`.
4. Simulatorul va genera automat:
    * **Jurnal în Consolă:** Vizualizarea pașilor principali în timp real.
    * **Fișierul `output.txt`:** Raport text complet al execuției pentru arhivare.
    * **Interfața Grafică:** Diagrama Gantt detaliată cu suport pentru scroll.

---

## ⚠️ 7. Notă Tehnică: Fenomenul de Thrashing
Simulatorul nu este programat să detecteze și să gestioneze fenomenul de **Thrashing**. Acesta apare atunci când memoria RAM fizică este insuficientă pentru setul de procese active, generând o stare în care sistemul își consumă resursele aproape exclusiv pentru operațiuni de Swap (Disk I/O) în detrimentul execuției pe CPU.


