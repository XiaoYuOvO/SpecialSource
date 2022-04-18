package net.xiaoyu233.bytecode.lambda_matcher.processor;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarEntry;

public interface JarFileProcessor {
    boolean suitableFor(JarEntry entry);
    void process(JarEntry entry, InputStream in) throws IOException;

    void startProcess(String fileName);
    void finishProcess();
}
