# 🖥️ Simulator de Planificare a Proceselor și Gestiune a Memoriei (OS Simulator)

## 📖 1. Introducere și Cerințe
Obiectivul acestui proiect este dezvoltarea unui simulator de nivel scăzut pentru componentele de bază ale unui sistem de operare: **Planificatorul de procese (Scheduler)** și **Managerul de Memorie (Memory Manager)**.

### Specificații Cheie:
* **Multi-procesor:** Simularea rulării pe un număr variabil de unități CPU.
* **Algoritm Round-Robin cu Afinitate:** Procesele sunt planificate în cuante de timp egale (Time Slice), cu prioritate pe ultimul procesor utilizat.
* **Proces de Sistem (VIP):** Un proces special cu prioritate maximă, lansat periodic pentru a rezolva apelurile de sistem (I/O).
* **Memorie Virtuală (LRU):** Gestiunea RAM-ului prin politica "Least Recently Used" și simularea transferului constant Disk <-> RAM.
* **Constrângere Strictă:** Interzicerea utilizării librăriilor Java (ex: `java.util.List`, `java.util.Queue`, `String.split`) pentru logica internă a sistemului.

---

## 🏗️ 2. Arhitectura Sistemului
Proiectul adoptă o arhitectură stratificată și decuplată, utilizând mai multe **Design Patterns** pentru a asigura calitatea și mentenabilitatea codului:

1.  **Strategy Pattern:** Interfețele `SchedulingStrategy` și `MemoryReplacementStrategy` permit schimbarea algoritmilor (ex: înlocuirea Round-Robin cu FIFO) fără a modifica motorul de bază al simulării.
2.  **Observer Pattern:** Motorul (`SimulationEngine`) notifică independent "ascultătorii" (`Logger`, `GanttChartGUI`) despre evenimentele de sistem prin intermediul interfeței `SimulationEventListener`.
3.  **Model-View-Controller (MVC):** Logica de business (Modelele și Core-ul) este complet izolată de reprezentarea grafică și de funcțiile de parsare I/O.

---

## 🛠️ 3. Componentele Unitare (Module)

### A. Motorul de Simulare (`SimulationEngine`)
Reprezintă „ceasul” (The Clock) sistemului. Într-o buclă continuă, acesta avansează timpul global (`globalTime`) și orchestrează fluxul de operațiuni:
1. Verifică și lansează procesele noi care apar în sistem.
2. Controlează și actualizează transferurile de pe Disk.
3. Execută instrucțiunile proceselor aflate pe unitățile CPU.
4. Gestionează preempțiunea și blocajele survenite în urma apelurilor de sistem.

### B. Planificatorul (`Scheduler`)
Implementează algoritmul **Round-Robin cu Afinitate**. Deoarece structurile de date standard sunt interzise, sistemul utilizează o **Coadă Circulară (Circular Buffer)** implementată manual prin vectori nativi.
* **Regula de afinitate:** Când un procesor devine liber, planificatorul caută cu prioritate în coadă un proces care a rulat cel mai recent pe acel procesor specific.

### C. Managerul de Memorie (`MemoryManager`)
Simulează limitările memoriei RAM fizice și mecanismul de Swapping.
* **Politica LRU:** Este implementată printr-un vector nativ, unde la fiecare accesare (CPU tick), procesul devine MRU (mutat la capătul vectorului), lăsând procesele vechi să alunece natural către indexul 0 pentru evacuare.
* **Swapping:** Timpul necesar pentru ca un proces să devină activ este calculat strict matematic pe baza `MemoryRequired / DiskTransferRate`.

### D. Procesul de Sistem (`SystemProcess`)
Un proces atipic (VIP) care necesită 0 MB de memorie. Acesta se trezește periodic, obținând controlul absolut asupra procesorului pentru a prelua și rezolva coada de apeluri de sistem (I/O Requests) blocate ale proceselor utilizator.

### E. Parserul de Intrare (`InputParser`)
Construit manual de la zero pentru extragerea secvențială a datelor. Analizează textul caracter cu caracter (prin coduri ASCII) pentru a asambla numerele, respectând interzicerea metodelor de librărie pentru procesarea șirurilor de caractere.

---

## 📊 4. Diagrama Fluxului de Date

![Diagrama UML a Simulatorului OS](Diagram.png)


## 📈 5. Detalii de Implementare Vizuală
Conform permisiunii specificate în cerință (excepție pentru GUI), am utilizat **Java Swing** cu un panou de desenare avansat (`Graphics2D`) pentru generarea diagramei Gantt.
* **Axa Timpului:** Riglă gradată clar și grile verticale pentru maparea fiecărui tick pentru o precizie maximă în analiză.
* **JScrollPane Orizontal:** Permite vizualizarea simulărilor lungi (mii de unități de timp) fără comprimarea sau distorsionarea graficului.
* **Design Intuitiv:** Fiecare proces primește o culoare unică generată automat. "Procesul 0" (Sistemul) este marcat întotdeauna cu gri închis pentru a fi ușor de identificat.
* **Hard Disk Activity:** Include un rând dedicat care vizualizează perioadele în care discul este ocupat cu transferul datelor, permițând corelarea timpilor de așteptare de pe CPU cu activitatea de swapping.

---

## 🚀 6. Cum se rulează

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
Simulatorul este programat să detecteze și să gestioneze fenomenul de **Thrashing**. Acesta apare atunci când memoria RAM fizică este insuficientă pentru setul de procese active, generând o stare în care sistemul își consumă resursele aproape exclusiv pentru operațiuni de Swap (Disk I/O) în detrimentul execuției pe CPU.

În implementarea curentă:
* Sistemul respectă strict politica **LRU** pentru a încerca minimizarea acestui efect.
* Există un mecanism de **Time-out de siguranță** (setat la 20.000 de tick-uri) care oprește simularea dacă se detectează o buclă de thrashing extrem care ar împiedica finalizarea naturală a proceselor.