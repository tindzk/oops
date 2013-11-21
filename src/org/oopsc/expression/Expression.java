package org.oopsc.expression;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;

import org.oopsc.*;
import org.oopsc.symbol.*;

import scala.Some;

/**
 * Die abstrakte Basisklasse für alle Ausdrücke im Syntaxbaum.
 * Zusätzlich zur Standardschnittstelle für Ausdrücke definiert sie auch
 * Methoden zur Erzeugung neuer Ausdrücke für das Boxing und Unboxing von
 * Ausdrücken sowie das Dereferenzieren.
 */
abstract public class Expression {
	/** Der Typ dieses Ausdrucks. Solange er nicht bekannt ist, ist dieser Eintrag null. */
	// TODO delete, provide helper method instead
	public ClassSymbol type;

	/**
	 * Ist dieser Ausdruck ein L-Wert, d.h. eine Referenz auf eine Variable?
	 * Die meisten Ausdrücke sind keine L-Werte.
	 */
	public boolean lValue = false;

	/** Die Quelltextposition, an der dieser Ausdruck beginnt. */
	public Position position;

	/**
	 * Konstruktor.
	 *
	 * @param position
	 *        Die Quelltextposition, an der dieser Ausdruck beginnt.
	 */
	public Expression(Position position) {
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
	 * @param sem
	 *        Die an dieser Stelle gültigen Deklarationen.
	 * @return Dieser Ausdruck oder ein neuer Ausdruck, falls ein Boxing,
	 *         Unboxing oder eine Dereferenzierung in den Baum eingefügt
	 *         wurden.
	 * @throws CompileException
	 *         Während der Kontextanylyse wurde ein Fehler
	 *         gefunden.
	 */
	// TODO do not return anything here
	public Expression refPass(SemanticAnalysis sem) throws CompileException {
		return this;
	}

	/**
	 * Die Methode gibt diesen Ausdruck in einer Baumstruktur aus.
	 * Wenn der Typ des Ausdrucks bereits ermittelt wurde, wird er auch ausgegeben.
	 *
	 * @param tree
	 *        Der Strom, in den die Ausgabe erfolgt.
	 */
	abstract public void print(TreeStream tree);

	/**
	 * Die Methode generiert den Assembler-Code für diesen Ausdruck. Sie geht
	 * davon aus, dass die Kontextanalyse vorher erfolgreich abgeschlossen wurde.
	 *
	 * @param code
	 *        Der Strom, in den die Ausgabe erfolgt.
	 */
	abstract public void generateCode(CodeStream code);

	protected void generateDeRefCode(CodeStream code) {
		code.println("; DEREF");
		code.println("MRM R5, (R2)");
		code.println("MRM R5, (R5)");
		code.println("MMR (R2), R5");
	}

	protected void generateBoxCode(CodeStream code) {
		code.println("; BOX");
		code.println("MRM R5, (R2) ; Wert vom Stapel nehmen");
		code.println("SUB R2, R1");
		code.println("MRM R6, (R2) ; Referenz auf neues Objekt holen (bleibt auf Stapel)");
		code.println("MRI R7, " + ClassSymbol.HEADERSIZE());
		code.println("ADD R6, R7 ; Speicherstelle in neuem Objekt berechnen");
		code.println("MMR (R6), R5 ; Wert in Objekt speichern");
	}

	protected void generateUnBoxCode(CodeStream code) {
		if (this.type == Types.boolClass()
				|| this.type == Types.intClass()) {
			code.println("; UNBOX type = " + this.type.identifier().name());
			code.println("MRM R5, (R2) ; Objektreferenz vom Stapel lesen");
			code.println("MRI R6, " + ClassSymbol.HEADERSIZE());
			code.println("ADD R5, R6 ; Adresse des Werts bestimmen");
			code.println("MRM R5, (R5) ; Wert auslesen");
			code.println("MMR (R2), R5 ; und auf den Stapel schreiben");
		}
	}

	public void generateCode(CodeStream code, boolean box) {
		if (box) {
			if (this.type == Types.intType()
					|| this.type == Types.boolType()) {
				NewExpression newType = null;

				if (this.type == Types.intType()) {
					newType = new NewExpression(
							new ResolvableClassSymbol(new Identifier("Integer",
									new Position(0, 0)), null));
					newType.newType.declaration_$eq(new Some<>(Types
							.intClass()));
				} else {
					newType = new NewExpression(
							new ResolvableClassSymbol(new Identifier("Boolean",
									new Position(0, 0)), null));
					newType.newType.declaration_$eq(new Some<>(Types
							.boolClass()));
				}

				newType.generateCode(code);
				this.generateCode(code);
				this.generateBoxCode(code);
			} else {
				this.generateCode(code);

				if (this.lValue) {
					this.generateDeRefCode(code);
				}
			}
		} else {
			this.generateCode(code);

			if (this.lValue) {
				this.generateDeRefCode(code);
			}

			this.generateUnBoxCode(code);
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
	public boolean isAlwaysTrue(SemanticAnalysis sem) {
		return false;
	}
}