package com.usi.emadpres.parser.parser.ParserCore.visitors;

import com.usi.emadpres.parser.parser.ParserCore.utils.BindingUtil;
import com.usi.emadpres.parser.parser.ds.MethodInvocationInfo;
import org.eclipse.jdt.core.dom.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 *  Record Method Invocations in the current CompilationUnit.
 *  In case of multiple invocations of the same method, we add line numbers in a list.
 */
public class MethodInvocationVisitor extends ASTVisitor {
    private static final Logger logger = LoggerFactory.getLogger(MethodInvocationVisitor.class);

    private CompilationUnit unit;
    private final String fileRelativePath, projectName, commitSHA;

    public List<MethodInvocationInfo> methodInvocations_thisUnit = new ArrayList<>();

    public MethodInvocationVisitor(String projectName, String commitSHA,String fileRelativePath, CompilationUnit unit)
    {
        this.fileRelativePath = fileRelativePath;
        this.unit = unit;
        this.projectName = projectName;
        this.commitSHA = commitSHA;
    }

    public void Parse() {
        this.unit.accept(this);
    }

    /**
     * Visiting a class constructor call
     */
    @Override
    public boolean visit(ClassInstanceCreation node)
    {
        IMethodBinding binding =  node.resolveConstructorBinding();
        ExtractMethodInvocations(binding, node);
        return true;
    }

    /**
     * Visiting a method invocation (not a constructor)
     */
    @Override
    public boolean visit(MethodInvocation node) {
        IMethodBinding binding = node.resolveMethodBinding();
        ExtractMethodInvocations(binding, node);
        return true;
    }

    private void ExtractMethodInvocations(IMethodBinding binding, ASTNode node) {
        if(binding==null) {
            //logger.warn("Method invocation binding is null: {}", node);
            return;
        }

        IMethodBinding methodDeclaration = binding.getMethodDeclaration();

        String qualifiedClassName = BindingUtil.qualifiedName(methodDeclaration.getDeclaringClass());
        //String qualifiedClassName2 = methodDeclaration.getDeclaringClass().getQualifiedName();
        //if(qualifiedClassName.equals(qualifiedClassName2)==false)
        //    System.err.printf("DIFF QUALIFIED CLASS: %s --vs-- %s", qualifiedClassName, qualifiedClassName2);
        String methodName = methodDeclaration.getName();
        if(methodName.isEmpty())
        {
            /*  When this happens?
                1) When we visit the constructor of an anonymous type
                    Example: Thread t = new Thread() { public void run(){} };
                2) [UPDATE ME WITH NEW CASES]
             */
            return;
        }
        int lineNumStart = unit.getLineNumber(node.getStartPosition()-1);

        // Some Hints:
        //   - BindingUtil.qualifiedName(binding.getMethodDeclaration().getReturnType()); ==> java.lang.List [GOOD]
        //   - binding.getMethodDeclaration().getReturnType().getQualifiedName() ==> java.lang.List<com.boo.foo> [BAD]
        //   - binding.getReturnType().getQualifiedName() ==> [BAD]
        String returnType = BindingUtil.qualifiedName(methodDeclaration.getReturnType());
        int nArgs = methodDeclaration.getParameterTypes().length;
        String argsTypes = "";
        for(int i=0; i<nArgs; i++)
        {
            argsTypes += BindingUtil.qualifiedName(methodDeclaration.getParameterTypes()[i]);
            if(i!=nArgs-1)
                argsTypes += ", ";
        }

        MethodInvocationInfo newInvocInfo = new MethodInvocationInfo(qualifiedClassName, returnType, methodName, nArgs, argsTypes);
        newInvocInfo.fileRelativePath = this.fileRelativePath;
        newInvocInfo.lineNumbers.add(lineNumStart);
        newInvocInfo.projectName = this.projectName;
        newInvocInfo.commitSHA = this.commitSHA;
        newInvocInfo.isJavaInvocation = qualifiedClassName.startsWith("java.");

        boolean found = false;
        for (MethodInvocationInfo inf : methodInvocations_thisUnit)
            if (inf.equals(newInvocInfo)) {
                inf.lineNumbers.add(lineNumStart);
                found = true;
                break;
            }

        if (!found) {
            methodInvocations_thisUnit.add(newInvocInfo);
        }
    }
}
