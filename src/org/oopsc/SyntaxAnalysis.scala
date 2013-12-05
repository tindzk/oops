package org.oopsc

import java.io.FileInputStream
import java.util.Collections
import org.antlr.v4.runtime._

/**
 * Die Klasse realisiert die syntaktische Analyse für die ANTLR4 OOPS Grammatik.
 * Daraus wird der Syntaxbaum aufgebaut, dessen Wurzel die Klasse {@link Program Program} ist.
 */
class CustomErrorListener(var syntax: SyntaxAnalysis) extends BaseErrorListener {
  override def syntaxError(recognizer: Recognizer[_, _], offendingSymbol: AnyRef, line: Int, charPositionInLine: Int, msg: String, e: RecognitionException) {
    val stack = (recognizer.asInstanceOf[Parser]).getRuleInvocationStack
    Collections.reverse(stack)

    val message = s"$msg with rule stack $stack"

    val offendingToken = offendingSymbol.asInstanceOf[Token]
    val tokens = recognizer.getInputStream.asInstanceOf[CommonTokenStream]
    val input = tokens.getTokenSource.getInputStream.toString

    /* Collect information for underlining the error. */
    val errorLine = input.split("\n")(line - 1)
    val start = offendingToken.getStartIndex
    val stop = offendingToken.getStopIndex

    this.syntax.err = new CompileException(message, new Position(line, charPositionInLine), errorLine, start, stop)
  }
}

class SyntaxAnalysis(fileName: String, var printSymbols: Boolean) {
  /** Der Datenstrom, aus dem der Quelltext gelesen wird. */
  private final val file = new FileInputStream(fileName)

  var err: CompileException = null

  /**
   * Die Methode parsiert den Quelltext und liefert die Wurzel des
   * Syntaxbaums zurück.
   */
  def parse: Program = {
    val input = new ANTLRInputStream(this.file)
    val lexer = new GrammarLexer(input)
    val tokens = new CommonTokenStream(lexer)
    val parser = new GrammarParser(tokens)

    /* Remove ConsoleErrorListener and add our custom error listener. */
    parser.removeErrorListeners
    parser.addErrorListener(new CustomErrorListener(this))

    this.err = null
    val tree = parser.program

    if (this.printSymbols) {
      System.out.println(tree.toStringTree(parser))
    }

    if (this.err != null) {
      throw this.err
    }

    val p = new Program
    val visitor = new ProgramVisitor(p)

    visitor.visit(tree)

    return p
  }
}