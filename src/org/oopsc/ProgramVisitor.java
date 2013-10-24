package org.oopsc;
import java.util.LinkedList;
import java.util.List;

import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.oopsc.expression.AccessExpression;
import org.oopsc.expression.BinaryExpression;
import org.oopsc.expression.Expression;
import org.oopsc.expression.LiteralExpression;
import org.oopsc.expression.NewExpression;
import org.oopsc.expression.UnaryExpression;
import org.oopsc.expression.VarOrCall;
import org.oopsc.statement.Assignment;
import org.oopsc.statement.CallStatement;
import org.oopsc.statement.IfStatement;
import org.oopsc.statement.ReadStatement;
import org.oopsc.statement.ReturnStatement;
import org.oopsc.statement.Statement;
import org.oopsc.statement.ThrowStatement;
import org.oopsc.statement.TryStatement;
import org.oopsc.statement.WhileStatement;
import org.oopsc.statement.WriteStatement;

public class ProgramVisitor extends GrammarBaseVisitor<Void> {

	final private Program p;

	private ClassDeclaration c = null;
	private MethodDeclaration m = null;

	public ProgramVisitor(Program p) {
		this.p = p;
	}

	public Identifier identifierFromToken(Token t) {
		return new Identifier(t.getText(), new Position(t.getLine(),
				t.getStartIndex()));
	}

	public ResolvableIdentifier resolvableIdentifierFromToken(Token t) {
		return new ResolvableIdentifier(t.getText(), new Position(t.getLine(),
				t.getStartIndex()));
	}

	@Override
	public Void visitClassDeclaration(GrammarParser.ClassDeclarationContext ctx) {
		ResolvableIdentifier baseType;

		if (ctx.extendsClass != null) {
			baseType = this.resolvableIdentifierFromToken(ctx.extendsClass);
		} else {
			baseType = new ResolvableIdentifier("Object", null);
		}

		this.c = new ClassDeclaration(this.identifierFromToken(ctx.name), baseType);
		this.p.addClass(this.c);

		return this.visitChildren(ctx);
	}

	public void varDecl(GrammarParser.VariableDeclarationContext ctx,
			List<VarDeclaration> vars, VarDeclaration.Type declType) {
		ResolvableIdentifier type = this.resolvableIdentifierFromToken(ctx
				.type().start);

		for (TerminalNode ident : ctx.Identifier()) {
			VarDeclaration var = new VarDeclaration(
					this.identifierFromToken(ident.getSymbol()), declType);
			var.type = type;
			vars.add(var);
		}
	}

	@Override
	public Void visitMemberVariableDeclaration(
			GrammarParser.MemberVariableDeclarationContext ctx) {
		this.varDecl(ctx.variableDeclaration(), this.c.attributes,
				VarDeclaration.Type.Attribute);
		return this.visitChildren(ctx);
	}

	@Override
	public Void visitMethodBody(GrammarParser.MethodBodyContext ctx) {
		for (GrammarParser.VariableDeclarationContext var : ctx
				.variableDeclaration()) {
			this.varDecl(var, this.m.locals, VarDeclaration.Type.Local);
		}

		this.m.statements = this.getStatements(ctx.statements());
		return this.visitChildren(ctx);
	}

	@Override
	public Void visitMethodDeclaration(
			GrammarParser.MethodDeclarationContext ctx) {
		this.m = new MethodDeclaration(this.identifierFromToken(ctx.name));
		this.c.methods.add(this.m);

		for (GrammarParser.VariableDeclarationContext var : ctx
				.variableDeclaration()) {
			this.varDecl(var, this.m.parameters, VarDeclaration.Type.Local);
		}

		if (ctx.type() != null) {
			ResolvableIdentifier retType = this
					.resolvableIdentifierFromToken(ctx.type().start);
			this.m.setReturnType(retType);
		}

		return this.visitChildren(ctx);
	}

	public LiteralExpression getLiteral(GrammarParser.LiteralContext ctx) {
		RuleContext rctx = ctx.getRuleContext();

		Position pos = new Position(ctx.start.getLine(),
				ctx.start.getStartIndex());

		if (rctx instanceof GrammarParser.IntegerLiteralContext) {
			return new LiteralExpression(Integer.parseInt(ctx.getText()),
					ClassDeclaration.intType, pos);
		} else if (rctx instanceof GrammarParser.CharacterLiteralContext) {
			String value = ctx.getText().substring(1,
					ctx.getText().length() - 1);

			if (value.equals("\\n")) {
				return new LiteralExpression('\n', ClassDeclaration.intType,
						pos);
			} else if (value.equals("\\\\")) {
				return new LiteralExpression('\\', ClassDeclaration.intType,
						pos);
			} else if (value.length() != 1) {
				/* Unsupported character. */
				assert (false);
			} else {
				return new LiteralExpression(value.charAt(0),
						ClassDeclaration.intType, pos);
			}
		} else if (rctx instanceof GrammarParser.StringLiteralContext) {
			/* TODO Implement ClassDeclaration.stringType. */
		} else if (rctx instanceof GrammarParser.BooleanLiteralContext) {
			if (((GrammarParser.BooleanLiteralContext) rctx).value.getType() == GrammarParser.TRUE) {
				return new LiteralExpression(1, ClassDeclaration.boolType, pos);
			}

			return new LiteralExpression(0, ClassDeclaration.boolType, pos);
		} else if (rctx instanceof GrammarParser.NullLiteralContext) {
			return new LiteralExpression(0, ClassDeclaration.nullType, pos);
		}

		return null;
	}

	public VarOrCall getCall(GrammarParser.CallContext ctx) {
		VarOrCall call = new VarOrCall(this.resolvableIdentifierFromToken(ctx
				.Identifier().getSymbol()));

		for (GrammarParser.ExpressionContext e : ctx.expression()) {
			call.arguments.add(this.getExpression(e));
		}

		return call;
	}

	public Expression getExpression(GrammarParser.ExpressionContext ctx) {
		Position pos = new Position(ctx.start.getLine(),
				ctx.start.getStartIndex());

		RuleContext rctx = ctx.getRuleContext();

		if (rctx instanceof GrammarParser.BracketsExpressionContext) {
			return this
					.getExpression(((GrammarParser.BracketsExpressionContext) rctx)
							.expression());
		} else if (rctx instanceof GrammarParser.CallExpressionContext) {
			return new AccessExpression(
					this.getExpression(((GrammarParser.CallExpressionContext) rctx)
							.expression()),
					this.getCall(((GrammarParser.CallExpressionContext) rctx)
							.call()));
		} else if (rctx instanceof GrammarParser.Call2ExpressionContext) {
			return this.getCall(((GrammarParser.Call2ExpressionContext) rctx)
					.call());
		} else if (rctx instanceof GrammarParser.MemberAccessExpressionContext) {
			return new AccessExpression(
					this.getExpression(((GrammarParser.MemberAccessExpressionContext) rctx)
							.expression()),
					new VarOrCall(
							this.resolvableIdentifierFromToken(((GrammarParser.MemberAccessExpressionContext) rctx)
									.Identifier().getSymbol())));
		} else if (rctx instanceof GrammarParser.MemberAccess2ExpressionContext) {
			return new VarOrCall(
					this.resolvableIdentifierFromToken(((GrammarParser.MemberAccess2ExpressionContext) rctx)
							.Identifier().getSymbol()));
		} else if (rctx instanceof GrammarParser.LiteralExpressionContext) {
			return this
					.getLiteral(((GrammarParser.LiteralExpressionContext) rctx)
							.literal());
		} else if (rctx instanceof GrammarParser.SelfExpressionContext) {
			return new VarOrCall(new ResolvableIdentifier("_self", pos));
		} else if (rctx instanceof GrammarParser.BaseExpressionContext) {
			return new VarOrCall(new ResolvableIdentifier("_base", pos));
		} else if (rctx instanceof GrammarParser.MinusExpressionContext) {
			return new UnaryExpression(
					Symbol.Id.MINUS,
					this.getExpression(((GrammarParser.MinusExpressionContext) rctx)
							.expression()), pos);
		} else if (rctx instanceof GrammarParser.NegateExpressionContext) {
			return new UnaryExpression(
					Symbol.Id.NOT,
					this.getExpression(((GrammarParser.NegateExpressionContext) rctx)
							.expression()), pos);
		} else if (rctx instanceof GrammarParser.InstantiateExpressionContext) {
			return new NewExpression(
					this.resolvableIdentifierFromToken(((GrammarParser.InstantiateExpressionContext) rctx)
							.Identifier().getSymbol()), pos);
		} else if (rctx instanceof GrammarParser.MulDivModExpressionContext) {
			int tokenOp = ((GrammarParser.MulDivModExpressionContext) rctx).op
					.getType();
			Symbol.Id op = null;

			if (tokenOp == GrammarParser.MUL) {
				op = Symbol.Id.MUL;
			} else if (tokenOp == GrammarParser.DIV) {
				op = Symbol.Id.DIV;
			} else if (tokenOp == GrammarParser.MOD) {
				op = Symbol.Id.MOD;
			}

			return new BinaryExpression(
					this.getExpression(((GrammarParser.MulDivModExpressionContext) rctx)
							.expression(0)),
					op,
					this.getExpression(((GrammarParser.MulDivModExpressionContext) rctx)
							.expression(1)));
		} else if (rctx instanceof GrammarParser.AddSubExpressionContext) {
			int tokenOp = ((GrammarParser.AddSubExpressionContext) rctx).op
					.getType();
			Symbol.Id op = null;

			if (tokenOp == GrammarParser.ADD) {
				op = Symbol.Id.PLUS;
			} else if (tokenOp == GrammarParser.SUB) {
				op = Symbol.Id.MINUS;
			}

			return new BinaryExpression(
					this.getExpression(((GrammarParser.AddSubExpressionContext) rctx)
							.expression(0)),
					op,
					this.getExpression(((GrammarParser.AddSubExpressionContext) rctx)
							.expression(1)));
		} else if (rctx instanceof GrammarParser.CompareExpressionContext) {
			int tokenOp = ((GrammarParser.CompareExpressionContext) rctx).op
					.getType();
			Symbol.Id op = null;

			if (tokenOp == GrammarParser.LEQ) {
				op = Symbol.Id.LTEQ;
			} else if (tokenOp == GrammarParser.GEQ) {
				op = Symbol.Id.GTEQ;
			} else if (tokenOp == GrammarParser.LT) {
				op = Symbol.Id.LT;
			} else if (tokenOp == GrammarParser.GT) {
				op = Symbol.Id.GT;
			}

			return new BinaryExpression(
					this.getExpression(((GrammarParser.CompareExpressionContext) rctx)
							.expression(0)),
					op,
					this.getExpression(((GrammarParser.CompareExpressionContext) rctx)
							.expression(1)));
		} else if (rctx instanceof GrammarParser.ConjunctionExpressionContext) {
			return new BinaryExpression(
					this.getExpression(((GrammarParser.ConjunctionExpressionContext) rctx)
							.expression(0)),
					Symbol.Id.AND,
					this.getExpression(((GrammarParser.ConjunctionExpressionContext) rctx)
							.expression(1)));
		} else if (rctx instanceof GrammarParser.DisjunctionExpressionContext) {
			return new BinaryExpression(
					this.getExpression(((GrammarParser.DisjunctionExpressionContext) rctx)
							.expression(0)),
					Symbol.Id.OR,
					this.getExpression(((GrammarParser.DisjunctionExpressionContext) rctx)
							.expression(1)));
		} else if (rctx instanceof GrammarParser.EqualityExpressionContext) {
			Symbol.Id op = null;

			if (((GrammarParser.EqualityExpressionContext) rctx).EQ() != null) {
				op = Symbol.Id.EQ;
			} else {
				op = Symbol.Id.NEQ;
			}

			return new BinaryExpression(
					this.getExpression(((GrammarParser.EqualityExpressionContext) rctx)
							.expression(0)),
					op,
					this.getExpression(((GrammarParser.EqualityExpressionContext) rctx)
							.expression(1)));
		}

		return null;
	}

	public Statement getIfStatement(GrammarParser.IfStatementContext ctx) {
		IfStatement s = new IfStatement(this.getExpression(ctx.expression(0)),
				this.getStatements(ctx.statements(0)));

		int i = 1;

		for (; i < ctx.expression().size(); i++) {
			s.addIfElse(this.getExpression(ctx.expression(i)),
					this.getStatements(ctx.statements(i)));
		}

		if (i < ctx.statements().size()) {
			s.setElse(this.getStatements(ctx.statements(i)));
		}

		return s;
	}

	public Statement getTryStatement(GrammarParser.TryStatementContext ctx) {
		Position pos = new Position(ctx.start.getLine(),
				ctx.start.getStartIndex());

		TryStatement s = new TryStatement(
				this.getStatements(ctx.statements(0)), pos);

		for (int i = 0; i < ctx.literal().size(); i++) {
			s.addCatchBlock(this.getLiteral(ctx.literal(i)),
					this.getStatements(ctx.statements(i + 1)));
		}

		return s;
	}

	public Statement getStatement(GrammarParser.StatementContext ctx) {
		RuleContext rctx = ctx.getRuleContext();

		Position pos = new Position(ctx.start.getLine(),
				ctx.start.getStartIndex());

		if (rctx instanceof GrammarParser.IfStatementContext) {
			return this.getIfStatement((GrammarParser.IfStatementContext) rctx);
		} else if (rctx instanceof GrammarParser.TryStatementContext) {
			return this
					.getTryStatement((GrammarParser.TryStatementContext) rctx);
		} else if (rctx instanceof GrammarParser.WhileStatementContext) {
			WhileStatement w = new WhileStatement(
					this.getExpression(((GrammarParser.WhileStatementContext) rctx)
							.expression()));
			w.statements = this
					.getStatements(((GrammarParser.WhileStatementContext) rctx)
							.statements());
			return w;
		} else if (rctx instanceof GrammarParser.ReadStatementContext) {
			return new ReadStatement(
					this.getExpression(((GrammarParser.ReadStatementContext) rctx)
							.expression()));
		} else if (rctx instanceof GrammarParser.WriteStatementContext) {
			return new WriteStatement(
					this.getExpression(((GrammarParser.WriteStatementContext) rctx)
							.expression()));
		} else if (rctx instanceof GrammarParser.ReturnStatementContext) {
			if (((GrammarParser.ReturnStatementContext) rctx).expression() != null) {
				return new ReturnStatement(
						this.getExpression(((GrammarParser.ReturnStatementContext) rctx)
								.expression()), pos);
			}

			return new ReturnStatement(pos);
		} else if (rctx instanceof GrammarParser.ThrowStatementContext) {
			return new ThrowStatement(
					this.getExpression(((GrammarParser.ThrowStatementContext) rctx)
							.expression()), pos);
		} else if (rctx instanceof GrammarParser.AssignStatementContext) {
			return new Assignment(
					this.getExpression(((GrammarParser.AssignStatementContext) rctx)
							.expression(0)),
					this.getExpression(((GrammarParser.AssignStatementContext) rctx)
							.expression(1)));
		} else if (rctx instanceof GrammarParser.ExpressionStatementContext) {
			return new CallStatement(
					this.getExpression(((GrammarParser.ExpressionStatementContext) rctx)
							.expression()));
		}

		return null;
	}

	public List<Statement> getStatements(GrammarParser.StatementsContext ctx) {
		List<Statement> stmts = new LinkedList<>();

		for (GrammarParser.StatementContext stmt : ctx.statement()) {
			stmts.add(this.getStatement(stmt));
		}

		return stmts;
	}

}