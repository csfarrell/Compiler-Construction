import java.util.*;

// Entries in symbol table getters/setters
public class SymbolEntry 
{
    private String name;
    private CCLType type;
    private SymbolKind kind;
    private int line;
    private boolean used = false;
    private boolean initialised = false;
    private boolean isGlobal = false;
    
    public SymbolEntry(String name, CCLType type, SymbolKind kind, int line, boolean isGlobal) 
    {
        this.name = name;
        this.type = type;
        this.kind = kind;
        this.line = line;
        this.isGlobal = isGlobal;
    }
    
    public String getName() { return name; }
    public CCLType getType() { return type; }
    public SymbolKind getKind() { return kind; }
    public int getLine() { return line; }
    public boolean isUsed() { return used; }
    public void setUsed(boolean used) { this.used = used; }
    public boolean isInitialised() { return initialised; }
    public void setInitialised(boolean initialised) { this.initialised = initialised; }
    public boolean isGlobal() { return isGlobal; }
}