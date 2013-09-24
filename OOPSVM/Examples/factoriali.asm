; Dieses Beispiel rechnet die Fakultät von 5 iterativ aus.
; Sie steht nach der Ausführung in R2.
  MRI R1, 5 ; Eingabe
  MRI R2, 1 ; Variable zum Ergebnisaufbau

loop:
  ISZ R3, R1 ; Wenn Eingabe 0,
  JPC R3, end ; dann fertig
  MUL R2, R1 ; Eine weitere Zahl ins Ergebnis hineinmultiplizieren
  MRI R3, 1
  SUB R1, R3 ; Eingabe um 1 runterzählen
  MRI R0, loop ; und nochmal
end: