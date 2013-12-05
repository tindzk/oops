package org.oopsc

import org.antlr.v4.runtime.RuleContext
import org.antlr.v4.runtime.Token
import org.oopsc.expression.AccessExpression
import org.oopsc.expression.BinaryExpression
import org.oopsc.expression.Expression
import org.oopsc.expression.LiteralExpression
import org.oopsc.expression.NewExpression
import org.oopsc.expression.UnaryExpression
import org.oopsc.expression.VarOrCall
import org.oopsc.statement.Assignment
import org.oopsc.statement.CallStatement
import org.oopsc.statement.IfStatement
import org.oopsc.statement.ReadStatement
import org.oopsc.statement.ReturnStatement
import org.oopsc.statement.Statement
import org.oopsc.statement.ThrowStatement
import org.oopsc.statement.TryStatement
import org.oopsc.statement.WhileStatement
import org.oopsc.statement.WriteStatement
import org.oopsc.symbol._
import scala.Some
import scala.collection.mutable.ListBuffer

class ProgramVisitor(var p: Program) extends GrammarBaseVisitor[Void] {
  private var c: ClassSymbol = null
  private var m: MethodSymbol = null

  def identifierFromToken(t: Token): Identifier =
    new Identifier(t.getText, new Position(t.getLine, t.getStartIndex))

  def resolvableIdentifierFromToken(t: Token): ResolvableSymbol =
    new ResolvableSymbol(new Identifier(t.getText, new Position(t.getLine, t.getStartIndex)))

  def resolvableClassIdentifierFromToken(t: Token): ResolvableClassSymbol =
    new ResolvableClassSymbol(new Identifier(t.getText, new Position(t.getLine, t.getStartIndex)))

  override def visitClassDeclaration(ctx: GrammarParser.ClassDeclarationContext): Void = {
    var baseType: ResolvableClassSymbol = null

    if (ctx.extendsClass == null) {
      this.c = new ClassSymbol(this.identifierFromToken(ctx.name))
    } else {
      baseType = this.resolvableClassIdentifierFromToken(ctx.extendsClass)
      this.c = new ClassSymbol(this.identifierFromToken(ctx.name), new Some[ResolvableClassSymbol](baseType))
    }

    this.p.addClass(this.c)
    return this.visitChildren(ctx)
  }

  def varDecl(ctx: GrammarParser.VariableDeclarationContext, vars: ListBuffer[VariableSymbol], attr: Boolean) {
    val `type` = this.identifierFromToken(ctx.`type`.start)

    import scala.collection.JavaConversions._
    for (ident <- ctx.Identifier) {
      val name = this.identifierFromToken(ident.getSymbol)
      vars += (if (attr) new AttributeSymbol(name, `type`) else new VariableSymbol(name, `type`))
    }
  }

  override def visitMemberVariableDeclaration(ctx: GrammarParser.MemberVariableDeclarationContext): Void = {
    this.varDecl(ctx.variableDeclaration, this.c.attributes, true)
    return this.visitChildren(ctx)
  }

  override def visitMethodBody(ctx: GrammarParser.MethodBodyContext): Void = {
    import scala.collection.JavaConversions._
    for (variable <- ctx.variableDeclaration) {
      this.varDecl(variable, this.m.locals, false)
    }

    this.m.statements = this.getStatements(ctx.statements)
    return this.visitChildren(ctx)
  }

  override def visitMethodDeclaration(ctx: GrammarParser.MethodDeclarationContext): Void = {
    this.m = new MethodSymbol(this.identifierFromToken(ctx.name))
    this.c.methods += this.m

    import scala.collection.JavaConversions._
    for (variable <- ctx.variableDeclaration) {
      this.varDecl(variable, this.m.parameters, false)
    }

    if (ctx.`type` != null) {
      val retType = this.identifierFromToken(ctx.`type`.start)
      this.m.retType = retType
    }

    return this.visitChildren(ctx)
  }

  def getLiteral(ctx: GrammarParser.LiteralContext): LiteralExpression = {
    val rctx: RuleContext = ctx.getRuleContext
    val pos = new Position(ctx.start.getLine, ctx.start.getStartIndex)

    if (rctx.isInstanceOf[GrammarParser.IntegerLiteralContext]) {
      return new LiteralExpression(Integer.parseInt(ctx.getText), Types.intType, pos)
    } else if (rctx.isInstanceOf[GrammarParser.CharacterLiteralContext]) {
      val value: String = ctx.getText.substring(1, ctx.getText.length - 1)
      if (value == "\\n") {
        return new LiteralExpression('\n', Types.intType, pos)
      } else if (value == "\\\\") {
        return new LiteralExpression('\\', Types.intType, pos)
      } else if (value.length != 1) {
        /* Unsupported character. */
        assert(false)
      } else {
        return new LiteralExpression(value.charAt(0), Types.intType, pos)
      }
    } else if (rctx.isInstanceOf[GrammarParser.StringLiteralContext]) {
      /* TODO Implement ClassDeclaration.stringType. */
    } else if (rctx.isInstanceOf[GrammarParser.BooleanLiteralContext]) {
      if ((rctx.asInstanceOf[GrammarParser.BooleanLiteralContext]).value.getType == GrammarParser.TRUE) {
        return new LiteralExpression(1, Types.boolType, pos)
      }

      return new LiteralExpression(0, Types.boolType, pos)
    } else if (rctx.isInstanceOf[GrammarParser.NullLiteralContext]) {
      return new LiteralExpression(0, Types.nullType, pos)
    }

    return null
  }

  def getCall(ctx: GrammarParser.CallContext): VarOrCall = {
    val call = new VarOrCall(this.resolvableIdentifierFromToken(ctx.Identifier.getSymbol))

    import scala.collection.JavaConversions._
    for (e <- ctx.expression) {
      call.addArgument(this.getExpression(e))
    }

    return call
  }

  def getExpression(ctx: GrammarParser.ExpressionContext): Expression = {
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

  def getIfStatement(ctx: GrammarParser.IfStatementContext): Statement = {
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

  def getTryStatement(ctx: GrammarParser.TryStatementContext): Statement = {
    val pos = new Position(ctx.start.getLine, ctx.start.getStartIndex)
    val s = new TryStatement(this.getStatements(ctx.statements(0)), pos)

    for (i <- 0 to ctx.literal().size() - 1) {
      s.addCatchBlock(this.getLiteral(ctx.literal(i)), this.getStatements(ctx.statements(i + 1)))
    }

    return s
  }

  def getStatement(ctx: GrammarParser.StatementContext): Statement = {
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

  def getStatements(ctx: GrammarParser.StatementsContext): ListBuffer[Statement] = {
    val stmts = scala.collection.JavaConversions.collectionAsScalaIterable(ctx.statement())
    stmts.map(this.getStatement(_)).to[ListBuffer]
  }
}