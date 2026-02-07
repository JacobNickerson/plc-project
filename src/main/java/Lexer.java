package plc.project;

import java.util.ArrayList;
import java.util.List;

/**
 * The lexer works through three main functions:
 *
 *  - {@link #lex()}, which repeatedly calls lexToken() and skips whitespace
 *  - {@link #lexToken()}, which lexes the next token
 *  - {@link CharStream}, which manages the state of the lexer and literals
 *
 * If the lexer fails to parse something (such as an unterminated string) you
 * should throw a {@link ParseException} with an index at the invalid character.
 *
 * The {@link #peek(String...)} and {@link #match(String...)} functions are
 * helpers you need to use, they will make the implementation easier.
 */
public final class Lexer {

    private final CharStream chars;

    public Lexer(String input) {
        chars = new CharStream(input);
    }

    /**
     * Repeatedly lexes the input using {@link #lexToken()}, also skipping over
     * whitespace where appropriate.
     */
    public List<Token> lex() {
        List<Token> tokens = new ArrayList<>();

        while (chars.has(0)) {
            // skip whitespace
            if (peek("[ \b\n\r\t]")) {
                chars.advance();
                chars.skip();
            } else {
                tokens.add(lexToken());
            }
        }

        return tokens;
    }

    /**
     * This method determines the type of the next token, delegating to the
     * appropriate lex method. As such, it is best for this method to not change
     * the state of the char stream (thus, use peek not match).
     *
     * The next character should start a valid token since whitespace is handled
     * by {@link #lex()}
     */
    public Token lexToken() {

        // Identifier:
        // * Can start with letters and underscores
        // * Cannot start with a digit or hyphen
        if (peek("[A-Za-z_]")) {
            return lexIdentifier();
        }

        // Number (Integer/Decimal):
        // * Can start with an optional + or - sign followed by digits
        //      * If there were no digits after the sign, it would be an operator
        // * Can start with only digits
        else if (peek("[\\+\\-]", "[0-9]") || peek("[0-9]")) {
            return lexNumber();
        }

        // Character:
        // Must be enclosed in (and thus start with) single quotes
        else if (peek("'")) {
            return lexCharacter();
        }

        // String:
        // Must be enclosed in (and thus start with) double quotes
        else if (peek("\"")) {
            return lexString();
        }

        // Operator:
        // Match any other character, except for whitespace
        // <=, >=, !=, == are special cases and are grouped together
        else if (peek("\\S+")) {
            return lexOperator();
        }
        else {
            throw new ParseException("Cannot determine token type", chars.index);
        }
    }

    public Token lexIdentifier() {
        while (match("[A-Za-z0-9_-]")){
            continue;
        }
        return chars.emit(Token.Type.IDENTIFIER);
    }

    public Token lexNumber() {
        match("[\\+\\-]");

        // handle leading zero (should only apply to decimal numbers)
        if (match("0")) {

            // if there are more digits after leading zero, it's an invalid decimal
            if (peek("\\.", "[0-9]")) {
                match("\\.");
                while (match("[0-9]")) {
                    continue;
                }
                return chars.emit(Token.Type.DECIMAL);
            }

            // return simple integer zero
            return chars.emit(Token.Type.INTEGER);
        }

        // handle non-leading zero
        while (match("[0-9]")) {
            continue;
        }

        // check for decimal
        if (peek("\\.", "[0-9]")) {
            match("\\.");
            while (match("[0-9]")) {
                continue;
            }
            return chars.emit(Token.Type.DECIMAL);
        }

        return chars.emit(Token.Type.INTEGER);
    }

    public Token lexCharacter() {
        match("'");
        if (peek("\\\\")) {
            lexEscape();
        } else if (!match("[^']")) {
            throw new ParseException("Empty character", chars.index);
        }
        if (!match("'")) {
            throw new ParseException("Unterminated character", chars.index);
        }
        return chars.emit(Token.Type.CHARACTER);
    }

    public Token lexString() {
        match("\"");
        while (!peek("[\"]")) {
            if (peek("\\\\")) {
                lexEscape();
            } else if (match("\n")){
                throw new ParseException("String literal cannot span multiple lines", chars.index);
            } else if (!match(".")) {
                throw new ParseException("Unterminated string literal", chars.index);
            }
        }
        match("\"");
        return chars.emit(Token.Type.STRING);
    }

    public void lexEscape() {
        if (!match("\\\\", "[bnrt'\"\\\\]")) {
            throw new ParseException("Invalid escape sequence", chars.index);
        }
    }

    public Token lexOperator() {
        if (peek("[!=<>]", "=") ) {
            match("[!=<>]", "=");
        } else if (!match("\\S+")) {
            throw new ParseException("White space in char", chars.index);
        }
        return chars.emit(Token.Type.OPERATOR);
    }

    /**
     * Returns true if the next sequence of characters match the given patterns,
     * which should be a regex. For example, {@code peek("a", "b", "c")} would
     * return true if the next characters are {@code 'a', 'b', 'c'}.
     */
    public boolean peek(String... patterns) {
        for (int i = 0; i < patterns.length; i++) {
            if (!chars.has(i) || !String.valueOf(chars.get(i)).matches(patterns[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true in the same way as {@link #peek(String...)}, but also
     * advances the character stream past all matched characters if peek returns
     * true. Hint - it's easiest to have this method simply call peek.
     */
    public boolean match(String... patterns) {
        boolean peek = peek(patterns);

        if (peek) {
            for (int i = 0; i < patterns.length; i++) {
                chars.advance();
            }
        }
        return peek;
    }

    /**
     * A helper class maintaining the input string, current index of the char
     * stream, and the current length of the token being matched.
     *
     * You should rely on peek/match for state management in nearly all cases.
     * The only field you need to access is {@link #index} for any {@link
     * ParseException} which is thrown.
     */
    public static final class CharStream {

        private final String input;
        private int index = 0;
        private int length = 0;

        public CharStream(String input) {
            this.input = input;
        }

        public boolean has(int offset) {
            return index + offset < input.length();
        }

        public char get(int offset) {
            return input.charAt(index + offset);
        }

        public void advance() {
            index++;
            length++;
        }

        public void skip() {
            length = 0;
        }

        public Token emit(Token.Type type) {
            int start = index - length;
            skip();
            return new Token(type, input.substring(start, index), start);
        }

    }

}
