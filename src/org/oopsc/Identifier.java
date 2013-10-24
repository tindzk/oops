package org.oopsc;

/**
 * Die Klasse repräsentiert einen Bezeichner im Quelltext.
 */
public class Identifier {
	/** Der Name des Bezeichners. */
	public String name;

	/** Die Quelltextstelle, an der der Bezeichner gelesen wurde. */
	public Position position;

	/**
	 * Konstruktor.
	 *
	 * @param name
	 *        Der Name des Bezeichners.
	 * @param position
	 *        Die Quelltextstelle, an der der Bezeichner gelesen wurde.
	 */
	public Identifier(String name, Position position) {
		this.name = name;
		this.position = position;
	}
}