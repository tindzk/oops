package org.oopsc

import java.io.FileInputStream
import java.util.Collections
import org.antlr.v4.runtime._
import org.oopsc.symbol._
import scala.collection.mutable.ListBuffer
import org.oopsc.expression._
import org.oopsc.statement._

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

  private def identifierFromToken(t: Token): Identifier =
    new Identifier(t.getText, new Position(t.getLine, t.getStartIndex))

  private def resolvableIdentifierFromToken(t: Token): ResolvableSymbol =
    new ResolvableSymbol(new Identifier(t.getText, new Position(t.getLine, t.getStartIndex)))

  private def resolvableClassIdentifierFromToken(t: Token): ResolvableClassSymbol =
    new ResolvableClassSymbol(new Identifier(t.getText, new Position(t.getLine, t.getStartIndex)))

  private def program(ctx: GrammarParser.ProgramContext, p: Program) {
    import scala.collection.JavaConversions._
    for (c <- ctx.classDeclaration()) {
      p.addClass(this.getClassDeclaration(c))
    }
  }

  private def getClassDeclaration(ctx: GrammarParser.ClassDeclarationContext): ClassSymbol = {
    var c: ClassSymbol = null

    if (ctx.extendsClass == null) {
      c = new ClassSymbol(this.identifierFromToken(ctx.name))
    } else {
      val baseType = this.resolvableClassIdentifierFromToken(ctx.extendsClass)
      c = new ClassSymbol(this.identifierFromToken(ctx.name), Some(baseType))
    }

    import scala.collection.JavaConversions._
    for (m <- ctx.memberDeclaration()) {
      if (m.memberVariableDeclaration() != null) {
        memberVariableDeclaration(m.memberVariableDeclaration(), c)
      } else if (m.methodDeclaration() != null) {
        methodDeclaration(m.methodDeclaration(), c)
      }
    }

    c
  }

  private def variableDeclaration(ctx: GrammarParser.VariableDeclarationContext, vars: ListBuffer[VariableSymbol], attr: Boolean) {
    val `type` = this.identifierFromToken(ctx.`type`.start)

    import scala.collection.JavaConversions._
    for (ident <- ctx.Identifier) {
      val name = this.identifierFromToken(ident.getSymbol)
      vars += (if (attr) new AttributeSymbol(name, `type`) else new VariableSymbol(name, `type`))
    }
  }

  private def memberVariableDeclaration(ctx: GrammarParser.MemberVariableDeclarationContext, c: ClassSymbol) {
    this.variableDeclaration(ctx.variableDeclaration, c.attributes, true)
  }

  private def methodBody(ctx: GrammarParser.MethodBodyContext, m: MethodSymbol) {
    import scala.collection.JavaConversions._
    for (variable <- ctx.variableDeclaration) {
      this.variableDeclaration(variable, m.locals, false)
    }

    m.statements = this.getStatements(ctx.statements)
  }

  private def methodDeclaration(ctx: GrammarParser.MethodDeclarationContext, c: ClassSymbol) = {
    var m = new MethodSymbol(this.identifierFromToken(ctx.name))
    c.methods += m

    import scala.collection.JavaConversions._
    for (variable <- ctx.variableDeclaration) {
      this.variableDeclaration(variable, m.parameters, false)
    }

    if (ctx.`type` != null) {
      val retType = this.identifierFromToken(ctx.`type`.start)
      m.retType = retType
    }

    methodBody(ctx.methodBody(), m)
    m
  }

  private def getLiteral(ctx: GrammarParser.LiteralContext): LiteralExpression = {
    val rctx: RuleContext = ctx.getRuleContext
    val pos = new Position(ctx.start.getLine, ctx.start.getStartIndex)

    rctx match {
      case l: GrammarParser.IntegerLiteralContext =>
        new LiteralExpression(Integer.parseInt(ctx.getText), Types.intType, pos)
      case l: GrammarParser.CharacterLiteralContext =>
        val value = ctx.getText.substring(1, ctx.getText.length - 1)

        if (value == "\\n") {
          new LiteralExpression('\n', Types.intType, pos)
        } else if (value == "\\\\") {
          new LiteralExpression('\\', Types.intType, pos)
        } else if (value.length != 1) {
          throw new CompileException("Unsupported character in literal.", pos)
        } else {
          new LiteralExpression(value.charAt(0), Types.intType, pos)
        }
      case l: GrammarParser.StringLiteralContext =>
        /* TODO Implement ClassDeclaration.stringType. */
        null
      case l: GrammarParser.BooleanLiteralContext =>
        if (l.value.getType == GrammarParser.TRUE) {
          new LiteralExpression(1, Types.boolType, pos)
        } else {
          new LiteralExpression(0, Types.boolType, pos)
        }
      case l: GrammarParser.NullLiteralContext =>
        new LiteralExpression(0, Types.nullType, pos)
    }
  }

  private def getCall(ctx: GrammarParser.CallContext): VarOrCall = {
    val call = new VarOrCall(this.resolvableIdentifierFromToken(ctx.Identifier.getSymbol))

    import scala.collection.JavaConversions._
    for (e <- ctx.expression) {
      call.addArgument(this.getExpression(e))
    }

    return call
  }

  private def getExpression(ctx: GrammarParser.ExpressionContext): Expression = {
    val rctx: RuleContext = ctx.getRuleContext
    val pos: Position = new Position(ctx.start.getLine, ctx.start.getStartIndex)

    rctx match {
      case e: GrammarParser.BracketsExpressionContext =>
        this.getExpression(e.expression)
      case e: GrammarParser.CallExpressionContext =>
        new AccessExpression(this.getExpression(e.expression), this.getCall(e.call))
      case e: GrammarParser.Call2ExpressionContext =>
        this.getCall(e.call)
      case e: GrammarParser.MemberAccessExpressionContext =>
        new AccessExpression(this.getExpression(e.expression), new VarOrCall(this.resolvableIdentifierFromToken(e.Identifier.getSymbol)))
      case e: GrammarParser.MemberAccess2ExpressionContext =>
        new VarOrCall(this.resolvableIdentifierFromToken(e.Identifier.getSymbol))
      case e: GrammarParser.LiteralExpressionContext =>
        this.getLiteral(e.literal)
      case e: GrammarParser.SelfExpressionContext =>
        new VarOrCall(new ResolvableSymbol(new Identifier("_self", pos)))
      case e: GrammarParser.BaseExpressionContext =>
        return new VarOrCall(new ResolvableSymbol(new Identifier("_base", pos)))
      case e: GrammarParser.MinusExpressionContext =>
        new UnaryExpression(UnaryExpression.MINUS, this.getExpression(e.expression), pos)
      case e: GrammarParser.NegateExpressionContext =>
        new UnaryExpression(UnaryExpression.NOT, this.getExpression(e.expression), pos)
      case e: GrammarParser.InstantiateExpressionContext =>
        new NewExpression(this.resolvableClassIdentifierFromToken(e.Identifier.getSymbol))
      case e: GrammarParser.MulDivModExpressionContext =>
        val op = e.op.getType match {
          case GrammarParser.MUL => BinaryExpression.MUL
          case GrammarParser.DIV => BinaryExpression.DIV
          case GrammarParser.MOD => BinaryExpression.MOD
        }

        new BinaryExpression(this.getExpression(e.expression(0)), op, this.getExpression(e.expression(1)))
      case e: GrammarParser.AddSubExpressionContext =>
        val op = e.op.getType match {
          case GrammarParser.ADD => BinaryExpression.PLUS
          case GrammarParser.SUB => BinaryExpression.MINUS
        }

        return new BinaryExpression(this.getExpression(e.expression(0)), op, this.getExpression(e.expression(1)))
      case e: GrammarParser.CompareExpressionContext =>
        val op = e.op.getType match {
          case GrammarParser.LEQ => BinaryExpression.LTEQ
          case GrammarParser.GEQ => BinaryExpression.GTEQ
          case GrammarParser.LT  => BinaryExpression.LT
          case GrammarParser.GT  => BinaryExpression.GT
        }

        new BinaryExpression(this.getExpression(e.expression(0)), op, this.getExpression(e.expression(1)))
      case e: GrammarParser.ConjunctionExpressionContext =>
        new BinaryExpression(this.getExpression(e.expression(0)), BinaryExpression.AND, this.getExpression(e.expression(1)))
      case e: GrammarParser.DisjunctionExpressionContext =>
        new BinaryExpression(this.getExpression(e.expression(0)), BinaryExpression.OR, this.getExpression(e.expression(1)))
      case e: GrammarParser.EqualityExpressionContext =>
        val op = if (e.EQ != null) BinaryExpression.EQ else BinaryExpression.NEQ
        new BinaryExpression(this.getExpression(e.expression(0)), op, this.getExpression(e.expression(1)))
    }
  }

  private def getIfStatement(ctx: GrammarParser.IfStatementContext): Statement = {
    val s: IfStatement = new IfStatement(this.getExpression(ctx.expression(0)), this.getStatements(ctx.statements(0)))

    var i = 1

    while (i < ctx.expression().size()) {
      s.addIfElse(this.getExpression(ctx.expression(i)), this.getStatements(ctx.statements(i)))
      i += 1
    }

    if (i < ctx.statements.size) {
      s.setElse(this.getStatements(ctx.statements(i)))
    }

    return s
  }

  private def getTryStatement(ctx: GrammarParser.TryStatementContext): Statement = {
    val pos = new Position(ctx.start.getLine, ctx.start.getStartIndex)
    val s = new TryStatement(this.getStatements(ctx.statements(0)), pos)

    for (i <- 0 to ctx.literal().size() - 1) {
      s.addCatchBlock(this.getLiteral(ctx.literal(i)), this.getStatements(ctx.statements(i + 1)))
    }

    return s
  }

  private def getStatement(ctx: GrammarParser.StatementContext): Statement = {
    val rctx: RuleContext = ctx.getRuleContext
    val pos = new Position(ctx.start.getLine, ctx.start.getStartIndex)

    rctx match {
      case s: GrammarParser.IfStatementContext =>
        this.getIfStatement(s)
      case s: GrammarParser.TryStatementContext =>
        this.getTryStatement(s)
      case s: GrammarParser.WhileStatementContext =>
        new WhileStatement(this.getExpression(s.expression), this.getStatements(s.statements))
      case s: GrammarParser.ReadStatementContext =>
        new ReadStatement(this.getExpression(s.expression))
      case s: GrammarParser.WriteStatementContext =>
        new WriteStatement(this.getExpression(s.expression))
      case s: GrammarParser.ReturnStatementContext =>
        if (s.expression == null) {
          new ReturnStatement(pos)
        } else {
          new ReturnStatement(pos, this.getExpression(s.expression))
        }
      case s: GrammarParser.ThrowStatementContext =>
        new ThrowStatement(this.getExpression(s.expression), pos)
      case s: GrammarParser.AssignStatementContext =>
        new Assignment(this.getExpression(s.expression(0)), this.getExpression(s.expression(1)))
      case s: GrammarParser.ExpressionStatementContext =>
        new CallStatement(this.getExpression(s.expression))
    }
  }

  private def getStatements(ctx: GrammarParser.StatementsContext): ListBuffer[Statement] = {
    val stmts = scala.collection.JavaConversions.collectionAsScalaIterable(ctx.statement())
    stmts.map(this.getStatement(_)).to[ListBuffer]
  }

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
    program(tree, p)
    p
  }
}