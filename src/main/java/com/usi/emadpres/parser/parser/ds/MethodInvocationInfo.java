package com.usi.emadpres.parser.parser.ds;

import com.usi.emadpres.MavenUtils.ds.MavenLibInfo;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.ArrayList;
import java.util.List;

public class MethodInvocationInfo {

    // Method -> Common
    public int databaseId =-1; // Useful when we want to load from db and store them with exact same id for consistency with other tables/files
    public int methodDeclarationId =-1; // Useful when we want to interconnect MethodInvocationInfo to MethodDeclarationInfo
    public String projectName; // repoFullName OR groupId:artifactId:version
    public String commitSHA=null;
    public String fileRelativePath; // relative to the project's root
    //////////////////////////////////////////////
    public String qualifiedClassName; //In reality, this could be either of: class, interface, enum, annotationType.
    public String name, returnType, argsTypes;
    public int nArgs;
    //////////////////////////////////////////////
    public int lineNumbersCount; // Alternative to `lineNumbers` when we don't care exact line numbers, but count matters.
    public List<Integer> lineNumbers = new ArrayList<>();
    public boolean isLocalInvocation = false, isJavaInvocation=false;
    public MavenLibInfo originalLibrary=null; // if external call

    // "Remark" field are useful to store project-specific information (project = PhD project)
    public int remark=-1;
    public String remark_str=null;

    public MethodInvocationInfo(String _qualifiedClassName, String _returnType, String _name, int _nArgs, String _argsType)
    {
        this.qualifiedClassName = _qualifiedClassName;
        this.returnType = _returnType;
        this.name = _name;
        this.nArgs = _nArgs;
        this.argsTypes = _argsType;
    }

    @Override
    public String toString() {
        int t = qualifiedClassName.lastIndexOf('.');
//        if(t==-1) t=-1;
        return String.format("[%s]    %s",qualifiedClassName.substring(t+1), name);
    }

    /**
     * Note that we only consider {@link MethodInvocationInfo#name}, {@link MethodInvocationInfo#qualifiedClassName}, {@link MethodInvocationInfo#returnType} and {@link MethodInvocationInfo#argsTypes}
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
