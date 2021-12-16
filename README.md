## Notes
To export the project as a .jar library to be used by other projects, make sure to uncomment snippet below in `pom.xml`:
```xml
<excludes>
    <exclude>**/log4j.properties</exclude>
</excludes>
```