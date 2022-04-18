package net.xiaoyu233.bytecode.lambda_matcher;

import net.xiaoyu233.bytecode.lambda_matcher.processor.JarFileProcessor;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JarClassProvider {
    private final JarFile jarFile;
    private final List<JarFileProcessor> processors = new ArrayList<>();
    public JarClassProvider(JarFile jarFile) {
        this.jarFile = jarFile;
    }

    public void addProcessor(JarFileProcessor processor) {
        this.processors.add(processor);
    }

    public void run() throws IOException {
        Enumeration<JarEntry> entries = jarFile.entries();
        processors.forEach((jarFileProcessor -> jarFileProcessor.startProcess(this.jarFile.getName())));
        while (entries.hasMoreElements()) {
            JarEntry jarEntry = entries.nextElement();
            for (JarFileProcessor processor : processors) {
                if (processor.suitableFor(jarEntry)){
                    try {
                        processor.process(jarEntry,jarFile.getInputStream(jarEntry));
                    }catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        processors.forEach(JarFileProcessor::finishProcess);
    }

    public InputStream findFile(String name) throws IOException{
        JarEntry jarEntry = jarFile.getJarEntry(name);
        if (jarEntry != null){
            return jarFile.getInputStream(jarEntry);
        }else {
            throw new IOException("Cannot find jar entry " + name + " in " + jarFile.getName());
        }
    }

    public String getJarName(){
        return jarFile.getName();
    }
}
