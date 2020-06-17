package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Bad practice!!!
import static com.craftinginterpreters.lox.TokenType.*;

public class Scanner {
  private final String source;
  private final List<Token> tokens = new ArrayList<>();
  private int start = 0;
  private int current = 0;
  private int line = 1;
  
  private static final Map<String, TokenType> keywords;

  static {
    keywords = new HashMap<>();
    keywords.put("and",    AND);
    keywords.put("class",  CLASS);
    keywords.put("else",   ELSE);
    keywords.put("false",  FALSE);
    keywords.put("for",    FOR);
    keywords.put("fun",    FUN);
    keywords.put("if",     IF);
    keywords.put("nil",    NIL);
    keywords.put("or",     OR);
    keywords.put("print",  PRINT);
    keywords.put("return", RETURN);
    keywords.put("super",  SUPER);
    keywords.put("this",   THIS);
    keywords.put("true",   TRUE);
    keywords.put("var",    VAR);
    keywords.put("while",  WHILE);
  }

  Scanner(String source) {
    this.source = source;
  }

  List<Token> scanTokens() {
    while (!isAtEnd()) {
      // Beginning of new lexeme
      start = current;
      scanToken();
    }

    tokens.add(new Token(EOF, "", null, line));
    return tokens;
  }

  private void scanToken() {
    char c = advance();

    switch (c) {
      case '(': addToken(LEFT_PAREN); break;
      case ')': addToken(RIGHT_PAREN); break;
      case '{': addToken(LEFT_BRACE); break;
      case '}': addToken(RIGHT_BRACE); break;
      case ',': addToken(COMMA); break;
      case '.': addToken(DOT); break;
      case '-': addToken(MINUS); break;
      case '+': addToken(PLUS); break;
      case ';': addToken(SEMICOLON); break;
      case '*': addToken(STAR); break;
      case '!': addToken(match('=') ? BANG_EQUAL : BANG); break;
      case '=': addToken(match('=') ? EQUAL_EQUAL : EQUAL); break;
      case '<': addToken(match('=') ? LESS_EQUAL : LESS); break;
      case '>': addToken(match('=') ? GREATER_EQUAL : GREATER); break;
      case '/':
        if (match('/')) {
          // Comment goes until end of the line
          while (peek() != '\n' && !isAtEnd()) advance();
        } else if (match('*')) {
          // Comment goes until we see */ 
          // Nesting not supported since we need to figure out how many
          // open block comments we have, which requires a some structure (stack)
          // to store them
          while (!(peek() == '*' && peekNext() == '/') && !isAtEnd()) {
            if (peek() == '\n') line++;

            advance();
          }

          // Advance twice so we don't accidentally add */ tokens
          advance(); advance();
        } else {
          addToken(SLASH);
        }
        break;

      // ignore whitespace
      case ' ':
      case '\r':
      case '\t':
        break;
      // increment line
      case '\n':
        line++;
        break;

      case '"': string(); break;

      default:
        if (isDigit(c)) {
          number();
        } else if (isAlpha(c)) {
          identifier();
        } else {
          Lox.error(line, "Unexpected character.");
        }
        break;
    }
  }

  private void identifier() {
    while (isAlphaNumeric(peek())) advance();

    // Check if it matches a reserved word
    String text = source.substring(start, current);

    TokenType type = keywords.get(text);
    if (type == null) type = IDENTIFIER;
    addToken(type);
  }

  private void number() {
    while (isDigit(peek())) advance();

    // Look for fractional part or DOT
    if (peek() == '.' && isDigit(peekNext())) {
      // Consume DOT
      advance();

      while (isDigit(peek())) advance();
    }

    addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
  }

  private void string() {
    while (peek() != '"' && !isAtEnd()) {
      if (peek() == '\n') line++;
      advance();
    }

    // Unterminated string
    if (isAtEnd()) {
      Lox.error(line, "Unterminated string.");
      return;
    }

    // Closing "
    advance();

    // Trim quotes out of literal
    String value = source.substring(start + 1, current - 1);
    addToken(STRING, value);
  }

  private char peek() {
    if (isAtEnd()) return '\0';
    return source.charAt(current);
  }

  // Two character look ahead
  private char peekNext() {
    if (current + 1 >= source.length()) return '\0';
    return source.charAt(current + 1);
  }

  private boolean isAlpha(char c) {
    return (c >= 'a' && c <= 'z') ||
           (c >= 'A' && c <= 'Z') ||
           c == '_';
  }

  private boolean isAlphaNumeric(char c) {
    return isAlpha(c) || isDigit(c);
  }

  private boolean isDigit(char c) {
    return c >= '0' && c <= '9';
  }

  // Lookahead and consume character
  private boolean match(char expected) {
    if (isAtEnd()) return false;
    if (source.charAt(current) != expected ) return false;

    current++;
    return true;
  }

  private boolean isAtEnd() {
    return current >= source.length();
  }

  private char advance() {
    current++;
    return source.charAt(current - 1);
  }

  private void addToken(TokenType type) {
    addToken(type, null);
  }

  private void addToken(TokenType type, Object literal) {
    String text = source.substring(start, current);
    tokens.add(new Token(type, text, literal, line));
  }
}