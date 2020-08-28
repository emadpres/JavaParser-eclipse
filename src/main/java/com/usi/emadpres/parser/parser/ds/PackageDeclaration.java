package com.usi.emadpres.parser.parser.ds;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class PackageDeclaration {
    public String projectName; // repoFullName OR groupId:artifactId:version
    public String commitSHA = null;
    public String dirRelativePath;
    public String fullyQualifiedPackageName;

    /**
     * @param dirRelativePath Since all files within same dir has the same package, we store directory instead if file path.
     */
    public PackageDeclaration(String projectName, String dirRelativePath, String fullyQualifiedPackageName)
    {
        this.projectName = projectName;
        this.dirRelativePath = dirRelativePath;
        this.fullyQualifiedPackageName = fullyQualifiedPackageName;
    }

    @Override
    public boolean equals(Object obj) {

        if (!(obj instanceof PackageDeclaration)) {
            return false;
        }
        PackageDeclaration otherAdmin = (PackageDeclaration) obj;
        EqualsBuilder builder = new EqualsBuilder();
        builder.append(projectName, otherAdmin.projectName);
        builder.append(fullyQualifiedPackageName, otherAdmin.fullyQualifiedPackageName);
        builder.append(dirRelativePath, otherAdmin.dirRelativePath);
        return builder.isEquals();

    }

    @Override
    public int hashCode() {
        HashCodeBuilder builder = new HashCodeBuilder();
        builder.append(projectName);
        builder.append(fullyQualifiedPackageName);
        builder.append(dirRelativePath);
        return builder.hashCode();
    }

    @Override
    public String toString() {
        return "package "+ fullyQualifiedPackageName;
    }
}
