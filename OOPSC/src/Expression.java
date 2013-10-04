import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;

/**
 * Die abstrakte Basisklasse für alle Ausdrücke im Syntaxbaum.
 * Zusätzlich zur Standardschnittstelle für Ausdrücke definiert sie auch
 * Methoden zur Erzeugung neuer Ausdrücke für das Boxing und Unboxing von
 * Ausdrücken sowie das Dereferenzieren.
 */
abstract class Expression {
	/** Der Typ dieses Ausdrucks. Solange er nicht bekannt ist, ist dieser Eintrag null. */
	ClassDeclaration type;

	/**
	 * Ist dieser Ausdruck ein L-Wert, d.h. eine Referenz auf eine Variable?
	 * Die meisten Ausdrücke sind keine L-Werte.
	 */
	boolean lValue = false;

	/** Die Quelltextposition, an der dieser Ausdruck beginnt. */
	Position position;

	/**
	 * Konstruktor.
	 *
	 * @param position
	 *        Die Quelltextposition, an der dieser Ausdruck beginnt.
	 */
	Expression(Position position) {
		this.position = position;
	}

	/**
	 * Die Methode führt die Kontextanalyse für diesen Ausdruck durch.
	 * Sie ist nicht abstrakt, da es einige abgeleitete Klassen gibt,
	 * die sie nicht implementieren, weil sie dort nicht benötigt wird.
	 * Da im Rahmen der Kontextanalyse auch neue Ausdrücke erzeugt werden
	 * können, sollte diese Methode immer in der Form "a = a.contextAnalysis(...)"
	 * aufgerufen werden, damit ein neuer Ausdruck auch im Baum gespeichert wird.
	 *
	 * @param declarations
	 *        Die an dieser Stelle gültigen Deklarationen.
	 * @return Dieser Ausdruck oder ein neuer Ausdruck, falls ein Boxing,
	 *         Unboxing oder eine Dereferenzierung in den Baum eingefügt
	 *         wurden.
	 * @throws CompileException
	 *         Während der Kontextanylyse wurde ein Fehler
	 *         gefunden.
	 */
	Expression contextAnalysis(Declarations declarations)
			throws CompileException {
		return this;
	}

	/**
	 * Die Methode gibt diesen Ausdruck in einer Baumstruktur aus.
	 * Wenn der Typ des Ausdrucks bereits ermittelt wurde, wird er auch ausgegeben.
	 *
	 * @param tree
	 *        Der Strom, in den die Ausgabe erfolgt.
	 */
	abstract void print(TreeStream tree);

	/**
	 * Die Methode generiert den Assembler-Code für diesen Ausdruck. Sie geht
	 * davon aus, dass die Kontextanalyse vorher erfolgreich abgeschlossen wurde.
	 *
	 * @param code
	 *        Der Strom, in den die Ausgabe erfolgt.
	 */
	abstract void generateCode(CodeStream code);

	/**
	 * Die Methode prüft, ob dieser Ausdruck "geboxt" oder dereferenziert werden muss.
	 * Ist dies der Fall, wird ein entsprechender Ausdruck erzeugt, von dem dieser
	 * dann der Operand ist. Dieser neue Ausdruck wird zurückgegeben. Daher sollte diese
	 * Methode immer in der Form "a = a.box(...)" aufgerufen werden.
	 * "Boxing" ist das Verpacken eines Basisdatentyps in ein Objekt. Dereferenzieren ist
	 * das Auslesen eines Werts, dessen Adresse angegeben wurde.
	 *
	 * @param declarations
	 *        Die an dieser Stelle gültigen Deklarationen.
	 * @return Dieser Ausdruck oder ein neuer Ausdruck, falls ein Boxing oder eine
	 *         Dereferenzierung eingefügt wurde.
	 * @throws CompileException
	 *         Während der Kontextanylyse wurde ein Fehler
	 *         gefunden.
	 */
	Expression box(Declarations declarations) throws CompileException {
		if (this.type.isA(ClassDeclaration.intType)) {
			return new BoxExpression(this, declarations);
		} else if (this.type.isA(ClassDeclaration.boolType)) {
			return new BoxExpression(this, declarations);
		} else if (this.lValue) {
			return new DeRefExpression(this);
		} else {
			return this;
		}
	}

	/**
	 * Die Methode prüft, ob dieser Ausdruck dereferenziert, "entboxt" oder beides
	 * werden muss.
	 * Ist dies der Fall, wird ein entsprechender Ausdruck erzeugt, von dem dieser
	 * dann der Operand ist. Dieser neue Ausdruck wird zurückgegeben. Daher sollte diese
	 * Methode immer in der Form "a = a.unBox(...)" aufgerufen werden.
	 * "Unboxing" ist das Auspacken eines Objekts zu einem Basisdatentyp. Dereferenzieren ist
	 * das Auslesen eines Werts, dessen Adresse angegeben wurde.
	 *
	 * @return Dieser Ausdruck oder ein neuer Ausdruck, falls ein Unboxing und/oder eine
	 *         Dereferenzierung eingefügt wurde(n).
	 */
	Expression unBox() {
		if (this.type != ClassDeclaration.nullType
				&& this.type.isA(ClassDeclaration.boolClass)) {
			return new UnBoxExpression(this);
		}

		if (this.lValue) {
			return new DeRefExpression(this).unBox();
		} else if (this.type != ClassDeclaration.nullType
				&& this.type.isA(ClassDeclaration.intClass)) {
			return new UnBoxExpression(this);
		} else {
			return this;
		}
	}

	@Override
	public String toString() {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		TreeStream tree = new TreeStream(stream, 4);
		this.print(tree);

		try {
			return stream.toString("UTF-8");
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}

	/**
	 * If the condition is static, evaluate it to true or false. If the condition
	 * is not static, always return false.
	 *
	 * TODO Must also evaluate more sophisticated expression such as `x == x' to
	 * true.
	 */
	public boolean isAlwaysTrue() {
		return false;
	}
}