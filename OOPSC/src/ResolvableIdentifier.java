import java.util.LinkedList;

/**
 * Die Klasse repräsentiert einen Bezeichner, dessen Vereinbarung im Laufe
 * der Kontextanalyse ermittelt wird. Alle Bezeichner werden in einer Liste
 * vermerkt, damit man sie alle bei Bedarf ausgeben kann.
 */
class ResolvableIdentifier extends Identifier {
    /** Dieses Klassenattribut ist eine Liste, die alle zuordenbaren Bezeichner enthält. */
    static LinkedList<ResolvableIdentifier> identifiers;

    /** Die Deklaration dieses Bezeichners. Solange sie unbekannt ist, ist dieses Attribut null. */
    Declaration declaration;

    /**
     * Konstruktor.
     * @param name Der Name des Bezeichners.
     * @param position Die Quelltextstelle, an der der Bezeichner gelesen wurde.
     */
    ResolvableIdentifier(String name, Position position) {
        super(name, position);
        identifiers.add(this);
    }

    /**
     * Die Klassenmethode initialisiert die Liste der zuordenbaren Bezeichner.
     * Sie wird benötigt, falls mehr als einmal übersetzt wird.
     */
    static void init() {
       identifiers = new LinkedList<ResolvableIdentifier>();
    }

    /**
     * Die Klassenmethode gibt alle zuordenbaren Bezeichner mit ihrer
     * Quelltextstelle und die Stelle ihrer Vereinbarung aus. Sollte
     * ein Eintrag nach der Kontextanalyse noch "unbekannt" sein,
     * enthält der Übersetzer einen Fehler.
     */
    static void print() {
        for (ResolvableIdentifier r : identifiers) {
            if (r.position != null) { // Ignoriere vom Übersetzer nachträglich erzeugte Bezeichner
                System.out.print("Zeile " + r.position.line + ", Spalte " +
                        r.position.column + ": " + r.name + " ist ");
                if (r.declaration == null) {
                    System.out.println("unbekannt");
                } else if (r.declaration.identifier.position == null) {
                    System.out.println("vordefiniert");
                } else {
                    System.out.println("definiert in Zeile " +
                            r.declaration.identifier.position.line + ", Spalte " +
                            r.declaration.identifier.position.column);
                }
            }
        }
    }
}
