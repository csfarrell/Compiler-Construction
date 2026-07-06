import java.util.*;

// Visitor pattern with base ASTNode

// Base ASTNode (abstract) which is for line number tracking on all classes
// https://www.geeksforgeeks.org/system-design/visitor-design-pattern-in-java/
// Loop notes
abstract class ASTNode 
{
    private int line;
    public int getLine() { return line; }
    public void setLine(int line) { this.line = line; }
    public abstract void accept(ASTVisitor visitor);
}

// Visitor for each node type
interface ASTVisitor 
{
    void visit(ProgramNode node);
    void visit(VarDeclNode node);
    void visit(ConstDeclNode node);
    void visit(FunctionNode node);
    void visit(ParameterNode node);
    void visit(MainNode node);
    void visit(AssignmentNode node);
    void visit(FunctionCallStatementNode node);
    void visit(IfStatementNode node);
    void visit(WhileStatementNode node);
    void visit(SkipStatementNode node);
    void visit(StatementBlockNode node);
    void visit(LogicalOrNode node);
    void visit(LogicalAndNode node);
    void visit(EqualityNode node);
    void visit(ComparisonNode node);
    void visit(AddSubNode node);
    void visit(NegationNode node);
    void visit(IdentifierNode node);
    void visit(NumberNode node);
    void visit(BooleanNode node);
    void visit(FunctionCallNode node);
}

// Abstract classes for expression, declaration and statement nodes
abstract class ExpressionNode extends ASTNode 
{
    public CCLType type;
    public CCLType getType() { return type; }
    public void setType(CCLType type) { this.type = type; }
}

abstract class DeclNode extends ASTNode 
{
    public String name;
    public CCLType type;
}

abstract class StatementNode extends ASTNode {}

// Program Node/Main
class ProgramNode extends ASTNode 
{
    public List<DeclNode> decls = new ArrayList<>();
    public List<FunctionNode> funcs = new ArrayList<>();
    public MainNode main;
    public void accept(ASTVisitor visitor) { visitor.visit(this); }
}

// Decl Nodes
class VarDeclNode extends DeclNode 
{
    public VarDeclNode(String name, CCLType type) { this.name = name; this.type = type; }
    public void accept(ASTVisitor visitor) { visitor.visit(this); }
}

class ConstDeclNode extends DeclNode 
{
    public ExpressionNode value;
    public ConstDeclNode(String name, CCLType type, ExpressionNode value) { this.name = name; this.type = type; this.value = value; }
    public void accept(ASTVisitor visitor) { visitor.visit(this); }
}

// Function Nodes
class FunctionNode extends ASTNode 
{
    public CCLType returnType;
    public String name;
    public List<ParameterNode> params = new ArrayList<>();
    public List<DeclNode> decls = new ArrayList<>();
    public StatementBlockNode stmts;
    public ExpressionNode returnExpr;
    public void accept(ASTVisitor visitor) { visitor.visit(this); }
}

class ParameterNode extends ASTNode 
{
    public String name;
    public CCLType type;
    public ParameterNode(String name, CCLType type) { this.name = name; this.type = type; }
    public void accept(ASTVisitor visitor) { visitor.visit(this); }
}

class MainNode extends ASTNode 
{
    public List<DeclNode> decls = new ArrayList<>();
    public StatementBlockNode stmts;
    public void accept(ASTVisitor visitor) { visitor.visit(this); }
}

// Statement Nodes
class AssignmentNode extends StatementNode 
{
    public String varName;
    public ExpressionNode expr;
    public AssignmentNode(String varName, ExpressionNode expr) { this.varName = varName; this.expr = expr; }
    public void accept(ASTVisitor visitor) { visitor.visit(this); }
}

class FunctionCallStatementNode extends StatementNode 
{
    public FunctionCallNode call;
    public FunctionCallStatementNode(FunctionCallNode call) { this.call = call; }
    public void accept(ASTVisitor visitor) { visitor.visit(this); }
}

class IfStatementNode extends StatementNode 
{
    public ExpressionNode cond;
    public StatementBlockNode thenBlock;
    public StatementBlockNode elseBlock;
    public void accept(ASTVisitor visitor) { visitor.visit(this); }
}

class WhileStatementNode extends StatementNode 
{
    public ExpressionNode cond;
    public StatementBlockNode body;
    public void accept(ASTVisitor visitor) { visitor.visit(this); }
}

class SkipStatementNode extends StatementNode 
{
    public void accept(ASTVisitor visitor) { visitor.visit(this); }
}

class StatementBlockNode extends ASTNode 
{
    public List<StatementNode> stmts = new ArrayList<>();
    public void accept(ASTVisitor visitor) { visitor.visit(this); }
}

// Expression Nodes
class LogicalOrNode extends ExpressionNode 
{
    public ExpressionNode left;
    public ExpressionNode right;
    public void accept(ASTVisitor visitor) { visitor.visit(this); }
}

class LogicalAndNode extends ExpressionNode 
{
    public ExpressionNode left;
    public ExpressionNode right;
    public void accept(ASTVisitor visitor) { visitor.visit(this); }
}

class EqualityNode extends ExpressionNode 
{
    public String operator;
    public ExpressionNode left;
    public ExpressionNode right;
    public void accept(ASTVisitor visitor) { visitor.visit(this); }
}

class ComparisonNode extends ExpressionNode 
{
    public String operator;
    public ExpressionNode left;
    public ExpressionNode right;
    public void accept(ASTVisitor visitor) { visitor.visit(this); }
}

class AddSubNode extends ExpressionNode 
{
    public String operator;
    public ExpressionNode left;
    public ExpressionNode right;
    public void accept(ASTVisitor visitor) { visitor.visit(this); }
}

class NegationNode extends ExpressionNode 
{
    public String operator;
    public ExpressionNode operand;
    public void accept(ASTVisitor visitor) { visitor.visit(this); }
}

class IdentifierNode extends ExpressionNode 
{
    public String name;
    public IdentifierNode(String name) { this.name = name; }
    public void accept(ASTVisitor visitor) { visitor.visit(this); }
}

class NumberNode extends ExpressionNode 
{
    public int value;
    public NumberNode(int value) { this.value = value; this.type = CCLType.INTEGER; }
    public void accept(ASTVisitor visitor) { visitor.visit(this); }
}

class BooleanNode extends ExpressionNode 
{
    public boolean value;
    public BooleanNode(boolean value) { this.value = value; this.type = CCLType.BOOLEAN; }
    public void accept(ASTVisitor visitor) { visitor.visit(this); }
}

class FunctionCallNode extends ExpressionNode 
{
    public String funcName;
    public List<ExpressionNode> args = new ArrayList<>();
    public void accept(ASTVisitor visitor) { visitor.visit(this); }
}