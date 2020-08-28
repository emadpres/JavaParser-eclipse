# How parse a project with my code?

1. Parse java files: `EclipseJavaParser.ParseProjectFiles(..)`
   - With `ParserConfig` specifies what information to be extracted
2. Determine local method calls: `ResolveLocalMethodInvocations.Resolve(..)`
3. Determine libraries which API calls belong to: `ResolveExternalMethodInvocations.Resolve(..)`
