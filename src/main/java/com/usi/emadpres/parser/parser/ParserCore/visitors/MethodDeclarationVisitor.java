package com.usi.emadpres.parser.parser.ParserCore.visitors;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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


    /**
     * Method or Constructor declaration.
     */
    @Override
	public boolean visit(MethodDeclaration node) {
		IMethodBinding binding = node.resolveBinding();
        extractMethodDeclaration(node, binding);
		return true;
	}


	public void extractMethodDeclaration(MethodDeclaration node, IMethodBinding binding)
    {
        if(binding==null) {
            //logger.warn("Method declaration binding is null: {}", node);
            return;
        }


        //////////// Signature
        String methodName = binding.getName(); // Probably equivalent to node.getName().getIdentifier();

        String qualifiedClassName = getQualifiedClassName(binding);
        if (qualifiedClassName == null)
            return; // We discard

        // For constructors, it replaces method name with <init>
        // It adds implicit modifiers like "public" or "abstract"
        // signature code is generated out of internal representations
        String signatureCode_generated = binding.toString();
        int signatureLineNumber = unit.getLineNumber(node.getName().getStartPosition()); //in fact, method name line number
        String returnType_fullyQualified = binding.getReturnType().getQualifiedName();



        //List parameters = node.parameters();
        int nArgs = binding.getParameterTypes().length;
        String argsTypes = "";
        for(int i=0; i<nArgs; i++)
        {
            argsTypes += BindingUtil.qualifiedName(binding.getParameterTypes()[i]);
            if(i!=nArgs-1)
                argsTypes += ", ";
        }


        ///////////// Everything (Javadoc+Signature+Body)
        int declarationLineStart = unit.getLineNumber(node.getStartPosition());
        int declarationLineEnd = unit.getLineNumber(node.getStartPosition()+node.getLength());

        // code is generated out of internal representations
        String declarationCode_generated =  node.toString(); //includes javadoc+signature+body

        ///////////// Java Doc
        Javadoc javadoc_block = node.getJavadoc();
        int javadocLineStart=-1;
        int javadocLineEnd =-1;
        String javadoc_text = "";
        if(javadoc_block!=null)
        {
            javadocLineStart = unit.getLineNumber(javadoc_block.getStartPosition());
            javadocLineEnd = unit.getLineNumber(javadoc_block.getStartPosition() + javadoc_block.getLength());
            javadoc_text = javadoc_block.toString();
        }

        ///////////// Method Body - from opening { until }
        Block methodBodyBlock = node.getBody();
        boolean hasBody = false;
        String bodyCode_generated = ""; // Body doesn't contain "signature" and starts from "{".
        int bodyLineStart = -1;
        int bodyLineEnd = -1;
        if(methodBodyBlock!=null) {
            hasBody = true;
            bodyCode_generated = methodBodyBlock.toString();
            bodyLineStart = unit.getLineNumber(methodBodyBlock.getStartPosition()); // Could be the same as signature line since it look up "{" line
            bodyLineEnd = unit.getLineNumber(methodBodyBlock.getStartPosition() + methodBodyBlock.getLength());
        }
        else
        {
            // It is a method signature only (like an interface methods)
        }


        ///////////// Other Info
        boolean isConstructor = binding.isConstructor(); // Probably equivalent to: node.isConstructor();
        boolean isDeprecated =  binding.isDeprecated();
        //IAnnotationBinding[] annotations = binding.getAnnotations();
        //List modifiers = node.modifiers(); // public, static, etc. // Doesn't include implicit modifiers (if "public" is not mentioned, this list doesn't include it)
        //List thrownExceptions = node.thrownExceptionTypes();


        MethodDeclarationInfo info = new MethodDeclarationInfo(qualifiedClassName, returnType_fullyQualified, methodName, nArgs, argsTypes, declarationLineStart, declarationLineEnd, signatureLineNumber);
        info.projectName = this.projectName;
        info.commitSHA = this.commitSHA;
        info.fileRelativePath = this.fileRelativePath;
        info.isPublic = isDeclarationPublic(node)==true?1:0;
        info.isConstructor = isConstructor;
        info.hasBody = hasBody;
        info.signatureCode_generated = signatureCode_generated;
        info.declarationCode_generated = declarationCode_generated;
        info.isDeprecated = isDeprecated;


        if(javadoc_block !=null) {
            info.hasJavaDoc = true;
            info.javaDoc = javadoc_text;
            info.javaDocStartLine = javadocLineStart;
            info.javaDocEndLine = javadocLineEnd;
        }

        methodDeclarations_thisUnit.add(info);
    }

    private boolean isDeclarationPublic(BodyDeclaration node) {
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
