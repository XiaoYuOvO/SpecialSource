package net.xiaoyu233.bytecode.lambda_matcher.filter;

import java.util.zip.ZipEntry;

public interface ZipFileFilter {
    boolean filter(ZipEntry entry);
}
