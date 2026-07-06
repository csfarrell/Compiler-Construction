import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import java.io.*;

// Pipeline for code
public class Main {
    public static void main(String[] args) {
        if (args.length == 0) 
        {
            System.out.println("Try: java Main x.ccl");
            return;
        }
        
        try 
        {
            String inputFile = args[0];
            if (!inputFile.endsWith(".ccl")) 
            {
                System.err.println("Input file with .ccl extension");
                return;
            }
            
            String irFile = inputFile.replace(".ccl", ".ir");
            
            System.out.println("First Begin Parsing " + inputFile);
            ProgramNode ast = parseFile(inputFile);
            
            System.out.println("Then Run SemanticAnalyser...");
            SemanticAnalyser analyser = new SemanticAnalyser();
            if (!analyser.analyse(ast)) 
            {
                System.err.println("Semantic error(s) found:");
                for (String error : analyser.getErrors()) 
                {
                    System.err.println("  " + error);
                }

                return;
            }
            
            for (String warning : analyser.getWarnings()) 
            {
                System.out.println("Warning!: " + warning);
            }
            
            System.out.println("Next Generate IR Code...");
            IRGenerator generator = new IRGenerator(analyser.getSymbolTable());
            generator.generateCode(ast);
            generator.writeToFile(irFile);
            
            System.out.println("Successfully Generated " + irFile);
            
        } 
        catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    // Similar to previous parse remade to be more to spec and cleaner
    // AST Tree built at end
    private static ProgramNode parseFile(String filename) throws Exception 
    {
        CharStream input = CharStreams.fromFileName(filename);
        CCLLexer lexer = new CCLLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        CCLParser parser = new CCLParser(tokens);
        
        lexer.removeErrorListeners();
        parser.removeErrorListeners();
        
        lexer.addErrorListener(new BaseErrorListener() 
        {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) 
            {
                throw new RuntimeException("Lexer error at " + line + ":" + charPositionInLine + " " + msg);
            }
        });
        
        parser.addErrorListener(new BaseErrorListener() 
        {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) 
            {
                throw new RuntimeException("Syntax error at " + line + ":" + charPositionInLine + " - " + msg);
            }
        });
        
        ParseTree tree = parser.program();

        ASTBuilder builder = new ASTBuilder();
        ProgramNode ast = (ProgramNode) builder.visit(tree);
        
        return ast;
    }
}