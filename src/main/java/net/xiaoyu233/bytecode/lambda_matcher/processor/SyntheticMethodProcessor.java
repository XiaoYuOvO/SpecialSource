package net.xiaoyu233.bytecode.lambda_matcher.processor;

import com.mojang.datafixers.util.Either;
import jdk.internal.org.objectweb.asm.Opcodes;
import net.md_5.specialsource.NodeType;
import net.md_5.specialsource.Ownable;
import net.md_5.specialsource.writer.Searge;
import net.xiaoyu233.bytecode.lambda_matcher.EmptyResult;
import net.xiaoyu233.bytecode.lambda_matcher.JarClassProvider;
import net.xiaoyu233.bytecode.lambda_matcher.filter.BytecodeFilter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.stream.Collectors;

public class SyntheticMethodProcessor implements JarFileProcessor {
    private final JarClassProvider referenceJar;

    private final List<BytecodeFilter> filters = new ArrayList<>();
    private final List<String> matched = new ArrayList<>();

    private Searge srg;
    private final PrintWriter errWriter = new PrintWriter(new FileWriter("lambda_match_err.txt",false));

    public SyntheticMethodProcessor(JarClassProvider referenceJar) throws IOException {
        this.referenceJar = referenceJar;
    }

    public void addFilter(BytecodeFilter filter){
        this.filters.add(filter);
    }

    private static boolean syntheticMethod(MethodNode method) {
        return (Opcodes.ACC_SYNTHETIC & method.access) == Opcodes.ACC_SYNTHETIC;
    }

    @Override
    public void startProcess(String fileName) {
        srg = new Searge(fileName,referenceJar.getJarName());
    }

    @Override
    public void finishProcess() {
        try (PrintWriter printWriter = new PrintWriter(new FileWriter("lambda_match.srg", false))) {
            srg.write(printWriter);
            errWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean suitableFor(JarEntry entry) {
        return entry.getName().endsWith(".class");
    }

    @Override
    public void process(JarEntry entry, InputStream in) throws IOException {
        ClassReader targetClassReader = new ClassReader(in);
        ClassNode targetClass = new ClassNode();
        targetClassReader.accept(targetClass,0);
        ClassReader refClassReader = new ClassReader(referenceJar.findFile(entry.getName()));
        ClassNode refClass = new ClassNode();
        refClassReader.accept(refClass,0);
        List<MethodNode> targetMethods = targetClass.methods.stream().filter(SyntheticMethodProcessor::syntheticMethod).collect(Collectors.toList());
        List<MethodNode> refMethods = refClass.methods.stream().filter(SyntheticMethodProcessor::syntheticMethod).collect(Collectors.toList());
        this.matchLambdaNames(targetClass.name,targetMethods,refMethods);
    }

    private void matchLambdaNames(String className,List<MethodNode> targetMethods,List<MethodNode> refMethods){
        List<MethodNode> backup = new ArrayList<>(refMethods);
        for (MethodNode targetMethod : targetMethods) {
            for (BytecodeFilter filter : this.filters) {
                Either<EmptyResult, List<MethodNode>> filterResult = filter.filter(targetMethod, refMethods);
                Optional<EmptyResult> left = filterResult.left();
                Optional<List<MethodNode>> right = filterResult.right();
                if (left.isPresent()) {
                    errWriter.println("No matching reference for " + className + "." + targetMethod.name + targetMethod.desc + " was found");
                    errWriter.println("    " + left.get().getInfo());
                    break;
                }else if (right.isPresent()){
                    List<MethodNode> matchedMethods = right.get();
                    refMethods.clear();
                    refMethods.addAll(matchedMethods);
                    if (refMethods.size() == 1) {
                        MethodNode refMethod = refMethods.get(0);
                        System.out.println("Method matched: " + className + "." + targetMethod.name + " -> " + refMethod.name);
                        String key = className + "." + targetMethod.name + targetMethod.desc;
                        if (!matched.contains(key)) {
                            srg.addMethodMap(
                                    new Ownable(NodeType.METHOD, className, targetMethod.name, refMethod.desc, 0),
                                    new Ownable(NodeType.METHOD, className, refMethod.name, refMethod.desc, 0));
                            matched.add(key);
                        }
                        //Remove the selected one
                        backup.remove(refMethod);
                        break;
                    }
                }
            }
            if (refMethods.size() > 1){
                errWriter.println("Method " + className + "."+targetMethod.name + " "+targetMethod.desc + " has multiple matched candidates");
                errWriter.println("    They are:");
                for (MethodNode refMethod : refMethods) {
                    errWriter.println("    " + refMethod.name + " "+targetMethod.desc);
                }
            }
            //Resume the refMethods but no the selected one
            refMethods.clear();
            refMethods.addAll(backup);
        }
    }

    private void filterSingleMethod(String className,MethodNode targetMethod,List<MethodNode> refMethods){

    }
}
