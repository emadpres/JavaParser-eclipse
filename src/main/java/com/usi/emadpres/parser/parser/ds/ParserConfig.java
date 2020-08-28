package com.usi.emadpres.parser.parser.ds;

public class ParserConfig {
    public boolean parseMethodInvocations;
    public boolean parseMethodDeclarations;
    public boolean parsePackagesAndUserDefinedTypes;
    public boolean storePathsAsAbsolute; // This is useful when parser analyze a single java file

    public ParserConfig(boolean parseMethodInvocations, boolean parseMethodDeclarations, boolean parsePackagesAndUserDefinedTypes) {
        this.parseMethodInvocations = parseMethodInvocations;
        this.parseMethodDeclarations = parseMethodDeclarations;
        this.parsePackagesAndUserDefinedTypes = parsePackagesAndUserDefinedTypes;
        this.storePathsAsAbsolute=false;
    }

    public ParserConfig(boolean parseMethodInvocations, boolean parseMethodDeclarations, boolean parsePackagesAndUserDefinedTypes, boolean storePathsAsAbsolute) {
        this.parseMethodInvocations = parseMethodInvocations;
        this.parseMethodDeclarations = parseMethodDeclarations;
        this.parsePackagesAndUserDefinedTypes = parsePackagesAndUserDefinedTypes;
        this.storePathsAsAbsolute=storePathsAsAbsolute;
    }
}
