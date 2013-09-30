import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;

/**
 * Die Klasse repräsentiert die Anweisung IF-THEN im Syntaxbaum.
 */
class IfStatement extends Statement {
    /** Die Bedingung der IF-Anweisung. */
    Expression condition;

    /** Die Anweisungen im THEN-Teil. */
    LinkedList<Statement> thenStatements = new LinkedList<Statement>();

    /** Die ELSE-IF-Anweisungen und der ELSE-Block. */
    public HashMap<Expression, LinkedList<Statement>> elseStatements = new HashMap<>();

    /**
     * Konstruktor.
     * @param condition Die Bedingung der IF-Anweisung.
     */
    IfStatement(Expression condition) {
        this.condition = condition;
    }

    /**
     * Die Methode führt die Kontextanalyse für diese Anweisung durch.
     * @param declarations Die an dieser Stelle gültigen Deklarationen.
     * @throws CompileException Während der Kontextanylyse wurde ein Fehler
     *         gefunden.
     */
    @Override
	void contextAnalysis(Declarations declarations) throws CompileException {
        this.condition = this.condition.contextAnalysis(declarations);
        this.condition = this.condition.unBox();
        this.condition.type.check(ClassDeclaration.boolType, this.condition.position);

        for (Statement s : this.thenStatements) {
            s.contextAnalysis(declarations);
        }

        for (Entry<Expression, LinkedList<Statement>> entry : this.elseStatements.entrySet()) {
	        for (Statement s : entry.getValue()) {
	            s.contextAnalysis(declarations);
	        }
        }
    }

	public void addIfElse(Expression condition, LinkedList<Statement> stmts) {
		this.elseStatements.put(condition, stmts);
	}

	public void setElse(LinkedList<Statement> stmts) {
		this.elseStatements.put(null, stmts);
	}

	private void print(TreeStream tree, Expression condition, LinkedList<Statement> stmts) {
        tree.indent();

        if (condition != null) {
        	condition.print(tree);
        } else {
        	tree.println("ELSE");
        }

        if (!stmts.isEmpty()) {
        	if (condition != null) {
        		tree.println("THEN");
        	}
            tree.indent();
            for (Statement s : stmts) {
                s.print(tree);
            }
            tree.unindent();
        }
        tree.unindent();
	}

    /**
     * Die Methode gibt diese Anweisung in einer Baumstruktur aus.
     * @param tree Der Strom, in den die Ausgabe erfolgt.
     */
    @Override
	void print(TreeStream tree) {
        tree.println("IF");
        this.print(tree, this.condition, this.thenStatements);

        for (Entry<Expression, LinkedList<Statement>> entry : this.elseStatements.entrySet()) {
        	if (entry.getKey() == null) {
        		continue;
        	}

	        this.print(tree, entry.getKey(), entry.getValue());
        }

        if (this.elseStatements.containsKey(null)) {
	        this.print(tree, null, this.elseStatements.get(null));
        }
    }

    /**
     * Die Methode generiert den Assembler-Code für diese Anweisung. Sie geht
     * davon aus, dass die Kontextanalyse vorher erfolgreich abgeschlossen wurde.
     * @param code Der Strom, in den die Ausgabe erfolgt.
     */
    @Override
	void generateCode(CodeStream code) {
        String endLabel = code.nextLabel();
        String lblNextIf = code.nextLabel();

        code.println("; IF");

        this.condition.generateCode(code);

        code.println("MRM R5, (R2) ; Bedingung vom Stapel nehmen");
        code.println("SUB R2, R1");
        code.println("ISZ R5, R5 ; Wenn 0, dann");
        code.println("JPC R5, " + lblNextIf + " ; Sprung zu END IF bzw. nächstem ELSEIF/ELSE");
        code.println("; THEN");

        for (Statement s : this.thenStatements) {
            s.generateCode(code);
        }

        for (Entry<Expression, LinkedList<Statement>> entry : this.elseStatements.entrySet()) {
        	if (entry.getKey() == null) {
        		continue;
        	}

	        code.println(lblNextIf + ":");
	        lblNextIf = code.nextLabel();

	        code.println("; ELSEIF");
	        entry.getKey().generateCode(code);

	        code.println("MRM R5, (R2) ; Bedingung vom Stapel nehmen");
	        code.println("SUB R2, R1");
	        code.println("ISZ R5, R5 ; Wenn 0, dann");
	        code.println("JPC R5, " + lblNextIf + " ; Sprung zu END IF bzw. nächstem ELSEIF/ELSE");
	        code.println("; THEN");

	        for (Statement s : entry.getValue()) {
	            s.generateCode(code);
	        }

	        code.println("MRI R0, " + endLabel + " ; Sprung zu END IF");

	        code.println("; END ELSEIF");
        }

        code.println(lblNextIf + ":");

        if (this.elseStatements.containsKey(null)) {
	        code.println("; ELSE");

	        for (Statement s : this.elseStatements.get(null)) {
	            s.generateCode(code);
	        }

	        code.println("; END ELSE");
        }

        code.println("; END IF");
        code.println(endLabel + ":");
    }

}