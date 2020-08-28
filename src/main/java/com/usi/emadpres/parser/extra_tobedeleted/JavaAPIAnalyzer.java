package com.usi.emadpres.parser.extra_tobedeleted;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import com.usi.emadpres.parser.parser.ParserCore.visitors.APIVisitor;
import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FileASTRequestor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated // TODO: delete this. We already have {@link Parser::ParserCore::JavaParser)
public class JavaAPIAnalyzer {
	private static final Logger logger = LoggerFactory.getLogger(JavaAPIAnalyzer.class);

	private static String classpath = "";

	public static void parseFiles(String dir, String[] classpathArr) {

		// Extract files within `dir`
		File dirFile = new File(dir);
		Collection<File> javaFiles = FileUtils.listFiles(dirFile, new String[] { "java" }, true);
		String[] filesArr = new String[javaFiles.size()];
		int i = 0;
		for (File f : javaFiles) {
			filesArr[i] = f.getAbsolutePath();
			i++;
		}



		String[] dirArr = new String[1];
		dirArr[0] = dir;

		ASTParser parser = ASTParser.newParser(AST.JLS9);
		parser.setEnvironment(classpathArr, dirArr, new String[] { "UTF-8" }, true);

		parser.setResolveBindings(true);
		parser.setBindingsRecovery(true);
		parser.setStatementsRecovery(true);

		Map<String, String> options = JavaCore.getOptions();
		options.put("org.eclipse.jdt.core.compiler.source", "1.8");
		parser.setCompilerOptions(options);

		FileASTRequestor requestor = new FileASTRequestor() {
			@Override
			public void acceptAST(String path, CompilationUnit ast) {
				APIVisitor visitor = new APIVisitor();
				visitor.run(ast);
				IProblem[] problems = ast.getProblems();

				if (problems.length > 0) {
					ArrayList<String> messages = new ArrayList<String>();
					int error = 0;
					for (IProblem p : problems) {
						if (p.isError()) {
							++error;
							messages.add((Integer.toString(p.getSourceLineNumber()) + ": " + p.getMessage()));
						}
					}
					if (error > 0) {
						logger.warn("There were problems parsing the file: " + path);
						for (String s : messages) {
							logger.warn(s);
						}
					}
				}
			}
		};

		parser.createASTs(filesArr, null, new String[] {}, requestor, new NullProgressMonitor());
	}


}
