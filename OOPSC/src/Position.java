/**
 * Die Klasse repräsentiert eine Position im Quelltext.
 */
class Position {
	/** Die Quelltextzeile. */
	int line;

	/** Die Quelltextspalte. */
	int column;

	/**
	 * Konstruktor.
	 *
	 * @param line
	 *        Die Quelltextzeile.
	 * @param column
	 *        Die Quelltextspalte.
	 */
	Position(int line, int column) {
		this.line = line;
		this.column = column;
	}

	/**
	 * Die Methode zählt die Quelltextposition weiter.
	 * Dazu wird das aktuelle Zeichen ausgewertet.
	 *
	 * @param c
	 *        Das aktuell gelesene Zeichen. '\n' springt
	 *        an den Anfang der nächsten Zeile. '\t' springt
	 *        zur nächsten Tabulatorposition (Vielfache von 8).
	 *        '\r' wird ignoriert.
	 */
	void next(char c) {
		if (c == '\n') {
			++this.line;
			this.column = 1;
		} else if (c == '\t') {
			this.column += 8 - (this.column - 1) % 8;
		} else if (c != '\r') {
			++this.column;
		}
	}
}
