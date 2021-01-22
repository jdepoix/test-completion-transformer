package org.jdepoix.dataset.ast;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.io.IOException;
import java.nio.file.Path;

public class DefaultParser {
    static {
        final CombinedTypeSolver typeSolver = new CombinedTypeSolver();
        typeSolver.add(new ReflectionTypeSolver());
        StaticJavaParser.getConfiguration().setSymbolResolver(new JavaSymbolSolver(typeSolver));
    }

    public static CompilationUnit parse(String code) {
        return StaticJavaParser.parse(code);
    }

    public static CompilationUnit parse(Path path) throws IOException {
        return StaticJavaParser.parse(path);
    }
}
