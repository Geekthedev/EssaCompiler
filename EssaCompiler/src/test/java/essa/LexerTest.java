package essa;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class LexerTest {
    
    @Test
    public void testTokenizeSimpleVariable() {
        Lexer lexer = new TypeScriptLexer();
        List<Token> tokens = lexer.tokenize("let x = 5;");
        
        assertEquals(5, tokens.size());
        assertEquals("let", tokens.get(0).getValue());
        assertEquals("x", tokens.get(1).getValue());
        assertEquals("=", tokens.get(2).getValue());
    }
}