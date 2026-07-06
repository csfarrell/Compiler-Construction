import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import java.util.*;

// Build AST Tree from antlr raw tree for each node in AST.java

// Extend base visitor uses visitor pattern for traversal
public class ASTBuilder extends CCLBaseVisitor<ASTNode>
{
    // Declare decl, main and function (lists)
    @Override
    public ASTNode visitProgram(CCLParser.ProgramContext ctx)
    {
        ProgramNode program = new ProgramNode();

        if (ctx.start != null)
        {
            program.setLine(ctx.start.getLine());
        }

        if (ctx.decl_list() != null)
        {
            program.decls = buildDeclList(ctx.decl_list());
        }

        if (ctx.function_list() != null)
        {
            program.funcs = buildFunctionList(ctx.function_list());
        }

        if (ctx.main() != null)
        {
            program.main = (MainNode) visit(ctx.main());
        }

        return program;
    }

    // Decl
    // Process decl lists recurisve
    private List<DeclNode> buildDeclList(CCLParser.Decl_listContext ctx)
    {
        List<DeclNode> decls = new ArrayList<>();

        if (ctx.decl() != null)
        {
            ASTNode first = visit(ctx.decl());

            if (first instanceof DeclNode)
            {
                decls.add((DeclNode) first);
            }

            if (ctx.decl_list() != null)
            {
                decls.addAll(buildDeclList(ctx.decl_list()));
            }
        }

        return decls;
    }

    // Make node for variable decl
    @Override
    public ASTNode visitVar_decl(CCLParser.Var_declContext ctx)
    {
        String name = ctx.IDENTIFIER().getText();
        CCLType type = CCLTypeConverter.fromString(ctx.type().getText());

        VarDeclNode node = new VarDeclNode(name, type);

        if (ctx.start != null)
        {
            node.setLine(ctx.start.getLine());
        }

        return node;
    }

    // Make node for constants with intial values
    @Override
    public ASTNode visitConst_decl(CCLParser.Const_declContext ctx)
    {
        String name = ctx.IDENTIFIER().getText();
        CCLType type = CCLTypeConverter.fromString(ctx.type().getText());
        ExpressionNode value = (ExpressionNode) visit(ctx.expression());

        ConstDeclNode node = new ConstDeclNode(name, type, value);

        if (ctx.start != null)
        {
            node.setLine(ctx.start.getLine());
        }

        return node;
    }

    // Function
    // Process multi function definitions
    private List<FunctionNode> buildFunctionList(CCLParser.Function_listContext ctx)
    {
        List<FunctionNode> funcs = new ArrayList<>();

        if (ctx.function() != null)
        {
            funcs.add((FunctionNode) visit(ctx.function()));

            if (ctx.function_list() != null)
            {
                funcs.addAll(buildFunctionList(ctx.function_list()));
            }
        }

        return funcs;
    }

    // Make function nodes with params, locals and body
    @Override
    public ASTNode visitFunction(CCLParser.FunctionContext ctx) {
        FunctionNode fn = new FunctionNode();

        if (ctx.start != null) 
        {
            fn.setLine(ctx.start.getLine());
        }

        fn.returnType = CCLTypeConverter.fromString(ctx.type().getText());
        fn.name = ctx.IDENTIFIER().getText();

        if (ctx.parameter_list() != null && ctx.parameter_list().nemp_parameter_list() != null) 
        {
            fn.params = buildNempParameterList(ctx.parameter_list().nemp_parameter_list());
        }

        if (ctx.decl_list() != null) 
        {
            fn.decls = buildDeclList(ctx.decl_list());
        }

        // System.out.println("Debug: Function " + fn.name + " - visiting statement_block");
        if (ctx.statement_block() != null) 
        {
            ASTNode stmtsNode = visit(ctx.statement_block());
            // System.out.println("Debug: Function " + fn.name + " - visited statement_block, got: " + stmtsNode.getClass().getSimpleName());
            
            if (stmtsNode instanceof StatementBlockNode) 
            {
                StatementBlockNode stmtBlock = (StatementBlockNode) stmtsNode;
                fn.stmts = stmtBlock;
                // System.out.println("Debug: Function " + fn.name + " - statement block has " + stmtBlock.stmts.size() + " statements");
            }
        } 
        else 
        {
            System.out.println("Debug: Function " + fn.name + " - NO statement_block found!");
        }

        if (ctx.expression() != null) 
        {
            fn.returnExpr = (ExpressionNode) visit(ctx.expression());
        }
        return fn;
    }

    // Non empty parameters
    private List<ParameterNode> buildNempParameterList(CCLParser.Nemp_parameter_listContext ctx)
    {
        List<ParameterNode> params = new ArrayList<>();

        if (ctx == null)
        {
            return params;
        }

        ParameterNode first = new ParameterNode(ctx.IDENTIFIER().getText(), CCLTypeConverter.fromString(ctx.type().getText()));

        if (ctx.start != null)
        {
            first.setLine(ctx.start.getLine());
        }

        params.add(first);

        if (ctx.nemp_parameter_list() != null)
        {
            params.addAll(buildNempParameterList(ctx.nemp_parameter_list()));
        }

        return params;
    }

    // Main block
    @Override
    public ASTNode visitMain(CCLParser.MainContext ctx)
    {
        MainNode main = new MainNode();

        if (ctx.start != null)
        {
            main.setLine(ctx.start.getLine());
        }

        if (ctx.decl_list() != null)
        {
            main.decls = buildDeclList(ctx.decl_list());
        }

        if (ctx.statement_block() != null)
        {
            main.stmts = (StatementBlockNode) visit(ctx.statement_block());
        }

        return main;
    }

    // Statements
    // Make block of statements 
    // (process, get fist statement, if statementnode add all its statements) 
    // (recusrisve check all statements, add statemetns from nested block)
    @Override
    public ASTNode visitStatement_block(CCLParser.Statement_blockContext ctx)
    {
        StatementBlockNode block = new StatementBlockNode();

        if (ctx.start != null) 
        {
            block.setLine(ctx.start.getLine());
        }

        if (ctx.statement() != null) 
        {
            ASTNode firstStmt = visit(ctx.statement());

            if (firstStmt instanceof StatementNode) 
            {
                block.stmts.add((StatementNode) firstStmt);
            } 
            else if (firstStmt instanceof StatementBlockNode) 
            {
                block.stmts.addAll(((StatementBlockNode) firstStmt).stmts);
            }
            
            if (ctx.statement_block() != null) 
            {
                ASTNode restBlock = visit(ctx.statement_block());
                if (restBlock instanceof StatementBlockNode) 
                {
                    block.stmts.addAll(((StatementBlockNode) restBlock).stmts);
                }
            }
        }

        // System.out.println("Debug: Built statement block with " + block.stmts.size() + " statements");
        return block;
    }

    // Statement, handles if/while, assign, func call, skip and nested blocks
    @Override
    public ASTNode visitStatement(CCLParser.StatementContext ctx)
    {
        // System.out.println("Debug: Process statement - " + (ctx.getText().length() > 50 ? ctx.getText().substring(0, 50) + : ctx.getText()));
        
        StatementNode stmt = null;

        if (ctx.IF() != null)
        {
            // System.out.println("Debug: Found an IF statement");
            IfStatementNode ifn = new IfStatementNode();
            ifn.cond = (ExpressionNode) visit(ctx.condition());
            
            // Statement_block (if->then)
            ASTNode thenBlock = visit(ctx.statement_block(0));
            if (thenBlock instanceof StatementBlockNode) 
            {
                ifn.thenBlock = (StatementBlockNode) thenBlock;
            } 
            else 
            {
                ifn.thenBlock = new StatementBlockNode();
                ifn.thenBlock.stmts.add((StatementNode) thenBlock);
            }
            
            if (ctx.statement_block(1) != null) 
            {
                ASTNode elseBlock = visit(ctx.statement_block(1));
                if (elseBlock instanceof StatementBlockNode) 
                {
                    ifn.elseBlock = (StatementBlockNode) elseBlock;
                } 
                else 
                {
                    ifn.elseBlock = new StatementBlockNode();
                    ifn.elseBlock.stmts.add((StatementNode) elseBlock);
                }
            }
            stmt = ifn;
        }
        else if (ctx.WHILE() != null)
        {
            // System.out.println("Debug: Found a WHILE statement");
            WhileStatementNode wn = new WhileStatementNode();
            wn.cond = (ExpressionNode) visit(ctx.condition());
            
            ASTNode body = visit(ctx.statement_block(0));
            if (body instanceof StatementBlockNode) 
            {
                wn.body = (StatementBlockNode) body;
            } 
            else 
            {
                wn.body = new StatementBlockNode();
                wn.body.stmts.add((StatementNode) body);
            }
            stmt = wn;
        }
        else if (ctx.ASSIGN() != null)
        {
            // System.out.println("Debug: Found an assignment statement");
            String varName = ctx.IDENTIFIER().getText();
            ExpressionNode expr = (ExpressionNode) visit(ctx.expression());
            stmt = new AssignmentNode(varName, expr);
        }
        else if (ctx.LBRACK() != null && ctx.IDENTIFIER() != null && ctx.arg_list() != null && ctx.ASSIGN() == null && ctx.RBRACK() != null && ctx.SEMICOL() != null && ctx.IF() == null && ctx.WHILE() == null)
        {
            // System.out.println("Debug: Found a function call statement");
            FunctionCallNode call = new FunctionCallNode();
            call.funcName = ctx.IDENTIFIER().getText();
            call.args = buildArgList(ctx.arg_list());
            stmt = new FunctionCallStatementNode(call);
        }
        else if (ctx.SKIP_() != null)
        {
            // System.out.println("Debug: Found a SKIP statement");
            stmt = new SkipStatementNode();
        }
        else if (ctx.LBRACE() != null && ctx.statement_block() != null && ctx.RBRACE() != null)
        {
            // System.out.println("Debug: Found a block statement");
            return visit(ctx.statement_block(0));
        }
        else
        {
            // System.out.println("Debug: Fall back to visitChildren");
            ASTNode child = visitChildren(ctx);

            if (child instanceof StatementNode)
            {
                stmt = (StatementNode) child;
            }
        }

        if (stmt != null && ctx.start != null)
        {
            stmt.setLine(ctx.start.getLine());
        } 
        else 
        {
            System.out.println("Debug: Statement is null");
        }

        // System.out.println("Debug: Returning statement type: " + (stmt != null ? stmt.getClass().getSimpleName() : "null"));
        return stmt;
    }

    // Expressions (arithmetic/func calls)
    @Override
    public ASTNode visitExpression(CCLParser.ExpressionContext ctx)
    {
        if (ctx.fragment_() != null && ctx.binary_arith_op() != null && ctx.fragment_(1) != null)
        {
            ExpressionNode left = (ExpressionNode) visit(ctx.fragment_(0));
            ExpressionNode right = (ExpressionNode) visit(ctx.fragment_(1));

            AddSubNode node = new AddSubNode();
            node.operator = ctx.binary_arith_op().getText();
            node.left = left;
            node.right = right;

            if (ctx.start != null)
            {
                node.setLine(ctx.start.getLine());
            }

            return node;
        }
        else if (ctx.LBRACK() != null && ctx.expression() != null && ctx.RBRACK() != null)
        {
            ExpressionNode inner = (ExpressionNode) visit(ctx.expression());

            if (ctx.start != null)
            {
                inner.setLine(ctx.start.getLine());
            }

            return inner;
        }
        else if (ctx.IDENTIFIER() != null && ctx.LBRACK() != null && ctx.arg_list() != null && ctx.RBRACK() != null)
        {
            FunctionCallNode call = new FunctionCallNode();
            call.funcName = ctx.IDENTIFIER().getText();
            call.args = buildArgList(ctx.arg_list());

            if (ctx.start != null)
            {
                call.setLine(ctx.start.getLine());
            }

            return call;
        }
        else if (ctx.fragment_() != null)
        {
            return visit(ctx.fragment_(0));
        }

        return null;
    }

    // Fragment expressions (ids, literals, negations)
    @Override
    public ASTNode visitFragment_(CCLParser.Fragment_Context ctx)
    {
        ExpressionNode expr = null;

        if (ctx.IDENTIFIER() != null && ctx.MINUS() == null)
        {
            IdentifierNode id = new IdentifierNode(ctx.IDENTIFIER().getText());

            if (ctx.start != null)
            {
                id.setLine(ctx.start.getLine());
            }

            expr = id;
        }
        else if (ctx.MINUS() != null && ctx.IDENTIFIER() != null)
        {
            IdentifierNode id = new IdentifierNode(ctx.IDENTIFIER().getText());
            NegationNode neg = new NegationNode();
            neg.operator = "-";
            neg.operand = id;

            if (ctx.start != null)
            {
                neg.setLine(ctx.start.getLine());
            }

            expr = neg;
        }
        else if (ctx.NUMBER() != null)
        {
            int v = Integer.parseInt(ctx.NUMBER().getText());
            NumberNode n = new NumberNode(v);

            if (ctx.start != null)
            {
                n.setLine(ctx.start.getLine());
            }

            expr = n;
        }
        else if (ctx.TRUE() != null)
        {
            BooleanNode b = new BooleanNode(true);

            if (ctx.start != null)
            {
                b.setLine(ctx.start.getLine());
            }

            expr = b;
        }
        else if (ctx.FALSE() != null)
        {
            BooleanNode b = new BooleanNode(false);

            if (ctx.start != null)
            {
                b.setLine(ctx.start.getLine());
            }

            expr = b;
        }
        else if (ctx.LBRACK() != null && ctx.expression() != null && ctx.RBRACK() != null)
        {
            ExpressionNode inner = (ExpressionNode) visit(ctx.expression());

            if (ctx.start != null)
            {
                inner.setLine(ctx.start.getLine());
            }

            expr = inner;
        }

        return expr;
    }

    // Boolean expresisons and comparisions/negations
    @Override
    public ASTNode visitCondition(CCLParser.ConditionContext ctx)
    {
        if (ctx.TILDE() != null)
        {
            ExpressionNode operand = (ExpressionNode) visit(ctx.condition(0));

            NegationNode n = new NegationNode();
            n.operator = "~";
            n.operand = operand;

            if (ctx.start != null)
            {
                n.setLine(ctx.start.getLine());
            }

            return n;
        }
        else if (ctx.LBRACK() != null && ctx.condition(0) != null && ctx.RBRACK() != null)
        {
            ExpressionNode inner = (ExpressionNode) visit(ctx.condition(0));

            if (ctx.start != null)
            {
                inner.setLine(ctx.start.getLine());
            }

            return inner;
        }
        else if (ctx.comp_op() != null)
        {
            ExpressionNode left = (ExpressionNode) visit(ctx.expression(0));
            ExpressionNode right = (ExpressionNode) visit(ctx.expression(1));
            String op = ctx.comp_op().getText();

            if (op.equals("==") || op.equals("!="))
            {
                EqualityNode eq = new EqualityNode();
                eq.operator = op;
                eq.left = left;
                eq.right = right;

                if (ctx.start != null)
                {
                    eq.setLine(ctx.start.getLine());
                }

                return eq;
            }
            else
            {
                ComparisonNode cp = new ComparisonNode();
                cp.operator = op;
                cp.left = left;
                cp.right = right;

                if (ctx.start != null)
                {
                    cp.setLine(ctx.start.getLine());
                }

                return cp;
            }
        }
        else if (ctx.OR() != null)
        {
            ExpressionNode left = (ExpressionNode) visit(ctx.condition(0));
            ExpressionNode right = (ExpressionNode) visit(ctx.condition(1));

            LogicalOrNode or = new LogicalOrNode();
            or.left = left;
            or.right = right;

            if (ctx.start != null)
            {
                or.setLine(ctx.start.getLine());
            }

            return or;
        }
        else if (ctx.AND() != null)
        {
            ExpressionNode left = (ExpressionNode) visit(ctx.condition(0));
            ExpressionNode right = (ExpressionNode) visit(ctx.condition(1));

            LogicalAndNode and = new LogicalAndNode();
            and.left = left;
            and.right = right;

            if (ctx.start != null)
            {
                and.setLine(ctx.start.getLine());
            }

            return and;
        }

        return null;
    }

    // Argument list
    private List<ExpressionNode> buildArgList(CCLParser.Arg_listContext ctx)
    {
        List<ExpressionNode> args = new ArrayList<>();

        if (ctx == null)
        {
            return args;
        }

        if (ctx.nemp_arg_list() != null)
        {
            args.addAll(buildNempArgList(ctx.nemp_arg_list()));
        }

        return args;
    }

    // Non empty args
    private List<ExpressionNode> buildNempArgList(CCLParser.Nemp_arg_listContext ctx)
    {
        List<ExpressionNode> args = new ArrayList<>();

        if (ctx == null)
        {
            return args;
        }

        IdentifierNode id = new IdentifierNode(ctx.IDENTIFIER().getText());

        if (ctx.start != null)
        {
            id.setLine(ctx.start.getLine());
        }

        args.add(id);

        if (ctx.nemp_arg_list() != null)
        {
            args.addAll(buildNempArgList(ctx.nemp_arg_list()));
        }

        return args;
    }

    // Utility

    @Override
    public ASTNode visitTerminal(TerminalNode node)
    {
        return null;
    }

    @Override
    protected ASTNode aggregateResult(ASTNode aggregate, ASTNode nextResult)
    {
        if (nextResult != null)
        {
            return nextResult;
        }

        return aggregate;
    }

    @Override
    public ASTNode visitChildren(RuleNode node)
    {
        return super.visitChildren(node);
    }
}