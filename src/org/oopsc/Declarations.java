package org.oopsc;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Die Klasse repräsentiert alle an einer bestimmten Stelle im Programm gültigen Deklarationen.
 * Die Deklarationen werden dabei als sich überlagernde Sichtbarkeitsebenen dargestellt.
 * Die Klasse stellt Methoden zum Auflösen von Bezeichnern bereit.
 */
public class Declarations {
	/**
	 * Die Ebenen mit allen Deklarationen. Deklarationen in später hinzugefügten Ebenen überdecken
	 * Deklarationen in früher hinzugefügten Ebenen. Jede Ebene wird durch eine Hash-Tabelle
	 * realisiert.
	 */
	LinkedList<HashMap<String, Declaration>> levels = new LinkedList<HashMap<String, Declaration>>();

	/** Die aktuelle Klasse. */
	ClassDeclaration currentClass;

	/** Die aktuelle Methode. */
	MethodDeclaration currentMethod;

	/**
	 * Die Method erstellt eine Kopie dieses Objekts. Dabei werden die Ebenen nicht kopiert,
	 * sondern auch von der Kopie weiter benutzt. Die umgebende Liste wird aber kopiert,
	 * so dass sie in beiden Instanzen unabhängig voneinander verändert werden kann.
	 *
	 * @return Die Kopie dieses Objekts.
	 */
	@Override
	public Object clone() {
		Declarations d = new Declarations();
		for (HashMap<String, Declaration> l : this.levels) {
			d.levels.add(l);
		}
		d.currentClass = this.currentClass;
		d.currentMethod = this.currentMethod;
		return d;
	}

	/**
	 * Erzeugt eine neue Deklarationsebene.
	 */
	public void enter() {
		this.levels.addFirst(new HashMap<String, Declaration>());
	}

	/**
	 * Verwirft die zuletzt erzeugte Deklarationsebene.
	 */
	public void leave() {
		this.levels.removeFirst();
	}

	/**
	 * Die Methode fügt eine neue Deklaration in die oberste Ebene ein.
	 * Wenn dort bereits die Deklaration eines gleichlautenden Bezeichners
	 * vorhanden war, wird ein Fehler erzeugt.
	 *
	 * @param declaration
	 *        Die neu einzufügende Deklaration.
	 * @throws CompileException
	 *         Dieser Bezeichner wurde bereits in dieser Ebene verwendet.
	 */
	public void add(Declaration declaration) throws CompileException {
		if (this.levels.getFirst().get(declaration.identifier.name) != null) {
			throw new CompileException("Doppelte Deklaration von "
					+ declaration.identifier.name,
					declaration.identifier.position);
		} else {
			this.levels.getFirst()
					.put(declaration.identifier.name, declaration);
		}
	}

	/**
	 * Die Methode ordnet einen Bezeichner seiner Deklaration im Programm zu.
	 *
	 * @param identifier
	 *        Der Bezeichner, der aufgelöst werden soll.
	 * @throws CompileException
	 *         Die Deklaration des Bezeichners wurde nicht gefunden.
	 */
	private void resolve(ResolvableIdentifier identifier)
			throws CompileException {
		if (identifier.declaration == null) {
			for (HashMap<String, Declaration> l : this.levels) {
				identifier.declaration = l.get(identifier.name);
				if (identifier.declaration != null) {
					return;
				}
			}
			throw new CompileException("Fehlende Deklaration von "
					+ identifier.name, identifier.position);
		}
	}

	/**
	 * Die Methode ordnet einen Typ seiner Deklaration im Programm zu.
	 *
	 * @param type
	 *        Der Typ, der aufgelöst werden soll.
	 * @throws CompileException
	 *         Die Deklaration des Typs wurde nicht gefunden.
	 */
	public void resolveType(ResolvableIdentifier type) throws CompileException {
		this.resolve(type);
		if (!(type.declaration instanceof ClassDeclaration)) {
			throw new CompileException("Typ erwartet", type.position);
		}
	}

	/**
	 * Die Methode ordnet eine Variable, ein Attribut oder einen Methodenaufruf
	 * der zugehörigen Deklaration im Programm zu.
	 *
	 * @param varOrMethod
	 *        Die Variable, das Attribut oder der Methodenaufruf.
	 * @throws CompileException
	 *         Die Deklaration der Variable, des Attributs oder
	 *         des Methodenaufruf wurde nicht gefunden.
	 */
	public void resolveVarOrMethod(ResolvableIdentifier varOrMethod)
			throws CompileException {
		this.resolve(varOrMethod);
		if (varOrMethod.declaration instanceof ClassDeclaration) {
			throw new CompileException("Variable oder Methode erwartet",
					varOrMethod.position);
		}
	}
}