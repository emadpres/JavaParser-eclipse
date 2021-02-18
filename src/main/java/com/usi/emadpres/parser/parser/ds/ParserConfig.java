package com.usi.emadpres.parser.parser.ds;

public class ParserConfig {
    public boolean parseMethodInvocations;
    public boolean parseMethodDeclarations;
    public boolean parsePackagesAndUserDefinedTypes;
    public boolean storePathsAsAbsolute=false; // This is useful when parser analyze a single java file

    public boolean storeJavaFileContent=false;

    @Deprecated
    public ParserConfig(boolean parseMethodInvocations, boolean parseMethodDeclarations, boolean parsePackagesAndUserDefinedTypes) {
        this.parseMethodInvocations = parseMethodInvocations;
        this.parseMethodDeclarations = parseMethodDeclarations;
        this.parsePackagesAndUserDefinedTypes = parsePackagesAndUserDefinedTypes;
    }

    public ParserConfig(boolean parseMethodInvocations, boolean parseMethodDeclarations, boolean parsePackagesAndUserDefinedTypes, boolean storePathsAsAbsolute) {
        this.parseMethodInvocations = parseMethodInvocations;
        this.parseMethodDeclarations = parseMethodDeclarations;
        this.parsePackagesAndUserDefinedTypes = parsePackagesAndUserDefinedTypes;
        this.storePathsAsAbsolute=storePathsAsAbsolute;
    }
    public ParserConfig(boolean parseMethodInvocations, boolean parseMethodDeclarations, boolean parsePackagesAndUserDefinedTypes, boolean storeJavaFileContent, boolean storePathsAsAbsolute) {
        this.parseMethodInvocations = parseMethodInvocations;
        this.parseMethodDeclarations = parseMethodDeclarations;
        this.parsePackagesAndUserDefinedTypes = parsePackagesAndUserDefinedTypes;
        this.storeJavaFileContent = storeJavaFileContent;
        this.storePathsAsAbsolute=storePathsAsAbsolute;
    }
}
