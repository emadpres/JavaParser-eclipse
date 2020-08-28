package com.usi.emadpres.parser.parser.ParserCore.visitors;

import java.util.HashSet;
import java.util.Set;

import com.usi.emadpres.parser.parser.ds.PackageDeclaration;
import com.usi.emadpres.parser.parser.ds.UserTypeDeclaration;
import org.eclipse.jdt.core.dom.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Identifies:
 *  - Package declarations
 *  - User type declarations (class, interface, enum, annotation)
 */
public class UserTypesAndPackagesVisitor extends ASTVisitor {
    private static final Logger logger = LoggerFactory.getLogger(UserTypesAndPackagesVisitor.class);

    public Set<UserTypeDeclaration> userTypeDeclarations = new HashSet<>();
	public Set<PackageDeclaration> packageNames = new HashSet<>();

	private final CompilationUnit unit;
	private final String projectName, commitSHA, fileRelativePath, dirRelativePath;

    public UserTypesAndPackagesVisitor(String projectName, String commitSHA, String fileRelativePath, String dirRelativePath, CompilationUnit unit)
    {
        this.unit = unit;
        this.projectName = projectName;
        this.commitSHA = commitSHA;
        this.fileRelativePath = fileRelativePath;
        this.dirRelativePath = dirRelativePath;
    }

    public void Parse() {
        this.unit.accept(this);
    }


    @Override
    public boolean visit(org.eclipse.jdt.core.dom.PackageDeclaration node)
    {
        IPackageBinding binding = node.resolveBinding();
        if(binding!=null)
        {
            PackageDeclaration newP = new PackageDeclaration(projectName, dirRelativePath, binding.getName());
            newP.commitSHA = commitSHA;
            packageNames.add(newP);
        }
        return true;
    }

    @Override
    public boolean visit(TypeDeclaration node)
    {
        ITypeBinding binding = node.resolveBinding();

        if(binding==null)
        {
            String parentsNames = GetQualifiedNameByTrackParentTypes(node, node.getName().toString());
            if(node.isInterface())
                userTypeDeclarations.add(new UserTypeDeclaration(projectName, commitSHA, fileRelativePath, parentsNames, UserTypeDeclaration.UserTypeDeclaration_Kind.INTERFACE, false));
            else
                userTypeDeclarations.add(new UserTypeDeclaration(projectName, commitSHA, fileRelativePath, parentsNames, UserTypeDeclaration.UserTypeDeclaration_Kind.CLASS, false));
        }
        else {
            if (binding.getQualifiedName().equals(binding.getTypeDeclaration().getQualifiedName()) == false)
                logger.error("************** CONFLICT in CLASS/INTERFACE name ******************** {} vs {}\n\n\n", binding.getQualifiedName(), binding.getTypeDeclaration().getQualifiedName());

            if(binding.isInterface())
                userTypeDeclarations.add(new UserTypeDeclaration(projectName, commitSHA, fileRelativePath, binding.getQualifiedName(), UserTypeDeclaration.UserTypeDeclaration_Kind.INTERFACE, true));
            else
                userTypeDeclarations.add(new UserTypeDeclaration(projectName, commitSHA, fileRelativePath, binding.getQualifiedName(), UserTypeDeclaration.UserTypeDeclaration_Kind.CLASS, true));
        }
        return true;
    }

    @Override
    public boolean visit(EnumDeclaration node) {
        ITypeBinding binding = node.resolveBinding();
        if(binding==null)
        {
            String parentsNames = GetQualifiedNameByTrackParentTypes(node, node.getName().toString());
            userTypeDeclarations.add(new UserTypeDeclaration(projectName, commitSHA, fileRelativePath, parentsNames, UserTypeDeclaration.UserTypeDeclaration_Kind.ENUM, false));

        }
        else {
            if (binding != null) {
                if (binding.getQualifiedName().equals(binding.getTypeDeclaration().getQualifiedName()) == false)
                    logger.error("************** CONFLICT in ENUM name ******************** {} vs {}\n\n\n", binding.getQualifiedName(), binding.getTypeDeclaration().getQualifiedName());
                userTypeDeclarations.add(new UserTypeDeclaration(projectName, commitSHA, fileRelativePath, binding.getQualifiedName(), UserTypeDeclaration.UserTypeDeclaration_Kind.ENUM, true));
            }
        }
        return true;

    }

    @Override
    public boolean visit(AnnotationTypeDeclaration node) {
        ITypeBinding binding = node.resolveBinding();
        if(binding==null)
        {
            String parentsNames = GetQualifiedNameByTrackParentTypes(node, node.getName().toString());
            userTypeDeclarations.add(new UserTypeDeclaration(projectName, commitSHA, fileRelativePath, parentsNames, UserTypeDeclaration.UserTypeDeclaration_Kind.ANNOTATION, false));

        }
        else {
            if (binding != null) {
                if (binding.getQualifiedName().equals(binding.getTypeDeclaration().getQualifiedName()) == false)
                    logger.error("************** CONFLICT in ANNOTATION name ******************** {} vs {}\n\n\n", binding.getQualifiedName(), binding.getTypeDeclaration().getQualifiedName());
                userTypeDeclarations.add(new UserTypeDeclaration(projectName, commitSHA, fileRelativePath, binding.getQualifiedName(), UserTypeDeclaration.UserTypeDeclaration_Kind.ANNOTATION, true));
            }
        }
        return true;

    }

    private String GetQualifiedNameByTrackParentTypes(ASTNode node, String simpleName) {
        String toRet = simpleName;
        ASTNode n = node;
        while(n.getParent()!=null)
        {
            n = n.getParent();
            if(n instanceof AbstractTypeDeclaration)
                toRet = ((TypeDeclaration)n).getName().toString()+"."+toRet;
        }
        return toRet;
    }
}
