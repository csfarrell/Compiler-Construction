import java.util.*;

// Scope helper for symbol table
// Checks if global or local
// Finds symbol in current scope (returns entry if not found)
// Grab all symbols in scope are return values
public class Scope 
{
    private Map<String, SymbolEntry> symbols = new HashMap<>();
    private boolean isGlobal;
    
    public Scope(boolean isGlobal) 
    {
        this.isGlobal = isGlobal;
    }
    
    public boolean addSymbol(SymbolEntry symbol) 
    {
        if (symbols.containsKey(symbol.getName())) 
        { 
                return false;
        }
        symbols.put(symbol.getName(), symbol);
        return true;
    }
    
    public SymbolEntry lookup(String name) 
    {
        return symbols.get(name);
    }
    
    public Collection<SymbolEntry> getSymbols() 
    {
        return symbols.values();
    }
    
    public boolean isGlobal() 
    { 
        return isGlobal; 
    }
}