# The Vizzard — Election Analyzer

The Vizzard is a **Java-based election analysis and visualization tool** that reads real-world voting data from Excel and CSV files, processes results, simulates live reporting, and visualizes outcomes across states and counties.  
Built entirely from scratch in Java, it integrates file handling, GUI plotting, and statistical computation — transforming static datasets into dynamic, interpretable visuals.

---

## Overview

At its core, **The Vizzard** takes the concept of election night coverage — real-time percentages, state flips, county maps, and final tallies — and replicates it using data-driven logic and custom-rendered graphics.

It was first developed as a **data visualization experiment** using *BlueJ* and later refined into a standalone Java project.  
The challenge was to blend backend data computation with frontend map rendering — all without external frameworks beyond Apache POI for Excel reading.

---

## Key Capabilities

- **Reads live-like datasets:** State-level and county-level results parsed directly from `.csv` and `.xlsx` files.  
- **Calculates outcomes:** Derives state percentages, winners, and electoral totals dynamically.  
- **Partial result simulation:** Mimics “reporting hours” as the election progresses.  
- **Visualization suite:**
  - Bar chart comparisons of popular and electoral votes  
  - Full U.S. county heat map  
  - State-specific county visualizations  

---

## The Process — In Action

### 1. Initial Input & State Breakdown  
Candidate names are entered, files are loaded, and each state’s results are processed and displayed.  
![State Results](https://github.com/rfm5898/the-vizzard/blob/main/outputs/the_vizzard_loadedcounties.png)

---

### 2. Percentage Calculations  
Every state’s vote share is calculated and displayed in a clean formatted output.  
![State Summary](https://github.com/rfm5898/the-vizzard/blob/main/outputs/the_vizzard_summary.png)

---

### 3. Partial Reporting Simulation  
As hours progress, The Vizzard generates partial tallies for both candidates — mimicking real-time election coverage.  
![Partial Results](https://github.com/rfm5898/the-vizzard/blob/main/outputs/the_vizzard_partialres.png)

---

### 4. National Visualization  
A national county map is drawn, color-coded:  
- **Green → Candidate A**  
- **Magenta → Candidate B**  
![National Map](https://github.com/rfm5898/the-vizzard/blob/main/outputs/the_vizzard_map.png)

---

### 5. Final Results & State-Level Visualization  
Both popular and electoral votes are displayed visually.  
Users can then choose a specific state for detailed county visualization — such as Texas below.  
![Results Chart](https://github.com/rfm5898/the-vizzard/blob/main/outputs/the_vizzard_results.png)
![Texas Map](https://github.com/rfm5898/the-vizzard/blob/main/outputs/the_vizzard_TXplot.png)

---

## Implementation Notes

- **Language:** Java  
- **Libraries Used:** Apache POI (Excel reading), Swing (UI), AWT (graphics rendering)  
- **Core Files:**
  - `TheVizzard.java` — Data ingestion, computation, and logic  
  - `TheVizzardMap.java` — Visualization and projection rendering  
  - `/data/` — CSV and Excel datasets  
  - `/outputs/` — Result screenshots and map renderings  

---

## Reflection

This project was built purely out of curiosity — to see if **data could tell a visual story without modern libraries or frameworks.**  
It started as a console-based analyzer but evolved into something that could simulate a real election network’s workflow: from counting votes to drawing a map.  

While imperfect, *The Vizzard* remains a personal milestone — one that pushed the boundaries of what’s possible inside plain Java.

---

## Developer

Developed by **Rayan Minhaj**  
Computer Science Major @ The Pennsylvania State University  
[GitHub](https://github.com/rfm5898) | [LinkedIn](https://www.linkedin.com/in/rayan-minhaj-%F0%9F%A4%96-a8492134b/)
