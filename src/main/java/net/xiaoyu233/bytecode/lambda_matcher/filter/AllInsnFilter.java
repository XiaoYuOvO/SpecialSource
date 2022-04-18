package net.xiaoyu233.bytecode.lambda_matcher.filter;

import com.mojang.datafixers.util.Either;
import net.xiaoyu233.bytecode.lambda_matcher.EmptyResult;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AllInsnFilter implements BytecodeFilter{
    @Override
    public Either<EmptyResult, List<MethodNode>> filter(MethodNode target, List<MethodNode> candidates) {
        StringBuilder notMatchReasons = new StringBuilder("Cannot find full matched reference for " + target.name + target.desc + "\n");
        List<MethodNode> collect = candidates.stream().filter(reference -> matches(target, reference).ifRight(notMatchReasons::append).left().isPresent()).collect(Collectors.toList());
        if (collect.isEmpty()){
            return Either.left(new EmptyResult(notMatchReasons.toString()));
        }else return Either.right(collect);
    }

    private Either<Boolean,String> matches(MethodNode target,MethodNode reference){
        int index = 0;
        List<AbstractInsnNode> refInstructions = filterInsn(reference.instructions);
        for (AbstractInsnNode instruction : filterInsn(target.instructions)) {
            AbstractInsnNode insnNode = refInstructions.get(index);
            if (!BytecodeFilter.insnEquals(instruction, insnNode,target,reference)) {
                return Either.right("    Insn not equals at " + index + " : "+ instruction + " <==> " + insnNode + "  (" + reference.name + ")\n");
            }
            index++;
        }
        return Either.left(true);
    }

    private List<AbstractInsnNode> filterInsn(InsnList instructions){
        ArrayList<AbstractInsnNode> result = new ArrayList<>();
        for (AbstractInsnNode instruction : instructions) {
            if (BytecodeFilter.shouldCompareInsn(instruction)){
                result.add(instruction);
            }
        }
        return result;
    }
}
