package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.ir.IRImmediate;
import cn.edu.hitsz.compiler.ir.IRValue;
import cn.edu.hitsz.compiler.ir.IRVariable;
import cn.edu.hitsz.compiler.ir.Instruction;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FilePathConfig;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

// TODO: 实验三: 实现 IR 生成

/**
 *
 */
public class IRGenerator implements ActionObserver {

    private SymbolTable symbolTable;
    // value栈
    private final Stack<IRValue> valueStack = new Stack<>();
    // IR指令
    private final List<Instruction> IRList = new ArrayList<>();


    @Override
    public void whenShift(Status currentStatus, Token currentToken) {
        // TODO
        String isNumber = "^[0-9]+$";
        if (currentToken.getText().matches(isNumber)) {
            // 如果是数字，将其转换为立即数压入栈中
            valueStack.push(IRImmediate.of(Integer.parseInt(currentToken.getText())));
        } else {
            // 如果是标识符，将其转换为 IR 变量压入栈中
            valueStack.push(IRVariable.named(currentToken.getText()));
        }
    }

    @Override
    public void whenReduce(Status currentStatus, Production production) {
        // TODO
        if(FilePathConfig.isExtra){
            switch (production.index()) {
                case 6 -> {//S->id = E
                    IRValue E = valueStack.pop();
                    valueStack.pop();// =
                    IRValue id = valueStack.pop();
                    IRList.add(Instruction.createMov((IRVariable) id, E));
                    valueStack.push(null);
                }
                case 7 -> {//S->return E
                    IRValue E = valueStack.pop();
                    valueStack.pop();//return
                    IRList.add(Instruction.createRet(E));
                    valueStack.push(null);
                }
                case 8 -> {//E->E + A
                    IRValue A = valueStack.pop();
                    valueStack.pop();//+
                    IRValue E = valueStack.pop();
                    IRVariable temp = IRVariable.temp();
                    IRList.add(Instruction.createAdd(temp, E, A));
                    valueStack.push(temp);
                }
                case 9 -> {//E->E - A
                    IRValue A = valueStack.pop();
                    valueStack.pop();//-
                    IRValue E = valueStack.pop();
                    IRVariable temp = IRVariable.temp();
                    IRList.add(Instruction.createSub(temp, E, A));
                    valueStack.push(temp);
                }
                case 10, 12, 14, 16, 17 -> {//E->A, A->B, B->C, C->id, C->IntConst
                    valueStack.push(valueStack.pop());
                }
                case 11 -> {//A->A * B
                    IRValue B = valueStack.pop();
                    valueStack.pop();//*
                    IRValue A = valueStack.pop();
                    IRVariable temp = IRVariable.temp();
                    IRList.add(Instruction.createMul(temp, A, B));
                    valueStack.push(temp);
                }
                case 13 -> {//B->B**C
                    IRValue C = valueStack.pop();
                    valueStack.pop();//**
                    IRValue B = valueStack.pop();
                    IRVariable temp = IRVariable.temp();
                    IRList.add(Instruction.createPow(temp, B, C));
                    valueStack.push(temp);
                }
                case 15 -> {//C->(E)
                    valueStack.pop();//)
                    IRValue E = valueStack.pop();
                    valueStack.pop();//(
                    valueStack.push(E);
                }
                default -> {
                    for (int i = 0; i < production.body().size(); i++) {
                        valueStack.pop();
                    }
                    valueStack.push(null);
                }
            }
        }else {
            switch (production.index()) {
                case 6 -> {//S->id = E
                    IRValue E = valueStack.pop();
                    valueStack.pop();// =
                    IRValue id = valueStack.pop();
                    IRList.add(Instruction.createMov((IRVariable) id, E));
                    valueStack.push(null);
                }
                case 7 -> {//S->return E
                    IRValue E = valueStack.pop();
                    valueStack.pop();//return
                    IRList.add(Instruction.createRet(E));
                    valueStack.push(null);
                }
                case 8 -> {//E->E + A
                    IRValue A = valueStack.pop();
                    valueStack.pop();//+
                    IRValue E = valueStack.pop();
                    IRVariable temp = IRVariable.temp();
                    IRList.add(Instruction.createAdd(temp, E, A));
                    valueStack.push(temp);
                }
                case 9 -> {//E->E - A
                    IRValue A = valueStack.pop();
                    valueStack.pop();//-
                    IRValue E = valueStack.pop();
                    IRVariable temp = IRVariable.temp();
                    IRList.add(Instruction.createSub(temp, E, A));
                    valueStack.push(temp);
                }
                case 10, 12, 14, 15 -> {//E->A, A->B, B->id, B->IntConst
                    valueStack.push(valueStack.pop());
                }
                case 11 -> {//A->A * B
                    IRValue B = valueStack.pop();
                    valueStack.pop();//*
                    IRValue A = valueStack.pop();
                    IRVariable temp = IRVariable.temp();
                    IRList.add(Instruction.createMul(temp, A, B));
                    valueStack.push(temp);
                }
                case 13 -> {//B->(E)
                    valueStack.pop();//)
                    IRValue E = valueStack.pop();
                    valueStack.pop();//(
                    valueStack.push(E);
                }
                default -> {
                    for (int i = 0; i < production.body().size(); i++) {
                        valueStack.pop();
                    }
                    valueStack.push(null);
                }
            }
        }
    }


    @Override
    public void whenAccept(Status currentStatus) {
        // TODO
        // nothing to do here
    }

    @Override
    public void setSymbolTable(SymbolTable table) {
        // TODO
        this.symbolTable = table;
    }

    public List<Instruction> getIR() {
        // TODO
        return IRList;
    }

    public void dumpIR(String path) {
        FileUtils.writeLines(path, getIR().stream().map(Instruction::toString).toList());
    }
}

