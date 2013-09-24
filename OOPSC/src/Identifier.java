/**
 * Die Klasse repräsentiert einen Bezeichner im Quelltext.
 */
class Identifier {
    /** Der Name des Bezeichners. */
    String name;

    /** Die Quelltextstelle, an der der Bezeichner gelesen wurde. */
    Position position;

    /**
     * Konstruktor.
     * @param name Der Name des Bezeichners.
     * @param position Die Quelltextstelle, an der der Bezeichner gelesen wurde.
     */
    Identifier(String name, Position position) {
        this.name = name;
        this.position = position;
    }
}