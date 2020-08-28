package com.usi.emadpres.parser.parser.helper;

import com.usi.emadpres.MavenUtils.ds.MavenLibInfo;
import com.usi.emadpres.parser.parser.ds.MethodInvocationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Given a {@code List<MethodInvocationInfo>}, for non-Local/Java calls, it tries to find the original library
 * Read {@link #Resolve} for more details and inputs
 */
public class ResolveExternalMethodInvocations {
    private static final Logger logger = LoggerFactory.getLogger(ResolveExternalMethodInvocations.class);

    // TODO: What if several versions if a library exists in the dependency list for this project
    // TODO: What if we have a different version of library in "projectDependencies_extractedClasses" object

    /**
     * Given a {@code List<MethodInvocationInfo>}, for non-Local/Java calls, it tries to find the original library
     * @param methodInvocations                             List of method invocations
     * @param projectDependencies                           List of Maven dependencies
     * @param projectDependencies_extractedClasses_byLib    Map from Maven dependencies (= libs) to all classes defined in that dependency (extracted from its jar file)
     *                                                      This data can be prepared using {@link com.usi.emadpres.MavenUtils.helper.JarExtractedClassAndPackageCSVReader#Load_LibNameAsKey}
     * @param projectDependencies_extractedClasses_byClass  (optional) Needed if linkToNonDependencyIfNeeded=true
     *                                                      This data can be prepared using {@link com.usi.emadpres.MavenUtils.helper.JarExtractedClassAndPackageCSVReader#Load_DataAsKey}
     * @param linkToNonDependencyIfNeeded                   Should we consider libraries not listed as client project's
     *                                                      dependencies as original library of a non-Local/Java method
     *                                                      call, if no match found within dependency libraries? [Shit happens]
     * @param linkToLibraryAsReference                      Whether {@link MethodInvocationInfo#originalLibrary} filled up with a reference from {@code projectDependencies} or with a new objects.
     */
    public static void Resolve(List<MethodInvocationInfo> methodInvocations,
                               List<MavenLibInfo> projectDependencies,
                               Map<String, Set<String>> projectDependencies_extractedClasses_byLib,
                               Map<String, Set<String>> projectDependencies_extractedClasses_byClass,
                               boolean linkToNonDependencyIfNeeded,
                               boolean linkToLibraryAsReference,
                               boolean printReport) {

        if(projectDependencies==null) {
            logger.error("Skipping to resolve external method -> No dependency provided!");
            return;
        }
        if(projectDependencies_extractedClasses_byLib==null || ( linkToNonDependencyIfNeeded && projectDependencies_extractedClasses_byClass==null)) {
            logger.error("Skipping to resolve external method -> Extracted class maps are null");
            return;
        }

        int total=0, total_nonLocal_nonJava=0;
        int linked=0, linked_byNonDependency=0;
        int notLinked=0;
        int notLinked__dueToMultipleLibraryMatchWithinDependencies=0;
        int notLinked__dueToMultipleVersionsMatchWithinDependencies=0;
        int notLinked_butJavaxOrSun=0;

        for (MethodInvocationInfo method : methodInvocations) {
            total++;
            if(method.isLocalInvocation || method.isJavaInvocation) continue;
            total_nonLocal_nonJava++;
            method.originalLibrary=null; // needed. Consider case where we ran this process wrong. Now we read data from db (so .originalLibrary has some value) but no longer will get a match

            boolean libraryFound = false;
            boolean multipleLibraryMatched =false, multipleLibraryDifferOnlyInVersion=true;
            MavenLibInfo foundLibrary = null;

            // How this code works?
            // For each method call, check if any dependency contain a
            // class equals to this method call qualifiedClassName
            for(MavenLibInfo dep: projectDependencies)
            {
                Set<String> c = projectDependencies_extractedClasses_byLib.get(dep.GetIdentifier());
                if(c==null) continue;
                if(c.contains(method.qualifiedClassName))
                {
                    if(!libraryFound)
                    {
                        libraryFound = true;
                        foundLibrary = dep;
                    }
                    else
                    {
                        if(foundLibrary.equals(dep)==false) {
                            multipleLibraryMatched = true;
                            if (foundLibrary.groupId.equals(dep.groupId) == false || foundLibrary.artifactId.equals(dep.artifactId) == false) {
                                multipleLibraryDifferOnlyInVersion = false;
                            }
                        }
                    }
                }
            }

            if(libraryFound )
            {
                if(multipleLibraryMatched==false) {
                    linked++;
                    if (linkToLibraryAsReference)
                        method.originalLibrary = foundLibrary;
                    else
                        method.originalLibrary = new MavenLibInfo(foundLibrary.groupId, foundLibrary.artifactId, foundLibrary.version);
                }
                else
                {
                    if(multipleLibraryDifferOnlyInVersion)
                        notLinked__dueToMultipleVersionsMatchWithinDependencies++;
                    else
                        notLinked__dueToMultipleLibraryMatchWithinDependencies++;
                }
            }

            if(libraryFound == false && linkToNonDependencyIfNeeded==true && projectDependencies_extractedClasses_byClass != null) {
                Set<String> s = projectDependencies_extractedClasses_byClass.get(method.qualifiedClassName);
                if (s != null && s.size() > 0) {
                    String firstLib = s.iterator().next(); // Randomly assign the first match
                    String[] split = firstLib.split(":");
                    method.originalLibrary = new MavenLibInfo(split[0], split[1], split[2]);
                    linked_byNonDependency++;
                    libraryFound = true;
                }
            }

            if(libraryFound==false) {
                //logger.info("No library matches for {} {} {}", method.qualifiedClassName, method.name, method.fileRelativePath);
                notLinked++;
                if(method.qualifiedClassName.startsWith("javax.") ||method.qualifiedClassName.startsWith("sun."))
                    notLinked_butJavaxOrSun++;
            }

        }
        if(printReport) {
            logger.info("Method invocations: non-Local/Java: {}  (out of Total={})", total_nonLocal_nonJava, total);
            logger.info("                            Linked: {}", linked);
            logger.info("                        Not-Linked: {} (JavaX/Sun: {})", notLinked, notLinked_butJavaxOrSun);
            logger.info("                        Not-Linked: {} (because multiple version Matched)", notLinked__dueToMultipleVersionsMatchWithinDependencies);
            logger.info("                        Not-Linked: {} (because multiple Library Matched)", notLinked__dueToMultipleLibraryMatchWithinDependencies);
            logger.info("     (to non-dependencies) Linked : {}", linkToNonDependencyIfNeeded ? linked_byNonDependency : "[DISABLED IN CODE]");
        }
    }
}
