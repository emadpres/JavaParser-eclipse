package com.usi.emadpres.parser.parser.ParserCore.visitors;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Given a CompilationUnit, this visitor extracts:
 *  1. Field declarations (only Public)
 *  2. Method declarations (only Public)
 *  3. Method invocations
 */
@Deprecated //TODO: seems need to be deleted.
public class APIVisitor extends ASTVisitor {
    private static final Logger logger = LoggerFactory.getLogger(APIVisitor.class);
    private CompilationUnit unit;

    private List<String> fieldDeclarations = new ArrayList<String>();
    private List<String> methodDeclarations = new ArrayList<String>();
    private List<String> methodInvocations = new ArrayList<String>();

    @SuppressWarnings("unchecked")
    public boolean visit(FieldDeclaration node) {
        if (!Modifier.isPublic(node.getModifiers())) {
            return true;
        }

        for (VariableDeclarationFragment fragment : (List<VariableDeclarationFragment>) node.fragments()) {
            IVariableBinding binding = fragment.resolveBinding();
            if (binding != null) {
                fieldDeclarations.add(binding.getKey());
                logger.debug("FIELD: " + binding.getKey());
            }
        }
        return true;
    }

    public boolean visit(MethodDeclaration node) {
        if (!Modifier.isPublic(node.getModifiers())) {
            return true;
        }

        IMethodBinding binding = node.resolveBinding();
        if (binding != null) {
            // alternatively: BindingUtil.qualifiedSignature() provides: System.out.println(java.lang.String)
            methodDeclarations.add(binding.getKey());
            logger.debug("METHOD(" + unit.getLineNumber(node.getName().getStartPosition()) + "): " + binding.getKey());
        }
        return true;
    }

    public boolean visit(MethodInvocation node) {
        IMethodBinding binding = node.resolveMethodBinding();
        if (binding != null) {
            methodInvocations.add(binding.getKey());
            logger.debug("CALL: " + binding.getKey());
        }
        return true;
    }

    public void run(CompilationUnit unit) {
        this.unit = unit;
        this.unit.accept(this);
    }
}
