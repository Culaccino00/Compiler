package cn.edu.hitsz.compiler.lexer;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

/**
 * TODO: 实验一: 实现词法分析
 * <br>
 * 你可能需要参考的框架代码如下:
 *
 * @see Token 词法单元的实现
 * @see TokenKind 词法单元类型的实现
 */
public class LexicalAnalyzer {
    private final SymbolTable symbolTable;
    public String sourceString ="";
    public List<Token> tokens = new ArrayList<>();

    public LexicalAnalyzer(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }


    /**
     * 从给予的路径中读取并加载文件内容
     *
     * @param path 路径
     */
    public void loadFile(String path) {
        List<String> lines = FileUtils.readLines(path);
        for(String line : lines){
            sourceString += line;
        }
        System.out.println(sourceString);
    }

    /**
     * 执行词法分析, 准备好用于返回的 token 列表 <br>
     * 需要维护实验一所需的符号表条目, 而得在语法分析中才能确定的符号表条目的成员可以先设置为 null
     */
    public void run() {
        // 使用状态机
        // 词法分析过程中需要维护符号表
        // 词法分析过程中需要维护 Token 列表
        int i = 0;
        enum State{
            IDLE,
            OPERATOR,
            DELIMITER,
            NUMBER,
            LETTER
        }
        State currentState = State.IDLE;
        String currentString = "";
        String currentNumber = "";
        String currentOperator = "";
        while(i < sourceString.length()){
            char c = sourceString.charAt(i);
            switch(currentState) {
                case IDLE:
                    if(c == ' ' || c == '\t' || c == '\r' ||c == '\n'){
                        currentState = State.IDLE;
                        i ++;
                    }else if (Character.isLetter(c)) {
                        currentState = State.LETTER;
                        currentString += c;
                        i++;
                    } else if (Character.isDigit(c) && c != '0') {
                        currentState = State.NUMBER;
                        currentNumber += c;
                        i++;
                    } else if (c == '+' || c == '-' || c == '*' || c == '/' || c =='=') {
                        currentState = State.OPERATOR;
                    } else if (c == '(' || c == ')' || c == ',' || c == ';') {
                        currentState = State.DELIMITER;
                    }else{
                        throw new NotImplementedException();
                    }
                    break;
                case LETTER:
                    if(Character.isLetter(c) || Character.isDigit(c)){
                        currentState = State.LETTER;
                        currentString += c;
                        i++;
                    }else{
                        if(TokenKind.isAllowed(currentString)) {
                            tokens.add(Token.simple(currentString));
                        }else{
                            tokens.add(Token.normal("id",currentString));
                            if (!symbolTable.has(currentString)) {
                                symbolTable.add(currentString);
                            }
                        }
                        currentString = "";
                        currentState = State.IDLE;
                    }
                    break;
                case NUMBER:
                    if(Character.isDigit(c)){
                        currentState = State.NUMBER;
                        currentNumber += c;
                        i++;
                    }else{
                        tokens.add(Token.normal("IntConst",currentNumber));
                        currentNumber = "";
                        currentState = State.IDLE;
                    }
                    break;
                case OPERATOR:
                    if(c == '+' || c == '-' || c == '/' || c =='='){
                        tokens.add(Token.simple(String.valueOf(c)));
                        i++;
                        currentState = State.IDLE;
                    }else if(c == '*'){
                        currentOperator += c;
                        i++;
                        currentState = State.OPERATOR;
                    }else{
                        if(currentOperator.equals("*")){
                            tokens.add(Token.simple("*"));
                        }else if(currentOperator.equals("**")){
                            tokens.add(Token.simple("**"));
                        }
                        currentOperator = "";
                        currentState = State.IDLE;
                    }
                    break;
                case DELIMITER:
                    if(c == '(' || c == ')' || c == ',' ){
                        tokens.add(Token.simple(String.valueOf(c)));
                        i++;
                    }else if(c == ';'){
                        tokens.add(Token.simple("Semicolon"));
                        i++;
                    }else{
                        throw new NotImplementedException();
                    }
                    currentState = State.IDLE;
                    break;
                default:
                    throw new NotImplementedException();
            }
        }
        tokens.add(Token.eof());
    }

    /**
     * 获得词法分析的结果, 保证在调用了 run 方法之后调用
     *
     * @return Token 列表
     */
    public Iterable<Token> getTokens() {
        // 词法分析过程可以使用 Stream 或 Iterator 实现按需分析
        // 亦可以直接分析完整个文件
        // 总之实现过程能转化为一列表即可
        return tokens;
    }

    public void dumpTokens(String path) {
        FileUtils.writeLines(
            path,
            StreamSupport.stream(getTokens().spliterator(), false).map(Token::toString).toList()
        );
    }


}
