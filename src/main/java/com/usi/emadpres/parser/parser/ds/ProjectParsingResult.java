package com.usi.emadpres.parser.parser.ds;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ProjectParsingResult {
    private static final Logger logger = LoggerFactory.getLogger(ProjectParsingResult.class);

    public boolean incompleteResult; // useful when we didn't finish parsing all files (like when Parser crashes at createASTs() line, not at acceptAST()), but still partial results are useful.
    public String projectName;
    public List<MethodInvocationInfo> methodInvocations;
    public List<MethodDeclarationInfo> methodDeclarations;
    public Set<PackageDeclaration> packageDeclarations;
    public Set<UserTypeDeclaration> userTypeDeclarations;

    public ProjectParsingResult(String projectName)
    {
        incompleteResult = false;
        this.projectName = projectName;
        this.methodInvocations = null;
        this.methodDeclarations = null;
        this.packageDeclarations = null;
        this.userTypeDeclarations = null;
    }

    public ProjectParsingResult(String projectName, ArrayList<MethodInvocationInfo> methodInvocations,
                                ArrayList<MethodDeclarationInfo> methodDeclarations,
                                Set<PackageDeclaration> packageDeclarations,
                                Set<UserTypeDeclaration> userTypeDeclarations) {
        incompleteResult = false;
        this.projectName = projectName;
        this.methodInvocations = methodInvocations;
        this.methodDeclarations = methodDeclarations;
        this.packageDeclarations = packageDeclarations;
        this.userTypeDeclarations = userTypeDeclarations;
    }
}
