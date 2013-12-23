package org.oopsc

import java.io.FileInputStream
import java.util.Collections
import org.antlr.v4.runtime._
import org.oopsc.symbol._
import scala.collection.mutable.ListBuffer
import org.oopsc.expression._
import org.oopsc.statement._
import org.oopsc.symbol.AccessLevel.AccessLevel

class CustomErrorListener(var syntax: SyntaxAnalysis) extends BaseErrorListener {
  override def syntaxError(recognizer: Recognizer[_, _], offendingSymbol: AnyRef, line: Int, charPositionInLine: Int, msg: String, e: RecognitionException) {
    val stack = recognizer.asInstanceOf[Parser].getRuleInvocationStack
    Collections.reverse(stack)

    val message = s"$msg with rule stack $stack"

    val offendingToken = offendingSymbol.asInstanceOf[Token]
    val tokens = recognizer.getInputStream.asInstanceOf[CommonTokenStream]
    val input = tokens.getTokenSource.getInputStream.toString

    /* Collect information for underlining the error. */
    val errorLine = input.split("\n")(line - 1)
    val start = offendingToken.getStartIndex
    val stop = offendingToken.getStopIndex

    throw new CompileException(message, new Position(line, charPositionInLine), errorLine, start, stop)
  }
}

/* Performs syntactic analysis using the ANTLR4 grammar for the OOPS language.
 * Then constructs abstract syntax tree (AST).
 */
class SyntaxAnalysis(fileName: String, var printSymbols: Boolean) {
  private final val file = new FileInputStream(fileName)

  private def identifierFromToken(t: Token): Identifier =
    new Identifier(t.getText, new Position(t.getLine, t.getCharPositionInLine))

  private def resolvableIdentifierFromToken(t: Token): ResolvableSymbol =
    new ResolvableSymbol(new Identifier(t.getText, new Position(t.getLine, t.getCharPositionInLine)))

  private def resolvableClassIdentifierFromToken(t: Token): ResolvableClassSymbol =
    new ResolvableClassSymbol(new Identifier(t.getText, new Position(t.getLine, t.getCharPositionInLine)))

  private def program(ctx: GrammarParser.ProgramContext, p: Program) {
    import scala.collection.JavaConversions._
    for (c <- ctx.classDeclaration()) {
      p.addClass(this.getClassDeclaration(c))
    }
  }

  private def getClassDeclaration(ctx: GrammarParser.ClassDeclarationContext): ClassSymbol = {
    val c = ctx.extendsClass match {
      case null =>
        new ClassSymbol(this.identifierFromToken(ctx.name))
      case e =>
        new ClassSymbol(this.identifierFromToken(ctx.name), this.resolvableClassIdentifierFromToken(e))
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

  private def variableDeclaration(ctx: GrammarParser.VariableDeclarationContext, vars: ListBuffer[VariableSymbol], attr: Boolean, accessLevel: AccessLevel) {
    val `type` = this.identifierFromToken(ctx.`type`.start)

    import scala.collection.JavaConversions._
    for (ident <- ctx.Identifier) {
      val name = this.identifierFromToken(ident.getSymbol)
      vars += (if (attr) {
        val sym = AttributeSymbol.apply(name, `type`)
        sym.accessLevel = accessLevel
        sym
      } else {
        new VariableSymbol(name, `type`)
      })
    }
  }

  private def accessLevel(ctx: GrammarParser.AccessLevelContext): AccessLevel = {
    if (ctx != null) {
      if (ctx.PRIVATE() != null) {
        return AccessLevel.Private
      } else if (ctx.PROTECTED() != null) {
        return AccessLevel.Protected
      }
    }

    AccessLevel.Public
  }

  private def memberVariableDeclaration(ctx: GrammarParser.MemberVariableDeclarationContext, c: ClassSymbol) {
    this.variableDeclaration(ctx.variableDeclaration, c.attributes, true, this.accessLevel(ctx.accessLevel()))
  }

  private def methodBody(ctx: GrammarParser.MethodBodyContext, m: MethodSymbol) {
    import scala.collection.JavaConversions._
    for (variable <- ctx.variableDeclaration) {
      this.variableDeclaration(variable, m.locals, false, null)
    }

    m.statements = this.getStatements(ctx.statements)
  }

  private def methodDeclaration(ctx: GrammarParser.MethodDeclarationContext, c: ClassSymbol) = {
    var m = new MethodSymbol(this.identifierFromToken(ctx.name))
    m.accessLevel = this.accessLevel(ctx.accessLevel())

    c.methods += m

    import scala.collection.JavaConversions._
    for (variable <- ctx.variableDeclaration) {
      this.variableDeclaration(variable, m.parameters, false, null)
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
    val pos = new Position(ctx.start.getLine, ctx.start.getCharPositionInLine)

    rctx match {
      case l: GrammarParser.IntegerLiteralContext =>
        IntegerLiteralExpression(Integer.parseInt(ctx.getText), pos)
      case l: GrammarParser.CharacterLiteralContext =>
        val value = ctx.getText.substring(1, ctx.getText.length - 1)

        if (value == "\\n") {
          CharacterLiteralExpression('\n', pos)
        } else if (value == "\\\\") {
          CharacterLiteralExpression('\\', pos)
        } else if (value.length != 1) {
          throw new CompileException("Unsupported character in literal.", pos)
        } else {
          CharacterLiteralExpression(value.charAt(0), pos)
        }
      case l: GrammarParser.StringLiteralContext =>
        val value = ctx.getText.substring(1, ctx.getText.length - 1)
        StringLiteralExpression(value.replaceAll("\\\\'", "'"), pos)
      case l: GrammarParser.BooleanLiteralContext =>
        BooleanLiteralExpression((l.value.getType == GrammarParser.TRUE), pos)
      case l: GrammarParser.NullLiteralContext =>
        NullLiteralExpression(pos)
    }
  }

  private def getCall(ctx: GrammarParser.CallContext): EvaluateExpression = {
    val call = new EvaluateExpression(this.resolvableIdentifierFromToken(ctx.Identifier.getSymbol))

    import scala.collection.JavaConversions._
    for (e <- ctx.expression) {
      call.addArgument(this.getExpression(e))
    }

    return call
  }

  private def getExpression(ctx: GrammarParser.ExpressionContext): Expression = {
    val rctx: RuleContext = ctx.getRuleContext
    val pos = new Position(ctx.start.getLine, ctx.start.getCharPositionInLine)

    rctx match {
      case e: GrammarParser.BracketsExpressionContext =>
        this.getExpression(e.expression)
      case e: GrammarParser.CallExpressionContext =>
        new AccessExpression(this.getExpression(e.expression), this.getCall(e.call))
      case e: GrammarParser.Call2ExpressionContext =>
        this.getCall(e.call)
      case e: GrammarParser.MemberAccessExpressionContext =>
        new AccessExpression(this.getExpression(e.expression), new EvaluateExpression(this.resolvableIdentifierFromToken(e.Identifier.getSymbol)))
      case e: GrammarParser.MemberAccess2ExpressionContext =>
        new EvaluateExpression(this.resolvableIdentifierFromToken(e.Identifier.getSymbol))
      case e: GrammarParser.LiteralExpressionContext =>
        this.getLiteral(e.literal)
      case e: GrammarParser.SelfExpressionContext =>
        new EvaluateExpression(new ResolvableSymbol(new Identifier("_self", pos)))
      case e: GrammarParser.BaseExpressionContext =>
        return new EvaluateExpression(new ResolvableSymbol(new Identifier("_base", pos)))
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
    val s = new IfStatement(this.getExpression(ctx.expression(0)), this.getStatements(ctx.statements(0)))

    var i = 1

    while (i < ctx.expression.size) {
      s.addIfElse(this.getExpression(ctx.expression(i)), this.getStatements(ctx.statements(i)))
      i += 1
    }

    if (i < ctx.statements.size) {
      s.setElse(this.getStatements(ctx.statements(i)))
    }

    s
  }

  private def getTryStatement(ctx: GrammarParser.TryStatementContext): Statement = {
    val pos = new Position(ctx.start.getLine, ctx.start.getCharPositionInLine)
    val s = new TryStatement(this.getStatements(ctx.statements(0)), pos)

    for (i <- 0 to ctx.literals().size() - 1) {
      s.addCatchBlock(this.getLiterals(ctx.literals(i)), this.getStatements(ctx.statements(i + 1)))
    }

    s
  }

  private def getStatement(ctx: GrammarParser.StatementContext): Statement = {
    val rctx: RuleContext = ctx.getRuleContext
    val pos = new Position(ctx.start.getLine, ctx.start.getCharPositionInLine)

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

  private def getLiterals(ctx: GrammarParser.LiteralsContext) = {
    val literals = scala.collection.JavaConversions.collectionAsScalaIterable(ctx.literal())
    literals.map(this.getLiteral(_)).to[ListBuffer]
  }

  private def getStatements(ctx: GrammarParser.StatementsContext) = {
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

    val tree = parser.program

    if (this.printSymbols) {
      println(tree.toStringTree(parser))
    }

    val p = new Program
    program(tree, p)
    p
  }
}