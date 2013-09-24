; Dieses Beispiel rechnet die Fakultät von 5 rekursiv aus.
; Sie steht nach der Ausführung in R3.
  MRI R3, 5 ; Eingabe
  MRI R1, 1 ; Hilfsvariable. Ist immer 1.
  MRI R2, stack ; Stapelzeiger

factorial:
  JPC R3, label1 ; Wenn Eingabe ungleich 0, dann weiter
  MRI R3, 1 ; Ansonsten ist das Ergebnis 1
  MRI R0, label3 ; Weiter beim Funktionsende

label1:
  MMR (R2), R3 ; Eingabe auf dem Stapel zwischenspeichern
  ADD R2, R1 ; Stapelzeiger einen weiter
  SUB R3, R1 ; Eingabe um 1 verringern
  MRI R4, label2 ; Rücksprungadresse
  MMR (R2), R4 ; auf den Stapel legen
  ADD R2, R1 ; Stapelzeiger einen weiter
  MRI R0, factorial ; Rekursiver Aufruf, R3 ist Funktionsergebnis

label2:
  SUB R2, R1 ; Stapelzeiger einen zurück
  MRM R4, (R2) ; alten Wert der Eingabe entnehmen
  MUL R3, R4 ; Ins Ergebnis hineinmultiplizieren

label3:
  SUB R2, R1 ; Stapelzeiger einen hoch
  MRM R0, (R2) ; Rücksprungadresse entnehmen und hinspringen

  DAT 1, end ; Rücksprungadresse zum Beenden des Programms

stack:
  DAT 30, 0 ; 30 Worte Platz für den Stapel
end:
