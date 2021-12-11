# How parse a project with my code?

1. Parse java files: `EclipseJavaParser.ParseProjectFiles(..)`
   - With `ParserConfig` specifies what information to be extracted
2. For method invocations, determine local method calls: `ResolveLocalMethodInvocations.Resolve(..)`
3. For non-Local/Java calls, determine libraries to which each API call belong: `ResolveExternalMethodInvocations.Resolve(..)`
4. Merge parsing results: `ProjectParsingResultDB.Merge`
5. Store results: `ProjectParsingResultDB.WriteToSQLite` 
