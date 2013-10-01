/**
 * Die Klasse repräsentiert die Ausnahme, die bei Übersetzungsfehlern erzeugt wird.
 * Sie wird in der Hauptmethode {@link OOPSC#main(String[]) OOPSC.main} gefangen und
 * ausgegeben.
 */
class CompileException extends Exception {
	static final long serialVersionUID = 0x8374625L; // eliminiert eine Warnung

	/**
	 * Konstruktor.
	 *
	 * @param message
	 *        Die Fehlermeldung. Ihr wird der Text "Fehler in Zeile x,
	 *        Spalte y: " vorangestellt, bzw. lediglich "Fehler: ", wenn die
	 *        Quelltextstelle unbekannt ist.
	 * @param position
	 *        Die Quelltextstelle an der der Fehler aufgetreten ist.
	 *        Dieser Parameter kann auch null sein, wenn die Stelle nicht
	 *        zugeordnet werden kann.
	 */
	CompileException(String message, Position position) {
		super("Fehler"
				+ (position == null ? "" : " in Zeile " + position.line
						+ ", Spalte " + position.column) + ": " + message);
	}
}