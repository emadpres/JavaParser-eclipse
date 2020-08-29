package com.usi.emadpres.parser.parser.ParserCore;

import com.usi.emadpres.parser.parser.db.ProjectParsingResultDB;
import com.usi.emadpres.parser.parser.ds.*;
import com.usi.emadpres.parser.parser.ParserCore.visitors.MethodDeclarationVisitor;
import com.usi.emadpres.parser.parser.ParserCore.visitors.MethodInvocationVisitor;
import com.usi.emadpres.parser.parser.ParserCore.visitors.UserTypesAndPackagesVisitor;
import com.usi.emadpres.parser.parser.ds.PackageDeclaration;
import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Given a project, it extracts:
 *      - Method invocations
 *              --> MethodInvocations.db (MethodInvocation, MethodInvocationLine)
 *      - Method declarations
 *              --> APIDeclarations.db (APIDeclaration)
 *      - Package declarations
 *              --> MavenPackages.db (MavenLibs, MavenPackageDef)
 *              --> ReposPackages.db        (Repos,     RepoPackageDef)
 *              --> ReposPackagesClasses.db (Repos, RepoPackageDef, RepoTypeDef)
 *      - User type declarations
 */
public class EclipseJavaParser {
    private static final Logger logger = LoggerFactory.getLogger(EclipseJavaParser.class);

    public static void main(String[] args) {
        if(args.length!=6)
        {
            System.err.println("Usage: <project-name> <project-path> <call> <decl> <pack> <output-file>");
            return;
        }
        String projectName = args[0];
        String projectPath = args[1];
        boolean parseMethodInvocation = Boolean.parseBoolean(args[2]);
        boolean parseMethodDeclaration = Boolean.parseBoolean(args[3]);
        boolean parsePackageAndTypeDeclaration = Boolean.parseBoolean(args[4]);
        Path output = Paths.get(args[5]);

        ProjectParsingResult res;
        res = ParseProject(projectName,null, projectPath,null,
                new ParserConfig(parseMethodInvocation, parseMethodDeclaration, parsePackageAndTypeDeclaration));
        res = new ProjectParsingResult(projectName, new ArrayList<>(), new ArrayList<>(), new HashSet<>(), new HashSet<>());
        ProjectParsingResultDB.Report(res);
        ProjectParsingResultDB.WriteToSQLite(res, output);
    }

    /**
     *
     * @param projectName     Can be the repository full name
     * @param pathToProject Local path to root of the project
     * @param dependencyClasspathArr  Class paths (dependency jar file like: ".../.m2/repository/junit/junit/4.12/junit-4.12.jar")
     * @param config
     * @return
     */
    public static ProjectParsingResult ParseProject(String projectName, String commitSHA, String pathToProject, String[] dependencyClasspathArr, ParserConfig config) {
        if(Files.exists(Paths.get(pathToProject))==false)
        {
            logger.error("SKIPPING Project: Path Not Found : {}", pathToProject);
            return null;
        }

        // Get list of all java files
        File rootDir = new File(pathToProject);
        Collection<File> javaFiles = FileUtils.listFiles(rootDir, new String[] { "java" }, true);
        String[] javaFilesArr = new String[javaFiles.size()];
        int i = 0;
        for (File f : javaFiles) {
            javaFilesArr[i] = f.getAbsolutePath();
            i++;
        }

        ProjectParsingResult result = DoParse(projectName, commitSHA, rootDir.toPath(), javaFilesArr, dependencyClasspathArr, config);


        return result;
    }

    /**
     * ******************************************************************
     * *** My Experience on how Eclipse Java Parser handle cross-file references:
     * ***      - Assume a project containing two classes A and B (and many more).
     * ***      - Assume in file A, a variable of type B is used (e.g., as return type for a method).
     * ***      - If we pass both file A and B (along others) to Java Parser, it will resolve binding
     * ***          for type B when parsing A.java file
     * ***      - However, if B.java is not passed, it fails to resolve B type.
     * ***      -- For replication consider:
     * ***         A = "apache/commons-beanutils -> src/main/java/org/apache/commons/beanutils2/ConvertUtilsBean.java";
     * ***         B = "apache/commons-beanutils -> src/main/java/org/apache/commons/beanutils2/converters/BigIntegerConverter.java";
     * ******************************************************************
     */
    public static ProjectParsingResult ParseProjectFiles(String projectName, String commitSHA, String projectPath, List<String> relativeFilePaths, String[] classpathArr, ParserConfig config) {
        String[] filePaths = new String[relativeFilePaths.size()];
        for(int i=0; i<relativeFilePaths.size(); i++)
            filePaths[i] = Paths.get(projectPath, relativeFilePaths.get(i)).toAbsolutePath().toString();
        ProjectParsingResult result = DoParse(projectName, commitSHA, Paths.get(projectPath), filePaths, classpathArr, config);
        return result;
    }

    /**
     *  Parse an orphan java file
     * Read JavaDoc of {@link #ParseProjectFiles} to learn more how cross-file references are resolved.
     */
    public static ProjectParsingResult ParseSingleFile(String projectName, String commitSHA, String filePath, String[] classpathArr, ParserConfig config) {
        File file = new File(filePath);
        ProjectParsingResult result = DoParse(projectName, commitSHA, file.getParentFile().toPath(), new String[]{filePath}, classpathArr, config);
        return result;
    }

    /**
     * Eclipse Java Parser is invoked here.
     */
    private static ProjectParsingResult DoParse(String projectName, String commitSHA, Path pathToProject, String[] javaFilesArr, String[] classpathArr, ParserConfig config) {

        ASTParser parser = ASTParser.newParser(AST.JLS14); // Update this when you update eclipse parser Maven lib version.
        parser.setResolveBindings(true);
        parser.setBindingsRecovery(true);
        parser.setStatementsRecovery(true);
        parser.setEnvironment(classpathArr,
                new String[]{pathToProject.toString()} ,
                new String[] { "UTF-8" }, true);

        /* XP: If we comment out line below ("org.eclipse.jdt.core.compiler.source"="11")
            (1) It no longer detects "new ArrayList<>()" because empty angular brackets is not inferred in older java.
            (2) Although "setStatementsRecovery" is on, it skip parsing lines after error similar to item (1).
        */
        Map<String, String> options = JavaCore.getOptions();
        options.put("org.eclipse.jdt.core.compiler.source", "11");
        parser.setCompilerOptions(options);


        ArrayList<MethodInvocationInfo> methodInvocations = new ArrayList<>();
        ArrayList<MethodDeclarationInfo> methodDeclarations = new ArrayList<>();
        Set<PackageDeclaration> packageDeclarations = new HashSet<>();
        Set<UserTypeDeclaration> userTypeDeclarations = new HashSet<>();

        final int total = javaFilesArr.length;
        int counter=0;

        FileASTRequestor requestor = new FileASTRequestor() {
            @Override
            public void acceptAST(String fullPath, CompilationUnit ast) {
                //logger.debug("\t\t\t\tVisiting... {} --> {}", projectName, fullPath);

                String fileRelativePath = !config.storePathsAsAbsolute?(fullPath.substring(pathToProject.toString().length()+1)):fullPath;
                String dirRelativePath = Paths.get(fileRelativePath).getParent().toString();

                try {
                    if(config.parseMethodInvocations) { // Method Invocations
                        MethodInvocationVisitor iVisitor = new MethodInvocationVisitor(projectName, commitSHA, fileRelativePath, ast);
                        iVisitor.Parse();
                        methodInvocations.addAll(iVisitor.methodInvocations_thisUnit);
                    }
                } catch (Exception e)
                {
                    logger.error("{} --> Error while parsing method invocations: {} @ {}", projectName, fullPath, commitSHA);
                    logger.error("Exception: ", e);
                    //e.printStackTrace();
                    //PrintASTProblems(fullPath, ast);
                }


                try {
                    if(config.parseMethodDeclarations) { // Method Declarations
                        MethodDeclarationVisitor dVisitor = new MethodDeclarationVisitor(projectName, commitSHA, fileRelativePath, ast);
                        dVisitor.Parse();
                        methodDeclarations.addAll(dVisitor.methodDeclarations_thisUnit);
                    }
                } catch (Exception e) {
                    logger.error("{} --> Error while parsing method declarations: {} @ {}", projectName, fullPath, commitSHA);
                    logger.error("Exception: ", e);
                    //e.printStackTrace();
                    //PrintASTProblems(fullPath, ast);
                }

                try {
                    if(config.parsePackagesAndUserDefinedTypes) {
                        // Package declarations + Type declarations (class, enum, ...)
                        UserTypesAndPackagesVisitor tVisitor = new UserTypesAndPackagesVisitor(projectName, commitSHA, fileRelativePath, dirRelativePath, ast);
                        tVisitor.Parse();
                        if (tVisitor.packageNames.size() > 1) // every non-empty file has one package name only
                            logger.error("Fatal Error: Unexpected number of packages : Repo: {} / File: {} / Packages: {} / SHA: {}", pathToProject, fullPath, tVisitor.packageNames, commitSHA);
                        packageDeclarations.addAll(tVisitor.packageNames);
                        for (UserTypeDeclaration t : tVisitor.userTypeDeclarations) {
                            if (t.isBinded == false && tVisitor.packageNames.size() == 1) {
                                PackageDeclaration thePackageDecl = tVisitor.packageNames.iterator().next();
                                t.fullyQualifiedName = thePackageDecl.fullyQualifiedPackageName + t.fullyQualifiedName;
                            }
                            userTypeDeclarations.add(t);

                        }
                    }
                } catch (Exception e)
                {
                    logger.error("{} --> Error while parsing packages/user defined types: {} @ {}", projectName, fullPath, commitSHA);
                    logger.error("Exception: ", e);
                    //e.printStackTrace();
                    //PrintASTProblems(fullPath, ast);
                }
            }
        };


        boolean successful = false;
        try {
            parser.createASTs(javaFilesArr, null, new String[]{}, requestor, new NullProgressMonitor());
            successful = true;
        } catch(Exception e)
        {
            logger.error("Exception While Parsing Project: {} (Eclipse Parsing operation SKIPPED]", pathToProject);
            logger.error("Exception: ", e);
        }
        if(successful) {
            ProjectParsingResult result = new ProjectParsingResult(projectName, methodInvocations, methodDeclarations,
                                            packageDeclarations, userTypeDeclarations);
            return result;
        }
        else
            return null;
    }

    private static void PrintASTProblems(String fullPath, CompilationUnit ast) {
        logger.debug("---------------------------- Printing AST Problems ------------------");
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
                logger.debug("There were problems parsing the file: " + fullPath);
                for (String s : messages) {
                    logger.debug(s);
                }
            }
        }
        logger.debug("-------------------------------------------------------------------------");
    }
}
