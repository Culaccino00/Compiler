package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.lexer.TokenKind;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.parser.table.Symbol;
import cn.edu.hitsz.compiler.symtab.SourceCodeType;
import cn.edu.hitsz.compiler.symtab.SymbolTable;

import java.util.Stack;

// TODO: 实验三: 实现语义分析
public class SemanticAnalyzer implements ActionObserver {

    private SymbolTable symbolTable;
    //语义分析栈
    private final Stack<Symbol> semanticStack = new Stack<>();


    @Override
    public void whenAccept(Status currentStatus) {
        // TODO: 该过程在遇到 Accept 时要采取的代码动作
        // nothing to do here
    }

    @Override
    public void whenReduce(Status currentStatus, Production production) {
        // TODO: 该过程在遇到 reduce production 时要采取的代码动作
        switch (production.index()) {
            case 4 -> { // S -> D id
                //弹出栈顶的两个符号
                Symbol id = semanticStack.pop();
                Symbol D = semanticStack.pop();
                //将id的类型设置为D的类型
                this.symbolTable.get(id.getToken().getText()).setType(D.getType());
                Symbol S = new Symbol(production.head());
                S.setType(null);
                //将S压入栈中
                semanticStack.push(S);
            }
            case 5 -> { // D -> int
                //弹出栈顶的符号
                semanticStack.pop();
                //将D的类型设置为int
                Symbol D = new Symbol(production.head());
                D.setType(SourceCodeType.Int);
                //将D压入栈中
                semanticStack.push(D);
            }
            default -> { //
                //弹出与产生式右部相同长度的符号
                for(int i = 0; i < production.body().size(); i++){
                    semanticStack.pop();
                }
                //将产生式左部符号压入栈中
                semanticStack.push(new Symbol(production.head()));
            }
        }
    }

    @Override
    public void whenShift(Status currentStatus, Token currentToken) {
        // TODO: 该过程在遇到 shift 时要采取的代码动作
        Symbol symbol = new Symbol(currentToken);
        if(currentToken.getKind() == TokenKind.fromString("int")){
            symbol.setType(SourceCodeType.Int);
        }else{
            symbol.setType(null);
        }
        semanticStack.push(symbol);
    }

    @Override
    public void setSymbolTable(SymbolTable table) {
        // TODO: 设计你可能需要的符号表存储结构
        // 如果需要使用符号表的话, 可以将它或者它的一部分信息存起来, 比如使用一个成员变量存储
        this.symbolTable = table;
    }
}

