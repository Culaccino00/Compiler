package cn.edu.hitsz.compiler.parser.table;

import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.symtab.SourceCodeType;

public class Symbol {
    Token token;
    NonTerminal nonTerminal;
    //扩展符号栈结构
    SourceCodeType type = null;

    private Symbol(Token token, NonTerminal nonTerminal){
        this.token = token;
        this.nonTerminal = nonTerminal;
    }

    public Symbol(Token token){
        this(token, null);
    }

    public Symbol(NonTerminal nonTerminal){
        this(null, nonTerminal);
    }

    public boolean isToken(){
        return this.token != null;
    }

    public void setType(SourceCodeType type){
        this.type = type;
    }

    public Token getToken(){
        return this.token;
    }

    public SourceCodeType getType(){
        return this.type;
    }

    public boolean isNonterminal(){
        return this.nonTerminal != null;
    }

}
