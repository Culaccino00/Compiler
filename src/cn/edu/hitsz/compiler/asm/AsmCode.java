package cn.edu.hitsz.compiler.asm;

import cn.edu.hitsz.compiler.ir.IRImmediate;
import cn.edu.hitsz.compiler.ir.IRValue;
import cn.edu.hitsz.compiler.ir.Instruction;

public class AsmCode {
    private String instructionKind = null;
    private String result = null;
    private String lhs = null;
    private String rhs = null;
    private String ir = null;
    private String text = null;

    public AsmCode(String instructionKind, String result, String lhs, String rhs, String ir) {
        this.instructionKind = instructionKind;
        this.result = result;
        this.lhs = lhs;
        this.rhs = rhs;
        this.ir = ir;
    }

    public AsmCode(String text) {
        this.text = text;
    }

    public static AsmCode createAdd(String result, String lhs, String rhs, Instruction ir) {
        return new AsmCode("add", result, lhs, rhs, ir.toString());
    }

    public static AsmCode createAddi(String result, String lhs, IRValue rhs, Instruction ir) {
        return new AsmCode("addi", result, lhs, Integer.toString(((IRImmediate)rhs).getValue()), ir.toString());
    }

    public static AsmCode createSub(String result, String lhs, String rhs, Instruction ir) {
        return new AsmCode("sub", result, lhs, rhs, ir.toString());
    }

    public static AsmCode createSubi(String result, String lhs, IRValue rhs, Instruction ir) {
        return new AsmCode("subi", result, lhs, Integer.toString(((IRImmediate)rhs).getValue()), ir.toString());
    }

    public static AsmCode createMul(String result, String lhs, String rhs, Instruction ir) {
        return new AsmCode("mul", result, lhs, rhs, ir.toString());
    }

    public static AsmCode createPow(String result, String lhs, String rhs, Instruction ir) {
        return new AsmCode("pow", result, lhs, rhs, ir.toString());
    }

    public static AsmCode createMv(String result, String lhs, Instruction ir) {
        return new AsmCode("mv", result, lhs, null, ir.toString());
    }

    public static AsmCode createRet(String lhs, Instruction ir) {
        return new AsmCode("mv", "a0", lhs, null, ir.toString());
    }
    public static AsmCode createLi(String result, IRValue lhs, Instruction ir) {
        return new AsmCode("li", result, Integer.toString(((IRImmediate)lhs).getValue()), null, ir.toString());
    }

    @Override
    public String toString() {
        if(".text".equals(text)) {
            return text;
        }
        if(!instructionKind.equals("pow")) {
            String line = "\t" + instructionKind + " " + result;
            if (lhs != null) {
                line += ", " + lhs;
            }
            if (rhs != null) {
                line += ", " + rhs;
            }
            if (ir != null) {
                line += "\t\t# " + ir;
            }
            return line;
        }else {
            //用mul完成pow操作
            String line = "\t" + "addi" + " " + result + ", " + lhs + ", " + "0\n";
            line += "\t" + "addi" + " " + "x28" + ", " + rhs + ", " + "0\n";
            line += "\t" + "LOOP:\n";
            line += "\t\t" + "addi" + " " + "x28" + ", " + "x28" + ", " + "-1\n";
            line += "\t\t" + "beq" + " " + "x28" + ", " + "zero" +", " + "END_LOOP\n";
            line += "\t\t" + "mul" + " " + result + ", " + result + ", " + lhs + "\n";
            line += "\t\t" + "j" + " " + "LOOP\n";
            line += "\t" +"END_LOOP:" + "\t\t# " + ir;
            return line;
        }
    }
}
