package cn.edu.hitsz.compiler.asm;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.ir.*;
import cn.edu.hitsz.compiler.utils.FileUtils;

import javax.swing.text.html.StyleSheet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * TODO: 实验四: 实现汇编生成
 * <br>
 * 在编译器的整体框架中, 代码生成可以称作后端, 而前面的所有工作都可称为前端.
 * <br>
 * 在前端完成的所有工作中, 都是与目标平台无关的, 而后端的工作为将前端生成的目标平台无关信息
 * 根据目标平台生成汇编代码. 前后端的分离有利于实现编译器面向不同平台生成汇编代码. 由于前后
 * 端分离的原因, 有可能前端生成的中间代码并不符合目标平台的汇编代码特点. 具体到本项目你可以
 * 尝试加入一个方法将中间代码调整为更接近 risc-v 汇编的形式, 这样会有利于汇编代码的生成.
 * <br>
 * 为保证实现上的自由, 框架中并未对后端提供基建, 在具体实现时可自行设计相关数据结构.
 *
 * @see AssemblyGenerator#run() 代码生成与寄存器分配
 */
public class AssemblyGenerator {

    //预处理后的中间代码列表
    private final List<Instruction> newInstructions = new ArrayList<>();

    //变量与寄存器双向Map
    BMap<IRValue, Register> registerMap = new BMap<>();

    //生成的汇编指令列表
    private final List<AsmCode> asmCodes = new ArrayList<>();


    enum Register {
        //寄存器枚举
        t0, t1, t2, t3, t4, t5, t6
    }

    /**
     * 加载前端提供的中间代码
     * <br>
     * 视具体实现而定, 在加载中或加载后会生成一些在代码生成中会用到的信息. 如变量的引用
     * 信息. 这些信息可以通过简单的映射维护, 或者自行增加记录信息的数据结构.
     *
     * @param originInstructions 前端提供的中间代码
     */
    public void loadIR(List<Instruction> originInstructions) {
        // TODO: 读入前端提供的中间代码并生成所需要的信息
        for (Instruction instruction : originInstructions) {
            InstructionKind kind = instruction.getKind(); // 获取 IR 指令的种类
            //遇到Ret指令则直接舍弃后续指令
            if (kind.isReturn()){
                newInstructions.add(instruction);
                break;
            }
            if(kind.isUnary()) {
                //一元指令
                newInstructions.add(instruction);
            }else if (kind.isBinary()){
                //二元指令
                IRValue lhs = instruction.getLHS();
                IRValue rhs = instruction.getRHS();
                IRVariable result = instruction.getResult();

                if (lhs.isImmediate() && rhs.isImmediate()){
                    //两个操作数都是立即数
                    //直接计算结果
                    int newresult = 0;
                    switch (kind) {
                        case ADD -> newresult = ((IRImmediate)lhs).getValue() + ((IRImmediate)rhs).getValue();
                        case SUB -> newresult = ((IRImmediate)lhs).getValue() - ((IRImmediate)rhs).getValue();
                        case MUL -> newresult = ((IRImmediate)lhs).getValue() * ((IRImmediate)rhs).getValue();
                        case POW -> newresult = (int) Math.pow(((IRImmediate)lhs).getValue(), ((IRImmediate)rhs).getValue());
                        default -> System.err.println("Unknown instruction kind: " + kind);
                    }
                    newInstructions.add(Instruction.createMov(result, IRImmediate.of(newresult)));
                } else if (lhs.isImmediate() && rhs.isIRVariable()){
                    switch (kind) {
                        //加法将左立即数交换到右边即可
                        case ADD -> newInstructions.add(Instruction.createAdd(result, rhs, lhs));
                        //其余前插一条 MOV a imm
                        case SUB -> {
                            IRVariable temp = IRVariable.temp();
                            newInstructions.add(Instruction.createMov(temp, lhs));
                            newInstructions.add(Instruction.createSub(result, temp, rhs));
                        }
                        case MUL -> {
                            IRVariable temp = IRVariable.temp();
                            newInstructions.add(Instruction.createMov(temp, lhs));
                            newInstructions.add(Instruction.createMul(result, temp, rhs));
                        }
                        case POW -> {
                            IRVariable temp = IRVariable.temp();
                            newInstructions.add(Instruction.createMov(temp, lhs));
                            newInstructions.add(Instruction.createPow(result, temp, rhs));
                        }
                        default -> System.err.println("Unknown instruction kind: " + kind);
                    }
                } else if (lhs.isIRVariable() && rhs.isImmediate()){
                    switch (kind) {
                        case ADD, SUB  -> newInstructions.add(instruction);
                        case MUL -> {
                            IRVariable temp = IRVariable.temp();
                            newInstructions.add(Instruction.createMov(temp, rhs));
                            newInstructions.add(Instruction.createMul(result, lhs, temp));
                        }
                        case POW -> {
                            IRVariable temp = IRVariable.temp();
                            newInstructions.add(Instruction.createMov(temp, rhs));
                            newInstructions.add(Instruction.createPow(result, lhs, temp));
                        }
                        default -> System.err.println("Unknown instruction kind: " + kind);
                    }
                } else {
                        newInstructions.add(instruction);
                }
            }
        }
    }

    //分配寄存器
    public Register getReg(IRValue value, int regIndex){
        // 若是立即数则直接返回
        if(value.isImmediate())     return null;
        // 若已经分配过寄存器则直接返回
        if(registerMap.containsKey(value))      return registerMap.getByKey(value);
        // 寻找一个未分配的寄存器
        for(Register register : Register.values()){
            if(!registerMap.containsValue(register)){
                registerMap.replace(value, register);
                return register;
            }
        }
        // 若所有寄存器都已分配，则分配不再被变量占用的寄存器
        Set<Register> notUseRegs = Arrays.stream(Register.values()).collect(Collectors.toSet());
        for (int i = regIndex; i < newInstructions.size(); i++){
            Instruction instruction = newInstructions.get(i);
            //遍历搜寻不再使用的变量
            for(IRValue irValue : instruction.getOperands()){
                notUseRegs.remove(registerMap.getByKey(irValue));
            }
        }
        // 若找到可用寄存器则分配
        if(!notUseRegs.isEmpty()){
            registerMap.replace(value, notUseRegs.iterator().next());
            return notUseRegs.iterator().next();
        }
        // 若没有可用寄存器则抛出异常
        throw new RuntimeException("No register available");
    }

    /**
     * 执行代码生成.
     * <br>
     * 根据理论课的做法, 在代码生成时同时完成寄存器分配的工作. 若你觉得这样的做法不好,
     * 也可以将寄存器分配和代码生成分开进行.
     * <br>
     * 提示: 寄存器分配中需要的信息较多, 关于全局的与代码生成过程无关的信息建议在代码生
     * 成前完成建立, 与代码生成的过程相关的信息可自行设计数据结构进行记录并动态维护.
     */
    public void run() {
        // TODO: 执行寄存器分配与代码生成
        int i = 0;
        asmCodes.add(new AsmCode(".text"));
        for(Instruction instruction : newInstructions){
            InstructionKind kind = instruction.getKind();
            switch (kind){
                case ADD -> {
                    IRValue lhs = instruction.getLHS();
                    IRValue rhs = instruction.getRHS();
                    IRVariable result = instruction.getResult();
                    Register lhsReg = getReg(lhs, i);
                    Register rhsReg = getReg(rhs, i);
                    Register resultReg = getReg(result, i);
                    if(rhs.isImmediate()) {
                        asmCodes.add(AsmCode.createAddi(resultReg.toString(), lhsReg.toString(), rhs, instruction));
                    }else{
                        asmCodes.add(AsmCode.createAdd(resultReg.toString(), lhsReg.toString(), rhsReg.toString(), instruction));
                    }
                }
                case SUB -> {
                    IRValue lhs = instruction.getLHS();
                    IRValue rhs = instruction.getRHS();
                    IRVariable result = instruction.getResult();
                    Register lhsReg = getReg(lhs, i);
                    Register rhsReg = getReg(rhs, i);
                    Register resultReg = getReg(result, i);
                    if(rhs.isImmediate()) {
                        asmCodes.add(AsmCode.createSubi(resultReg.toString(), lhsReg.toString(), rhs, instruction));
                    }else{
                        asmCodes.add(AsmCode.createSub(resultReg.toString(), lhsReg.toString(), rhsReg.toString(), instruction));
                    }
                }
                case MUL -> {
                    IRValue lhs = instruction.getLHS();
                    IRValue rhs = instruction.getRHS();
                    IRVariable result = instruction.getResult();
                    Register lhsReg = getReg(lhs, i);
                    Register rhsReg = getReg(rhs, i);
                    Register resultReg = getReg(result, i);
                    asmCodes.add(AsmCode.createMul(resultReg.toString(), lhsReg.toString(), rhsReg.toString(), instruction));
                }
                case POW -> {
                    IRValue lhs = instruction.getLHS();
                    IRValue rhs = instruction.getRHS();
                    IRVariable result = instruction.getResult();
                    Register lhsReg = getReg(lhs, i);
                    Register rhsReg = getReg(rhs, i);
                    Register resultReg = getReg(result, i);
                    asmCodes.add(AsmCode.createPow(resultReg.toString(), lhsReg.toString(), rhsReg.toString(), instruction));
                }
                case MOV -> {
                    IRValue from = instruction.getFrom();
                    IRVariable result = instruction.getResult();
                    Register fromReg = getReg(from, i);
                    Register resultReg = getReg(result, i);
                    if(from.isImmediate()) {
                        asmCodes.add(AsmCode.createLi(resultReg.toString(), from, instruction));
                    }else{
                        asmCodes.add(AsmCode.createMv(resultReg.toString(), fromReg.toString(), instruction));
                    }
                }
                case RET -> {
                    IRValue returnValue = instruction.getReturnValue();
                    Register returnValueReg = getReg(returnValue, i);
                    asmCodes.add(AsmCode.createRet(returnValueReg.toString(), instruction));
                }
                default -> System.err.println("error asm!");
            }
            i ++;
            if(kind == InstructionKind.RET){
                break;
            }
        }
    }


    /**
     * 输出汇编代码到文件
     *
     * @param path 输出文件路径
     */
    public void dump(String path) {
        // TODO: 输出汇编代码到文件
        FileUtils.writeLines(path, asmCodes.stream().map(AsmCode::toString).toList());
    }
}

