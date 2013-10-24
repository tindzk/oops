package org.oopsc;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Die Klasse repräsentiert einen Datenstrom, in der Assemblercode des
 * auszugebenen Programms geschrieben wird. Da die Klasse von {@link java.io.PrintStream
 * PrintStream} erbt, können alle Methoden
 * verwendet werden, mit denen man auch auf die Konsole schreiben kann.
 * Zusätzlich kann die Klasse eindeutige Marken für den Assemblerquelltext
 * generieren.
 */
class CodeStream extends PrintStream {
	/** Das Attribut enthält den gerade gültigen Namensraum (Klasse + Methode). */
	private String namespace;

	/** Das Attribut ist ein Zähler zur Generierung eindeutiger Bezeichner. */
	private int counter;

	/**
	 * Konstruktor zur Ausgabe auf die Konsole.
	 */
	CodeStream() {
		super(System.out);
	}

	/**
	 * Konstruktor für beliebige Streams.
	 */
	CodeStream(OutputStream stream) {
		super(stream);
	}

	/**
	 * Konstruktor zur Ausgabe in eine Datei.
	 *
	 * @param fileName
	 *        Der Name der Ausgabedatei.
	 * @throws FileNotFoundException
	 *         Die Datei kann nicht erzeugt werden.
	 */
	CodeStream(String fileName) throws FileNotFoundException {
		super(new File(fileName));
	}

	/**
	 * Die Methode setzt den aktuell gültigen Namensraum.
	 * Dieser wird verwendet, um eindeutige Marken zu generieren.
	 * Derselbe Namensraum darf nur einmal während der Code-Erzeugung
	 * gesetzt werden.
	 *
	 * @param namespace
	 *        Den ab jetzt gültigen Namensraum (Klasse + Methode).
	 */
	void setNamespace(String namespace) {
		this.namespace = namespace;
		this.counter = 1;
	}

	/**
	 * Die Methode erzeugt eine eindeutige Marke im aktuellen Namensraum.
	 *
	 * @return Die Marke.
	 */
	String nextLabel() {
		return this.namespace + "_" + this.counter++;
	}
}