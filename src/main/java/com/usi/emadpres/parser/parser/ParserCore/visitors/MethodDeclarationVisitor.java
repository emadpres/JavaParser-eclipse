package com.usi.emadpres.parser.parser.ParserCore.visitors;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import com.usi.emadpres.parser.parser.ParserCore.utils.BindingUtil;

import com.usi.emadpres.parser.parser.ds.MethodDeclarationInfo;
import org.eclipse.jdt.core.dom.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MethodDeclarationVisitor extends ASTVisitor {
    private static final Logger logger = LoggerFactory.getLogger(MethodDeclarationVisitor.class);

	private CompilationUnit unit;
    private final String fileRelativePath, projectName, commitSHA;

	public ArrayList<MethodDeclarationInfo> methodDeclarations_thisUnit = new ArrayList<>();


    public MethodDeclarationVisitor(String projectName, String commitSHA, String fileRelativePath, CompilationUnit unit)
    {
        this.fileRelativePath = fileRelativePath;
        this.unit = unit;
        this.projectName = projectName;
        this.commitSHA = commitSHA;
    }

    public void Parse() {
        this.unit.accept(this);
    }

//    @Override
//    public boolean visit(AnnotationTypeMemberDeclaration node) { ---> This is something different than Method Declaration. Maybe in future we support it, but not in this visitor
//        IMethodBinding binding = node.resolveBinding();
//        extractMethodDeclaration(binding, node);
//        return true;
//    }

    @Override
	public boolean visit(MethodDeclaration node) {
		IMethodBinding binding = node.resolveBinding();
        extractMethodDeclaration(binding, node);
		return true;
	}


	public void extractMethodDeclaration(IMethodBinding binding, BodyDeclaration node)
    {
        if(binding==null) {
            //logger.warn("Method declaration binding is null: {}", node);
            return;
        }

        IMethodBinding methodDeclaration = binding.getMethodDeclaration();

        String methodName = methodDeclaration.getName();

        /**
         * "method_line*" refers to start and end of everything (including both)
         * "javaDoc_line*" refers to java-doc part
         * "javacode_line*" refer to everything after java-doc
         * "methodBody_line*" refers to part between { and }
         */
        int method_lineStart = unit.getLineNumber(node.getStartPosition()-1);
        int method_lineEnd = unit.getLineNumber(node.getStartPosition()+node.getLength());

        Javadoc javadoc_block = node.getJavadoc();
        int javaDoc_lineStart=-1;
        int javaDoc_lineEnd =-1;
        if(javadoc_block!=null)
        {
            javaDoc_lineStart = unit.getLineNumber(javadoc_block.getStartPosition());
            javaDoc_lineEnd = unit.getLineNumber(javadoc_block.getStartPosition() + javadoc_block.getLength());
        }
        //int javaCode_lineStart = (javadoc_block!=null)?(javaDoc_lineEnd+1):method_lineStart;
        //int JavaCode_lineEnd = method_lineEnd;
        //int methodName_line= unit.getLineNumber(((MethodDeclaration) node).getName().getStartPosition());

        Block body = ((MethodDeclaration) node).getBody();
        String methodBody = "";
        //int methodBody_lineNumStart = -1;
        //int methodBody_lineNumEnd = -1;
        if(body!=null) {
            //methodBody_lineNumStart = unit.getLineNumber(body.getStartPosition());
            //methodBody_lineNumEnd = unit.getLineNumber(body.getStartPosition() + body.getLength());
            // TODO: Body doesn't contain "signature" and starts from "{".
            // Solution: consider parent of `body node` and remove first X lines based on number of javadoc lines.
            methodBody = body.toString();
        }

        boolean isConstructor = methodDeclaration.isConstructor();
        IAnnotationBinding[] annotations = methodDeclaration.getAnnotations();
        List modifiers = node.modifiers(); // public, static, ...
        //List thrownExceptions = ((MethodDeclaration) node).thrownExceptionTypes();





        String qualifiedClassName = getQualifiedClassName(methodDeclaration);
        if (qualifiedClassName == null)
            return; // We discards

        /*
        BindingUtil.qualifiedName(binding.getMethodDeclaration().getReturnType()); ==> java.lang.List [GOOD]
        binding.getMethodDeclaration().getReturnType().getQualifiedName() ==> java.lang.List<com.boo.foo> [BAD]
        binding.getReturnType().getQualifiedName() ==> [BAD]
         */
        String returnType = BindingUtil.qualifiedName(methodDeclaration.getReturnType());
        int nArgs = methodDeclaration.getParameterTypes().length;
        String argsTypes = "";
        for(int i=0; i<nArgs; i++)
        {
            argsTypes += BindingUtil.qualifiedName(methodDeclaration.getParameterTypes()[i]);
            if(i!=nArgs-1)
                argsTypes += ", ";
        }


        MethodDeclarationInfo info = new MethodDeclarationInfo(qualifiedClassName, returnType, methodName, nArgs, argsTypes, method_lineStart, method_lineEnd);
        info.projectName = this.projectName;
        info.commitSHA = this.commitSHA;
        info.fileRelativePath = this.fileRelativePath;
        info.isPublic = isMethodPublic(node)==true?1:0;
        info.isConstructor = isConstructor;
        info.methodBody_java = methodBody;

        if(javadoc_block !=null) {
            info.hasJavaDoc = true;
            info.javaDoc = javadoc_block.toString();
            info.javaDocStartLine = javaDoc_lineStart;
            info.javaDocEndLine = javaDoc_lineEnd;
        }
        else {
            info.hasJavaDoc = false;
            info.javaDoc = "";
            info.javaDocStartLine=-1;
            info.javaDocEndLine=-1;
        }

        methodDeclarations_thisUnit.add(info);
    }

    private boolean isMethodPublic(BodyDeclaration node) {
        // Note: As "Annotations" are a form of "Interface", their fields are also automatically public.
        boolean isPublic = true;
        boolean noVisibilityIssueFaced = false;
        BodyDeclaration curNode = node;
        while(curNode!=null && isPublic)
        {
            if(curNode instanceof TypeDeclaration || curNode instanceof EnumDeclaration || curNode instanceof AnnotationTypeDeclaration) //Class or Interface || Enum
            {
                if(Modifier.isPrivate(curNode.getModifiers()) || Modifier.isProtected(curNode.getModifiers()))
                    isPublic  = false;
                else if(Modifier.isPublic(curNode.getModifiers()))
                {
                    if(noVisibilityIssueFaced==false) {
                        isPublic = true;
                    }
                    else
                    {
                        if(curNode instanceof TypeDeclaration) {
                            TypeDeclaration t = (TypeDeclaration) (curNode);
                            if (t.isInterface()) {
                                isPublic = true; // No Visibility for Interface => public
                            }
                            else {
                                isPublic = false; // No Visibility for class => not public
                            }
                        }
                        else if(curNode instanceof AnnotationTypeDeclaration)
                        {
                            isPublic = true; // No Visibility for Annotation => public
                        }
                        else {
                            isPublic = false; // No Visibility for enum => not public
                        }
                        noVisibilityIssueFaced = false; //resolved
                    }

                }
                else {
                    isPublic  = false;
                }

            }
            else
            { // MethodDeclarations || AnnotationTypeMemberDeclaration

                if(Modifier.isPrivate(curNode.getModifiers()) || Modifier.isProtected(curNode.getModifiers())) {
                    isPublic = false;
                }
                else if (Modifier.isPublic(curNode.getModifiers())) {
                    isPublic = true;
                }
                else {
                    /*
                    It means either there's no visibility or "public" visibility (Modifier.isPublic(curNode.getModifiers())=true)
                    For the former case, we set isPublic true to make the loop keep going up the hierarchy. The class/interface decides
                    For the latter one, we correctly is setting isPublic as true
                     */
                    noVisibilityIssueFaced = true;
                }
            }


            if(curNode.getParent() instanceof BodyDeclaration) {
                // BodyDeclaration includes: Methods, class, interface, ...
                // See: https://help.eclipse.org/mars/topic/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/dom/BodyDeclaration.html
                curNode = (BodyDeclaration) (curNode.getParent());
            }
            else  if(curNode.getParent() instanceof AnonymousClassDeclaration ||  curNode.getParent() instanceof  TypeDeclarationStatement)
            {
                curNode=null;
                isPublic=false;
            }
            else if(curNode.getParent() instanceof CompilationUnit) {
                curNode = null;
            }
            else
            {
                System.err.println("********************  "+curNode.getParent().toString()+"   **********************");
                curNode=null;
                isPublic=false;
            }
        }
        return isPublic;
    }

    private String getQualifiedClassName(IMethodBinding methodDeclaration) {
        ITypeBinding declaringClass = methodDeclaration.getDeclaringClass();

        /*
        Note (From documentation of ITypeBinding::isNested):
        ITypeBinding::isNested -> A nested type is any type whose declaration occurs within the body of another.
                                  Nested types further subdivide into:
                                  - Member types: ITypeBinding::isMember
                                  - Local types: ITypeBinding::isLocal
                                  - Anonymous types: ITypeBinding::isAnonymous
         */

        String qualifiedClassName = declaringClass.getQualifiedName();

        if((declaringClass.isLocal() || declaringClass.isAnonymous()) && qualifiedClassName.isEmpty()==false)
            System.err.println("Wat?! Local/Anonymous declaring class but the qualifiedClassName!=empty ?!?!!?");

        //declaringClass.isAnonymous()/isLocal() ==> Not really accessible and declaringClass.getQualifiedName()=""
        if(declaringClass.isAnonymous() || declaringClass.isLocal())
            return null;

        // There are cases when the declaringClass is not local/anonymous, but still no "qualifiedClassName is empty.
        // I took a look at 194 cases and actually they are a class defined within a anonymous class. So it's safe
        // if we ignore such cases.
        if(qualifiedClassName.isEmpty())
            return null;

        return qualifiedClassName;
    }
}
