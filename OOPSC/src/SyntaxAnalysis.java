import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.tree.ParseTree;

/**
 * Die Klasse realisiert die syntaktische Analyse für die ANTLR4 OOPS Grammatik.
 * Daraus wird der Syntaxbaum aufgebaut, dessen Wurzel die Klasse {@link Program Program} ist.
 */
class SyntaxAnalysis {
	/** Der Datenstrom, aus dem der Quelltext gelesen wird. */
	private final FileInputStream file;

	/** Print the tokens in a LISP-style tree. */
	private final boolean printSymbols;

	protected CompileException err = null;

	public static class CustomErrorListener extends BaseErrorListener {
		protected SyntaxAnalysis syntax;

		public CustomErrorListener(SyntaxAnalysis syntax) {
			this.syntax = syntax;
		}

		@Override
		public void syntaxError(Recognizer<?, ?> recognizer,
				Object offendingSymbol, int line, int charPositionInLine,
				String msg, RecognitionException e) {
			List<String> stack = ((Parser) recognizer).getRuleInvocationStack();
			Collections.reverse(stack);

			msg += " with rule stack " + stack;

			this.syntax.err = new CompileException(msg, new Position(line,
					charPositionInLine));
		}
	}

	/**
	 * Konstruktor.
	 *
	 * @param fileName
	 *        Der Name des Quelltexts.
	 * @param printSymbols
	 *        Die lexikalische Analyse gibt die erkannten
	 *        Symbole auf der Konsole aus.
	 * @throws FileNotFoundException
	 *         Der Quelltext wurde nicht gefunden.
	 * @throws IOException
	 *         Ein Lesefehler ist aufgetreten.
	 */
	SyntaxAnalysis(String fileName, boolean printSymbols)
			throws FileNotFoundException, IOException {
		this.file = new FileInputStream(fileName);
		this.printSymbols = printSymbols;

		ResolvableIdentifier.init();
	}

	/**
	 * Die Methode parsiert den Quelltext und liefert die Wurzel des
	 * Syntaxbaums zurück.
	 *
	 * @throws CompileException
	 *         Der Quelltext entspricht nicht der Syntax.
	 * @throws IOException
	 *         Ein Lesefehler ist aufgetreten.
	 */
	Program parse() throws CompileException, IOException {
		ANTLRInputStream input = new ANTLRInputStream(this.file);
		GrammarLexer lexer = new GrammarLexer(input);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		GrammarParser parser = new GrammarParser(tokens);

		/* Remove ConsoleErrorListener and add our custom error listener. */
		parser.removeErrorListeners();
		parser.addErrorListener(new CustomErrorListener(this));

		this.err = null;

		ParseTree tree = parser.program();

		if (this.printSymbols) {
			System.out.println(tree.toStringTree(parser));
		}

		if (this.err != null) {
			throw this.err;
		}

		Program p = new Program();
		ProgramVisitor visitor = new ProgramVisitor(p);

		visitor.visit(tree);

		return p;
	}
}