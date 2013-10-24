package org.oopsc;

/**
 * Die Klasse repräsentiert eine Position im Quelltext.
 */
public class Position {
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
}