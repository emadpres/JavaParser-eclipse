package com.usi.emadpres.parser.parser.ds;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class MethodDeclarationInfo {

    // Method -> Common
    public int databaseId =-1; // Useful when we want to load from db and store them with exact same id for consistency with other tables/files
    public String projectName; // repoFullName OR groupId:artifactId:version
    public String commitSHA=null;
    public String fileRelativePath; // relative to the project's root
    public String qualifiedClassName;
    public String name, returnType, argsTypes;
    public int nArgs;

    // Method -> Declarations
    public boolean hasJavaDoc = false;
    public int javaDocStartLine=-1, javaDocEndLine=-1;
    public String javaDoc=null;
    public int isPublic=-1; //1:public 0:nonPublic //TODO: make this boolean
    public boolean isConstructor;
    public boolean isDeprecated=false;
    public boolean hasBody = false;
    public String signatureCode_generated = null;
    public String declarationCode_generated =null; //includes javadoc+signature+body
    public int lineStart, lineEnd, signatureLine;

    // "Remark" field are useful to store project-specific information (project = PhD project)
    public int remark=-1;
    public String remark_str=null;

    public MethodDeclarationInfo(String _qualifiedClassName, String _returnType, String _name, int _nArgs, String _argsType, int _lineNumStart, int _lineNumEnd, int _signatureLine)
    {
        this.qualifiedClassName = _qualifiedClassName;
        this.returnType = _returnType;
        this.name = _name;
        this.nArgs = _nArgs;
        this.argsTypes = _argsType;
        this.lineStart = _lineNumStart;
        this.lineEnd = _lineNumEnd;
        this.signatureLine = _signatureLine;
    }

    @Override
    public String toString() {
        int t = qualifiedClassName.lastIndexOf('.');
        if(t==-1) t=-1;
        return String.format("[%s]    %s",qualifiedClassName.substring(t+1), name);
        //return String.format("@Method Declaration: %s %s..%s (%s) <%d>\n", returnType, qualifiedClassName, name, argsTypes, nArgs);
    }

    public String ToSignatureString() {
        int t = qualifiedClassName.lastIndexOf('.');
        return String.format("[%s] %s (%s) -> %s",qualifiedClassName, name, argsTypes, returnType);
    }

    /**
     * Note that we only consider {@link MethodDeclarationInfo#name}, {@link MethodDeclarationInfo#qualifiedClassName}, {@link MethodDeclarationInfo#returnType} and {@link MethodDeclarationInfo#argsTypes}
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MethodInvocationInfo)) {
            return false;
        }
        MethodInvocationInfo other = (MethodInvocationInfo) obj;
        EqualsBuilder builder = new EqualsBuilder();
        builder.append(name, other.name);
        builder.append(qualifiedClassName, other.qualifiedClassName);
        builder.append(returnType, other.returnType);
        builder.append(argsTypes, other.argsTypes);
        return builder.isEquals();
    }


    @Override
    public int hashCode() {
        HashCodeBuilder builder = new HashCodeBuilder();
        builder.append(qualifiedClassName);
        builder.append(returnType);
        builder.append(name);
        builder.append(argsTypes);
        return builder.hashCode();
    }
}
