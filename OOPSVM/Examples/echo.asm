; Dieses Beispiel gibt lediglich die Zeichen von der Standardeingabe wieder aus.
; Zum Beenden muss ein Dateiende auf der Standardeingabe erzeugt werden.
loop:
  SYS 0, 1 ; Zeichen einlesen
  ISN R2, R1 ; wenn negativ
  JPC R2, end ; dann Ende
  SYS 1, 1 ; ansonsten ausgeben
  MRI R0, loop ; und wieder von vorne
end:
