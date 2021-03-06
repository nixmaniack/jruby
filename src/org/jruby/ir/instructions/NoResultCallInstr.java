package org.jruby.ir.instructions;

import org.jruby.ir.Operation;
import org.jruby.ir.instructions.specialized.OneOperandArgNoBlockNoResultCallInstr;
import org.jruby.ir.operands.MethAddr;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.ir.targets.JVM;
import org.jruby.runtime.CallType;

public class NoResultCallInstr extends CallBase {
    public NoResultCallInstr(Operation op, CallType callType, MethAddr methAddr,
            Operand receiver, Operand[] args, Operand closure) {
        super(op, callType, methAddr, receiver, args, closure);
    }
    
    public NoResultCallInstr(NoResultCallInstr instr) {
        this(instr.getOperation(), instr.getCallType(), instr.methAddr, 
                instr.receiver, instr.arguments, instr.closure);
    }
    
    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new NoResultCallInstr(getOperation(), getCallType(), (MethAddr) getMethodAddr().cloneForInlining(ii), 
                receiver.cloneForInlining(ii), cloneCallArgs(ii), closure == null ? null : closure.cloneForInlining(ii));
    }

    @Override
    public CallBase specializeForInterpretation() {
        Operand[] callArgs = getCallArgs();
        if (hasClosure() || containsSplat(callArgs)) return this;
        
        switch (callArgs.length) {
//            case 0:
//                return new ZeroOperandArgNoBlockNoResultCallInstr(this);
            case 1:
//                if (isAllFixnums()) return new OneFixnumArgNoBlockNoResultCallInstr(this);

                return new OneOperandArgNoBlockNoResultCallInstr(this);
        }
        return this;
    }

    @Override
    public void compile(JVM jvm) {
        jvm.method().loadLocal(0);
        jvm.emit(getReceiver());
        for (Operand operand : getCallArgs()) {
            jvm.emit(operand);
        }

        switch (getCallType()) {
            case FUNCTIONAL:
            case VARIABLE:
                jvm.method().invokeSelf(getMethodAddr().getName(), getCallArgs().length);
                break;
            case NORMAL:
                jvm.method().invokeOther(getMethodAddr().getName(), getCallArgs().length);
                break;
            case SUPER:
                jvm.method().invokeSuper(getMethodAddr().getName(), getCallArgs().length);
                break;
        }

        jvm.method().adapter.pop();
    }
}
