// Type from grammar
public enum CCLType 
{
    INTEGER, BOOLEAN, VOID;
}

// Convert string to type enum values
class CCLTypeConverter 
{
    public static CCLType fromString(String type) 
    {
        switch (type.toUpperCase()) 
        {
            case "INTEGER": return CCLType.INTEGER;
            case "BOOLEAN": return CCLType.BOOLEAN;
            case "VOID": return CCLType.VOID;
            default: throw new IllegalArgumentException("Unknown type: " + type);
        }
    }
}