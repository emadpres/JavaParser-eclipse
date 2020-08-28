package com.usi.emadpres.parser.parser.ds;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class UserTypeDeclaration {
    public String projectName; // repoFullName OR groupId:artifactId:version
    public String commitSHA;
    public String fullyQualifiedName;
    public String fileRelativePath; // relative to the project's root

    public UserTypeDeclaration_Kind typeDeclType;
    public boolean isBinded;

    public UserTypeDeclaration(String projectName, String commitSHA, String fileRelativePath, String fullyQualifiedName, UserTypeDeclaration_Kind tdt, boolean isBinded)
    {
        this.projectName = projectName;
        this.commitSHA = commitSHA;
        this.fileRelativePath = fileRelativePath;

        this.fullyQualifiedName = fullyQualifiedName;
        this.typeDeclType = tdt;
        this.isBinded = isBinded;
    }

    public enum UserTypeDeclaration_Kind {
        CLASS(1),
        INTERFACE(2),
        ENUM(3),
        ANNOTATION(4);

        private final int ordinalValue;
        UserTypeDeclaration_Kind(int ordinalValue) { this.ordinalValue = ordinalValue; }
    }

    @Override
    public boolean equals(Object obj) {

        if (!(obj instanceof UserTypeDeclaration)) {
            return false;
        }
        UserTypeDeclaration other = (UserTypeDeclaration) obj;
        EqualsBuilder builder = new EqualsBuilder();
        builder.append(projectName, other.fullyQualifiedName);
        builder.append(fullyQualifiedName, other.fullyQualifiedName);
        builder.append(fileRelativePath, other.fileRelativePath);
        builder.append(isBinded, other.isBinded);
        builder.append(typeDeclType, other.typeDeclType);
        return builder.isEquals();

    }

    @Override
    public int hashCode() {
        HashCodeBuilder builder = new HashCodeBuilder();
        builder.append(projectName);
        builder.append(fullyQualifiedName);
        builder.append(fileRelativePath);
        builder.append(isBinded);
        builder.append(typeDeclType);
        return builder.hashCode();
    }

    @Override
    public String toString() {
        switch (typeDeclType)
        {
            case CLASS:
                return "class "+fullyQualifiedName;
            case INTERFACE:
                return "interface "+fullyQualifiedName;
            case ENUM:
                return "enum "+fullyQualifiedName;
            case ANNOTATION:
                return "annotation "+fullyQualifiedName;
        }
        return "ERROR";
    }
}
