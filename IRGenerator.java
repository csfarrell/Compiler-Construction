import java.util.*;
import java.io.*;

// https://www.geeksforgeeks.org/compiler-design/three-address-code-compiler/
// Loop notes
public class IRGenerator implements ASTVisitor 
{
    private List<String> code = new ArrayList<>();
    private int tempCount = 1;
    private int labelCount = 1;
    private SymbolTable symbolTable;
    private String currentFunction;

    public IRGenerator(SymbolTable symbolTable) 
    {
        this.symbolTable = symbolTable;
    }

    public void generateCode(ProgramNode ast) 
    {
        code.clear();
        tempCount = 1;
        labelCount = 1;

        code.add("// CCL File generated in 3-Addr Code for TACi");
        code.add("");

        // Generate functions
        for (FunctionNode func : ast.funcs) 
        {
            func.accept(this);
            code.add("");
        }

        // Generate main
        code.add("main:");
        
        ast.main.accept(this);
        
        code.add(" call _exit, 0");
    }

    public void writeToFile(String filename) throws IOException 
    {
        try (PrintWriter out = new PrintWriter(new FileWriter(filename))) 
        {
            for (String line : code) 
            {
                out.println(line);
            }
        }
    }

    // Create temp variable
    private String newTemp() 
    {
        return "t" + (tempCount++);
    }

    // Create label (Format: L1, L2 ... LN)
    private String newLabel(String prefix) 
    {
        String label = prefix + labelCount++;
        return label;
    }

    // Handle main declarations and statements
    public void visit(MainNode node) 
    {
        
        for (DeclNode decl : node.decls) {
            if (decl instanceof VarDeclNode) 
            {
                VarDeclNode varDecl = (VarDeclNode) decl;
                code.add(" " + varDecl.name + " = 0");
            } 
            else if (decl instanceof ConstDeclNode) 
            {
                ConstDeclNode constDecl = (ConstDeclNode) decl;
                String value = generateExpression(constDecl.value);
                code.add(" " + constDecl.name + " = " + value);
            }
        }

        if (node.stmts != null) 
        {
            node.stmts.accept(this);
        }
    }

    // Handle function (process params, local var)
    public void visit(FunctionNode node) {
        currentFunction = node.name;
        code.add(node.name + ":");
        
        // Parameters
        for (int i = 0; i < node.params.size(); i++) 
        {
            ParameterNode param = node.params.get(i);
            code.add(" " + param.name + " = getparam " + (i + 1));
        }

        // Variable declarations
        for (DeclNode decl : node.decls) 
        {
            if (decl instanceof VarDeclNode) 
            {
                VarDeclNode varDecl = (VarDeclNode) decl;
                code.add(" " + varDecl.name + " = 0");
            } 
            else if (decl instanceof ConstDeclNode) 
            {
                ConstDeclNode constDecl = (ConstDeclNode) decl;
                String value = generateExpression(constDecl.value);
                code.add(" " + constDecl.name + " = " + value);
            }
        }

        // Body
        if (node.stmts != null) 
        {
            node.stmts.accept(this);
        }

        // Return
        if (node.returnExpr != null) 
        {
            String retVal = generateExpression(node.returnExpr);
            code.add(" return " + retVal);
        } 
        else 
        {
            code.add(" return");
        }
        currentFunction = null;
    }

    // Statement block handling
    public void visit(StatementBlockNode node) 
    {
        for (StatementNode stmt : node.stmts) 
        {
            stmt.accept(this);
        }
    }

    // Assign nodes (var assignment)
    public void visit(AssignmentNode node) 
    {
        String value = generateExpression(node.expr);
        code.add(" " + node.varName + " = " + value);
    }

    // Call function statement
    public void visit(FunctionCallStatementNode node) 
    {
        generateFunctionCall(node.call, null);
    }

    // Conditional branching (if and else)
    public void visit(IfStatementNode node) 
    {
        String elseLabel = newLabel("L");
        String endLabel = newLabel("L");
        
        // Generate condition using direct comparison in conditional jump
        generateCondition(node.cond, elseLabel);
        
        // Then block
        if (node.thenBlock != null) 
        {
            node.thenBlock.accept(this);
        }
        
        if (node.elseBlock != null) 
        {
            code.add(" goto " + endLabel);
            code.add(elseLabel + ":");
            node.elseBlock.accept(this);
        } 
        else 
        {
            code.add(elseLabel + ":");
        }
        
        code.add(endLabel + ":");
    }

    // While loop generation (recursive)
    public void visit(WhileStatementNode node) 
    {
        String startLabel = newLabel("L");
        String endLabel = newLabel("L");
        
        code.add(startLabel + ":");
        
        // Condition check
        generateCondition(node.cond, endLabel);
        
        // Loop
        if (node.body != null) 
        {
            node.body.accept(this);
        }
        
        code.add(" goto " + startLabel);
        code.add(endLabel + ":");
    }

    // Skip does nothing
    public void visit(SkipStatementNode node) {}

    // Handle conditional statements (bool, negation)
    // And: left false, jump to false, else check right
    // Or: left true, jump to rest, else check right
    // Comparison: ifz x relop y goto L
    // Equality: Same as comparison but specifically "==" and "!="
    // Negation: Use temp for negation
    // If all fail, just generate as expression
    private void generateCondition(ExpressionNode cond, String falseLabel) 
    {
        if (cond instanceof LogicalAndNode) 
        {
            LogicalAndNode andNode = (LogicalAndNode) cond;
            String midLabel = newLabel("L");
            generateCondition(andNode.left, falseLabel);
            code.add(midLabel + ":");
            generateCondition(andNode.right, falseLabel);
        } 
        else if (cond instanceof LogicalOrNode) 
        {
            LogicalOrNode orNode = (LogicalOrNode) cond;
            String trueLabel = newLabel("L");
            generateCondition(orNode.left, trueLabel);
            generateCondition(orNode.right, falseLabel);
            code.add(trueLabel + ":");
        } 
        else if (cond instanceof ComparisonNode) 
        {
            ComparisonNode compNode = (ComparisonNode) cond;
            String left = generateSimpleExpression(compNode.left);
            String right = generateSimpleExpression(compNode.right);
            code.add(" ifz " + left + " " + compNode.operator + " " + right + " goto " + falseLabel);
        } 
        else if (cond instanceof EqualityNode) 
        {
            EqualityNode eqNode = (EqualityNode) cond;
            String left = generateSimpleExpression(eqNode.left);
            String right = generateSimpleExpression(eqNode.right);
            String op = eqNode.operator.equals("==") ? "==" : "!=";
            code.add(" ifz " + left + " " + op + " " + right + " goto " + falseLabel);
        } 
        else if (cond instanceof NegationNode && "~".equals(((NegationNode) cond).operator)) 
        {
            NegationNode negNode = (NegationNode) cond;
            String tempLabel = newLabel("L");
            generateCondition(negNode.operand, tempLabel);
            code.add(" goto " + falseLabel);
            code.add(tempLabel + ":");
        } 
        else 
        {
            String condResult = generateExpression(cond);
            code.add(" ifz " + condResult + " goto " + falseLabel);
        }
    }

    // Create temp vars, handle expression types with TAC operations as spec
    // If fail create temp value = 0
    private String generateExpression(ExpressionNode expr) 
    {
        if (expr instanceof IdentifierNode) 
        {
            return ((IdentifierNode) expr).name;
        } 
        else if (expr instanceof NumberNode) 
        {
            int value = ((NumberNode) expr).value;
            if (value < 0) 
            {
                String temp = newTemp();
                code.add(" " + temp + " = 0 - " + Math.abs(value));
                return temp;
            }
            return String.valueOf(value);
        } 
        else if (expr instanceof BooleanNode) 
        {
            return ((BooleanNode) expr).value ? "1" : "0";
        } 
        else if (expr instanceof FunctionCallNode) 
        {
            FunctionCallNode call = (FunctionCallNode) expr;
            String result = newTemp();
            generateFunctionCall(call, result);
            return result;
        } 
        else if (expr instanceof LogicalOrNode) 
        {
            LogicalOrNode orNode = (LogicalOrNode) expr;
            String left = generateExpression(orNode.left);
            String right = generateExpression(orNode.right);
            String result = newTemp();
            code.add(" " + result + " = " + left + " || " + right);
            return result;
        } 
        else if (expr instanceof LogicalAndNode) 
        {
            LogicalAndNode andNode = (LogicalAndNode) expr;
            String left = generateExpression(andNode.left);
            String right = generateExpression(andNode.right);
            String result = newTemp();
            code.add(" " + result + " = " + left + " && " + right);
            return result;
        } 
        else if (expr instanceof EqualityNode) 
        {
            EqualityNode eqNode = (EqualityNode) expr;
            String left = generateExpression(eqNode.left);
            String right = generateExpression(eqNode.right);
            String result = newTemp();
            String op = eqNode.operator.equals("==") ? "==" : "!=";
            code.add(" " + result + " = " + left + " " + op + " " + right);
            return result;
        } 
        else if (expr instanceof ComparisonNode) 
        {
            ComparisonNode compNode = (ComparisonNode) expr;
            String left = generateExpression(compNode.left);
            String right = generateExpression(compNode.right);
            String result = newTemp();
            code.add(" " + result + " = " + left + " " + compNode.operator + " " + right);
            return result;
        } 
        else if (expr instanceof AddSubNode) 
        {
            AddSubNode addSubNode = (AddSubNode) expr;
            String left = generateExpression(addSubNode.left);
            String right = generateExpression(addSubNode.right);
            String result = newTemp();
            code.add(" " + result + " = " + left + " " + addSubNode.operator + " " + right);
            return result;
        } 
        else if (expr instanceof NegationNode) 
        {
            NegationNode negNode = (NegationNode) expr;
            String operand = generateExpression(negNode.operand);
            String result = newTemp();
            if ("~".equals(negNode.operator)) 
            {
                code.add(" " + result + " = ~ " + operand);
            } 
            else if ("-".equals(negNode.operator)) 
            {
                code.add(" " + result + " = 0 - " + operand);
            }
            return result;
        }
        
        String temp = newTemp();
        code.add(" " + temp + " = 0");
        return temp;
    }

    // Helper to catch simple expressions first
    // Was made to optimise for ids, bools, literals
    private String generateSimpleExpression(ExpressionNode expr) 
    {
        if (expr instanceof IdentifierNode) 
        {
            return ((IdentifierNode) expr).name;
        }
        else if (expr instanceof NumberNode) 
        {
            int value = ((NumberNode) expr).value;
            if (value < 0) 
            {
                String temp = newTemp();
                code.add(" " + temp + " = 0 - " + Math.abs(value));
                return temp;
            }
            return String.valueOf(value);
        }
        else if (expr instanceof BooleanNode) 
        {
            return ((BooleanNode) expr).value ? "1" : "0";
        } 
        else 
        {
            return generateExpression(expr);
        }
    }

    // Reverse order, procedure and function calls
    private void generateFunctionCall(FunctionCallNode call, String resultVar) 
    {
        // Push parameters in reverse order
        for (int i = call.args.size() - 1; i >= 0; i--) 
        {
            String arg = generateExpression(call.args.get(i));
            code.add(" param " + arg);
        }

        if (resultVar != null) 
        {
            code.add(" " + resultVar + " = call " + call.funcName + ", " + call.args.size());
        } 
        else 
        {
            code.add(" call " + call.funcName + ", " + call.args.size());
        }
    }

    // Not used in IR generation but used in AST 
    public void visit(ProgramNode node) {}
    public void visit(VarDeclNode node) {}
    public void visit(ConstDeclNode node) {}
    public void visit(ParameterNode node) {}
    public void visit(FunctionCallNode node) {}
    public void visit(IdentifierNode node) {}
    public void visit(NumberNode node) {}
    public void visit(BooleanNode node) {}
    public void visit(LogicalOrNode node) {}
    public void visit(LogicalAndNode node) {}
    public void visit(EqualityNode node) {}
    public void visit(ComparisonNode node) {}
    public void visit(AddSubNode node) {}
    public void visit(NegationNode node) {}
}