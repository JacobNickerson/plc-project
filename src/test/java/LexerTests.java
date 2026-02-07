package plc.project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class LexerTests {

    @ParameterizedTest
    @MethodSource
    void testIdentifier(String test, String input, boolean success) {
        test(input, Token.Type.IDENTIFIER, success);
    }

    private static Stream<Arguments> testIdentifier() {
        return Stream.of(
                Arguments.of("Alphabetic", "getName", true),
                Arguments.of("Alphanumeric", "thelegend27", true),
                Arguments.of("Alphanumeric with hyphen/underscore", "the_legend-27", true),
                Arguments.of("Leading Hyphen", "-five", false),
                Arguments.of("Leading Digit", "1fish2fish3fishbluefish", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testInteger(String test, String input, boolean success) {
        test(input, Token.Type.INTEGER, success);
    }

    private static Stream<Arguments> testInteger() {
        return Stream.of(
                Arguments.of("Single Digit", "1", true),
                Arguments.of("Multiple Digits", "12345", true),
                Arguments.of("Negative", "-1", true),
                Arguments.of("Explicit Positive", "+123456", true),

                Arguments.of("Leading Zero", "01", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testDecimal(String test, String input, boolean success) {
        test(input, Token.Type.DECIMAL, success);
    }

    private static Stream<Arguments> testDecimal() {
        return Stream.of(
                Arguments.of("Multiple Digit Float", "123.456", true),
                Arguments.of("Explicit Positive Integer", "+1.0", true),
                Arguments.of("Positive Float", "2.5", true),
                Arguments.of("Negative Float", "-2.5", true),
                Arguments.of("Positive Multiple Digits", "12.57", true),
                Arguments.of("Negative Multiple Digits", "-12.57", true),
                Arguments.of("Positive Leading Zeros in Mantissa", "12.07", true),
                Arguments.of("Negative Leading Zeros in Mantissa", "-12.07", true),
                Arguments.of("Positive Leading Zero", "0.07", true),
                Arguments.of("Negative Leading Zero", "-0.07", true),

                Arguments.of("Float Without Mantissa", "10.", false),
                Arguments.of("No Integer", ".5", false),
                Arguments.of("Leading Zero", "01.5", false),
                Arguments.of("Characters in Integer", "10a1.5", false),
                Arguments.of("Characters in Mantissa", "101.5a7", false),
                Arguments.of("A String for Some Reason", "Hello world!", false),
                Arguments.of("Two Decimals", "10.70.10", false),
                Arguments.of("Symbols in Integer", "1&70.10", false),
                Arguments.of("Symbols in Mantissa", "170.1^0", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testCharacter(String test, String input, boolean success) {
        test(input, Token.Type.CHARACTER, success);
    }

    private static Stream<Arguments> testCharacter() {
        return Stream.of(
                Arguments.of("Alphabetic", "'c'", true),
                Arguments.of("Newline Escape", "'\\n'", true),
                Arguments.of("Tab Escape", "'\\t'", true),
                Arguments.of("b Escape", "'\\b'", true),
                Arguments.of("Return Escape", "'\\r'", true),
                Arguments.of("Double Quote Escape", "'\\\"'", true),
                Arguments.of("Single Quote Escape", "'\\''", true),
                Arguments.of("Numeric", "'5'", true),
                Arguments.of("Symbol", "'&'", true),

                Arguments.of("Empty", "''", false),
                Arguments.of("Single quote", "'''", false),
                Arguments.of("Multiple", "'abc'", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testString(String test, String input, boolean success) {
        test(input, Token.Type.STRING, success);
    }

    private static Stream<Arguments> testString() {
        return Stream.of(
                Arguments.of("Empty", "\"\"", true),
                Arguments.of("Alpha",   "\"Hello\"", true),
                Arguments.of("Numeric", "\"123456\"", true),
                Arguments.of("Symbols", "\"!@#$%^&*()\"", true),
                Arguments.of("Alphanumeric", "\"Hello, world997!\"", true),
                Arguments.of("Valid Escape Characters", "\"123 \\\\ \\b \\n \\r \\t \\' \\\" abc\"", true),

                Arguments.of("Unterminated", "\"Hello, world!", false),
                Arguments.of("Not Opened", "Hello, world!\"", false),
                Arguments.of("No Quotes", "Hello, world!", false),
                Arguments.of("Extra Opening Quote", "\"\"Hello, world!\"", false),
                Arguments.of("Extra Closing Quote", "\"Hello, world!\"\"", false),
                Arguments.of("Characters before quote", "a\"Hello world!\"", false),
                Arguments.of("Characters after quote", "\"Hello world!\"a", false),
                Arguments.of("Invalid Escape", "\"hello\\world\"", false),
                Arguments.of("Multi-line string", "\"hello\nworld\"", false)

        );
    }

    @ParameterizedTest
    @MethodSource
    void testOperator(String test, String input, boolean success) {
        //this test requires our lex() method, since that's where whitespace is handled.
        test(input, Arrays.asList(new Token(Token.Type.OPERATOR, input, 0)), success);
    }

    private static Stream<Arguments> testOperator() {
        return Stream.of(
                Arguments.of("Character", "(", true),
                Arguments.of("Not Equal", "!=", true),
                Arguments.of("Less Than or Equal", "<=", true),
                Arguments.of("Greater Than or Equal", ">=", true),
                Arguments.of("Equal", "==", true),
                Arguments.of("Bang", "!", true),
                Arguments.of("Less Than", "<", true),
                Arguments.of("Greater Than", ">", true),
                Arguments.of("Single Equal", "=", true),

                Arguments.of("Space", " ", false),
                Arguments.of("Tab", "\t", false),
                Arguments.of("Newline", "\n", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testExamples(String test, String input, List<Token> expected) {
        test(input, expected, true);
    }

    private static Stream<Arguments> testExamples() {
        return Stream.of(
                Arguments.of("Example 1", "LET x = 5;", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "LET", 0),
                        new Token(Token.Type.IDENTIFIER, "x", 4),
                        new Token(Token.Type.OPERATOR, "=", 6),
                        new Token(Token.Type.INTEGER, "5", 8),
                        new Token(Token.Type.OPERATOR, ";", 9)
                )),
                Arguments.of("Example 2", "print(\"Hello, World!\");", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "print", 0),
                        new Token(Token.Type.OPERATOR, "(", 5),
                        new Token(Token.Type.STRING, "\"Hello, World!\"", 6),
                        new Token(Token.Type.OPERATOR, ")", 21),
                        new Token(Token.Type.OPERATOR, ";", 22)
                ))
        );
    }

    @Test
    void testException() {
        ParseException exception = Assertions.assertThrows(ParseException.class,
                () -> new Lexer("\"unterminated").lex());
        Assertions.assertEquals(13, exception.getIndex());
    }

    /**
     * Tests that lexing the input through {@link Lexer#lexToken()} produces a
     * single token with the expected type and literal matching the input.
     */
    private static void test(String input, Token.Type expected, boolean success) {
        try {
            if (success) {
                Assertions.assertEquals(new Token(expected, input, 0), new Lexer(input).lexToken());
            } else {
                Assertions.assertNotEquals(new Token(expected, input, 0), new Lexer(input).lexToken());
            }
        } catch (ParseException e) {
            Assertions.assertFalse(success, e.getMessage());
        }
    }

    /**
     * Tests that lexing the input through {@link Lexer#lex()} matches the
     * expected token list.
     */
    private static void test(String input, List<Token> expected, boolean success) {
        try {
            if (success) {
                Assertions.assertEquals(expected, new Lexer(input).lex());
            } else {
                Assertions.assertNotEquals(expected, new Lexer(input).lex());
            }
        } catch (ParseException e) {
            Assertions.assertFalse(success, e.getMessage());
        }
    }

}
