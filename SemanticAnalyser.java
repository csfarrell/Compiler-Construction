import java.util.*;

// https://pgrandinetti.github.io/compilers/page/what-is-semantic-analysis-in-compilers/
// Loop notes and Java Documentation
public class SemanticAnalyser implements ASTVisitor 
{
    private SymbolTable symbolTable = new SymbolTable();
    private List<String> errors = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
    private String currentFunction;
    
    public boolean analyse(ProgramNode ast) 
    {
        errors.clear();
        warnings.clear();
        addBuiltInFunctions();
        ast.accept(this);
        checkUnusedSymbols();
        return errors.isEmpty();
    }
    
    // Built in TAC functions
    private void addBuiltInFunctions() 
    {
        symbolTable.addSymbol("_read", CCLType.INTEGER, SymbolKind.FUNCTION, 0);
        symbolTable.addSymbol("_print", CCLType.VOID, SymbolKind.FUNCTION, 0);
        symbolTable.addSymbol("_println", CCLType.VOID, SymbolKind.FUNCTION, 0);
        symbolTable.addSymbol("_exit", CCLType.VOID, SymbolKind.FUNCTION, 0);
    }
    
    public void visit(ProgramNode node) 
    {
        // Global declarations
        for (DeclNode decl : node.decls) 
        {
            checkVoidDeclaration(decl);
            decl.accept(this);
        }
        
        // Functions
        for (FunctionNode func : node.funcs) 
        {
            func.accept(this);
        }
        
        node.main.accept(this);
    }
    
    // Check that variables/constants are not declared with void type
    private void checkVoidDeclaration(DeclNode decl) 
    {
        if (decl.type == CCLType.VOID) 
        {
            errors.add("Variables or constants cannot be declared using the void type (line " + decl.getLine() + ")");
        }
    }
    
    // Check that no identifier has been declared more than once in same scope
    public void visit(VarDeclNode node) 
    {
        if (!symbolTable.addSymbol(node.name, node.type, SymbolKind.VARIABLE, node.getLine())) 
        {
            errors.add("Duplicate variable: " + node.name + " (line " + node.getLine() + ")");
        }
    }
    
    // Same as above for const
    // Check type mismatch
    public void visit(ConstDeclNode node) 
    {
        if (!symbolTable.addSymbol(node.name, node.type, SymbolKind.CONSTANT, node.getLine())) 
        {
            errors.add("Duplicate constant: " + node.name + " (line " + node.getLine() + ")");
            return;
        }

        node.value.accept(this);

        if (node.value.getType() != node.type) 
        {
            errors.add("Constant type mismatch (line " + node.getLine() + ")");
        }
        
        // Mark constant as used and initialised
        SymbolEntry symbol = symbolTable.lookup(node.name);
        if (symbol != null) 
        {
            symbol.setUsed(true);
            symbol.setInitialised(true);
        }
    }
    
    // Checks:
    // Duplicate function names
    // Returns match function return type
    // Non-void functions have returns
    public void visit(FunctionNode node) 
    {
        currentFunction = node.name;
        
        if (!symbolTable.addSymbol(node.name, node.returnType, SymbolKind.FUNCTION, node.getLine())) 
        {
            errors.add("Duplicate function: " + node.name + " (line " + node.getLine() + ")");
        }
        
        symbolTable.enterScope();
        
        // Parameters
        for (ParameterNode param : node.params) 
        {
            checkVoidParameter(param);
            param.accept(this);
        }
        
        // Local declarations
        for (DeclNode decl : node.decls) 
        {
            checkVoidDeclaration(decl);
            decl.accept(this);
        }
        
        // Statements
        if (node.stmts != null) {
            node.stmts.accept(this);
        }
        
        // Return check
        if (node.returnExpr != null) 
        {
            node.returnExpr.accept(this);
            if (node.returnExpr.getType() != node.returnType) 
            {
                errors.add("Return type mismatch in function " + node.name + " (line " + node.getLine() + ")");
            }
        } 
        else if (node.returnType != CCLType.VOID) 
        {
            errors.add(node.name + " must return value (line " + node.getLine() + ")");
        }
        
        symbolTable.exitScope();
        currentFunction = null;
    }
    
    // Checks if parameters have void type
    private void checkVoidParameter(ParameterNode param) 
    {
        if (param.type == CCLType.VOID) 
        {
            errors.add("Cannot have void parameter: " + param.name + " (line " + param.getLine() + ")");
        }
    }
    
    // Checks if there are duplicates with parameters
    public void visit(ParameterNode node) 
    {
        if (!symbolTable.addSymbol(node.name, node.type, SymbolKind.PARAMETER, node.getLine())) 
        {
            errors.add("Duplicate parameter: " + node.name + " (line " + node.getLine() + ")");
        }
    }
    
    public void visit(MainNode node) 
    {
        symbolTable.enterScope();
        for (DeclNode decl : node.decls) 
        {
            checkVoidDeclaration(decl);
            decl.accept(this);
        }
        if (node.stmts != null) {
            node.stmts.accept(this);
        }
        symbolTable.exitScope();
    }
    
    // Checks statements for:
    // Undeclared variables/constants
    // Variables on left side is of correct type
    public void visit(AssignmentNode node) 
    {
        SymbolEntry symbol = symbolTable.lookup(node.varName);
        if (symbol == null) 
        {
            errors.add("Undeclared variable: " + node.varName + " (line " + node.getLine() + ")");
        } 
        else if (symbol.getKind() == SymbolKind.CONSTANT) 
        {
            errors.add("Cannot assign to constant: " + node.varName + " (line " + node.getLine() + ")");
        } 
        else 
        {
            symbol.setInitialised(true);
        }
        
        node.expr.accept(this);

        if (symbol != null && node.expr.getType() != symbol.getType()) 
        {
            errors.add("Assignment type mismatch for variable " + node.varName + " (line " + node.getLine() + ")");
        }
    }
    
    // Checks if every identifier is declared before it is used
    public void visit(IdentifierNode node) 
    {
        SymbolEntry symbol = symbolTable.lookup(node.name);
        if (symbol == null) 
        {
            errors.add("Undeclared identifier: " + node.name + " (line " + node.getLine() + ")");
        } 
        else 
        {
            symbol.setUsed(true);
            node.setType(symbol.getType());
        }
    }
    
    public void visit(NumberNode node) 
    {
        node.setType(CCLType.INTEGER);
    }
    
    public void visit(BooleanNode node) 
    {
        node.setType(CCLType.BOOLEAN);
    }
    
    public void visit(LogicalOrNode node) 
    {
        node.left.accept(this);
        node.right.accept(this);
        checkBooleanOperands(node);
        node.setType(CCLType.BOOLEAN);
    }
    
    public void visit(LogicalAndNode node) 
    {
        node.left.accept(this);
        node.right.accept(this);
        checkBooleanOperands(node);
        node.setType(CCLType.BOOLEAN);
    }
    
    // Checks if equality operands have same type
    public void visit(EqualityNode node) 
    {
        node.left.accept(this);
        node.right.accept(this);
        if (node.left.getType() != node.right.getType()) 
        {
            errors.add("Equality operands must have same type (line " + node.getLine() + ")");
        }
        node.setType(CCLType.BOOLEAN);
    }
    
    // Checks if variables for comparison are int
    public void visit(ComparisonNode node) 
    {
        node.left.accept(this);
        node.right.accept(this);
        if (node.left.getType() != CCLType.INTEGER || node.right.getType() != CCLType.INTEGER) 
        {
            errors.add("Comparison operands must work with integers (line " + node.getLine() + ")");
        }
        node.setType(CCLType.BOOLEAN);
    }
    
    // Checks if variables for aritmetic are int
    public void visit(AddSubNode node) 
    {
        node.left.accept(this);
        node.right.accept(this);
        if (node.left.getType() != CCLType.INTEGER || node.right.getType() != CCLType.INTEGER) 
        {
            errors.add("Arithmetic operands must be integers (line " + node.getLine() + ")");
        }
        node.setType(CCLType.INTEGER);
    }
    
    // Checks logical and arithmetic negation are bool and int respectively
    public void visit(NegationNode node) 
    {
        node.operand.accept(this);
        if ("~".equals(node.operator)) 
        {
            if (node.operand.getType() != CCLType.BOOLEAN) 
            {
                errors.add("Logical negation operand must be boolean (line " + node.getLine() + ")");
            }
            node.setType(CCLType.BOOLEAN);
        } 
        else if ("-".equals(node.operator)) 
        {
            if (node.operand.getType() != CCLType.INTEGER) 
            {
                errors.add("Arithmetic negation operand must be integer (line " + node.getLine() + ")");
            }
            node.setType(CCLType.INTEGER);
        }
    }

    // Checks for undeclared functions, aswell as variables called as functions
    // Function signature check
    public void visit(FunctionCallNode node) 
    {
        SymbolEntry symbol = symbolTable.lookup(node.funcName);
        if (symbol == null) 
        {
            errors.add("Undeclared function: " + node.funcName + " (line " + node.getLine() + ")");
        } 
        else if (symbol.getKind() != SymbolKind.FUNCTION) 
        {
            errors.add("Not a function: " + node.funcName + " (line " + node.getLine() + ")");
        } 
        else 
        {
            symbol.setUsed(true);
            node.setType(symbol.getType());
            
            if (!node.funcName.startsWith("_")) 
            {
                for (ExpressionNode arg : node.args) 
                {
                    arg.accept(this);
                }
            }
        }
        
        for (ExpressionNode arg : node.args) 
        {
            arg.accept(this);
        }
    }
    
    // Chekc if condtion to see if bool
    public void visit(IfStatementNode node) 
    {
        node.cond.accept(this);
        if (node.cond.getType() != CCLType.BOOLEAN) 
        {
            errors.add("If condition must be boolean (line " + node.getLine() + ")");
        }
        
        symbolTable.enterScope();
        if (node.thenBlock != null) 
        {
            node.thenBlock.accept(this);
        }
        symbolTable.exitScope();
        
        symbolTable.enterScope();
        if (node.elseBlock != null) 
        {
            node.elseBlock.accept(this);
        }
        symbolTable.exitScope();
    }

    // Check while condition to see if bool
    public void visit(WhileStatementNode node) 
    {
        node.cond.accept(this);
        if (node.cond.getType() != CCLType.BOOLEAN) 
        {
            errors.add("While condition must be boolean (line " + node.getLine() + ")");
        }
        
        symbolTable.enterScope();
        if (node.body != null) 
        {
            node.body.accept(this);
        }
        symbolTable.exitScope();
    }
    
    // Checks if "or" and "and" are bool 
    private void checkBooleanOperands(ExpressionNode node) 
    {
        if (node instanceof LogicalOrNode) 
        {
            LogicalOrNode lor = (LogicalOrNode) node;
            if (lor.left.getType() != CCLType.BOOLEAN || lor.right.getType() != CCLType.BOOLEAN) 
            {
                errors.add("Logical OR operands must be boolean (line " + node.getLine() + ")");
            }
        } 
        else if (node instanceof LogicalAndNode) 
        {
            LogicalAndNode land = (LogicalAndNode) node;
            if (land.left.getType() != CCLType.BOOLEAN || land.right.getType() != CCLType.BOOLEAN) 
            {
                errors.add("Logical AND operands must be boolean (line " + node.getLine() + ")");
            }
        }
    }
    
    public void visit(FunctionCallStatementNode node) 
    { 
        node.call.accept(this); 
    }

    public void visit(SkipStatementNode node) {}

    public void visit(StatementBlockNode node) 
    { 
        for (StatementNode stmt : node.stmts) stmt.accept(this); 
    }
    
    // Checks for unused variable/uncalled functions
    private void checkUnusedSymbols() 
    {
        for (SymbolEntry symbol : symbolTable.getAllSymbols()) 
        {
            if (!symbol.isUsed() && symbol.getKind() == SymbolKind.VARIABLE) 
            {
                warnings.add("Unused variable: " + symbol.getName() + " (line " + symbol.getLine() + ")");
            }
            if (!symbol.isUsed() && symbol.getKind() == SymbolKind.FUNCTION && !symbol.getName().startsWith("_") && !symbol.getName().equals("main")) 
            {
                warnings.add("Uncalled function: " + symbol.getName() + " (line " + symbol.getLine() + ")");
            }
        }
    }
    
    public List<String> getErrors() { return errors; }
    public List<String> getWarnings() { return warnings; }
    public SymbolTable getSymbolTable() { return symbolTable; }
}