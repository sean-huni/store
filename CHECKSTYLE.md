# Checkstyle Configuration Guide

## Overview

This project has been configured with Checkstyle to enforce consistent Java coding standards and improve code quality.

## Configuration Details

### Gradle Plugin Configuration

The Checkstyle plugin has been added to `build.gradle`:

```gradle
plugins {
    // ... other plugins
    id 'checkstyle'
}

// Version configuration
ext {
    checkstyleVersion = '10.17.0'
}

// Checkstyle configuration
checkstyle {
    toolVersion = checkstyleVersion
    configFile = file("${project.rootDir}/config/checkstyle/checkstyle.xml")
    ignoreFailures = false
    maxWarnings = 0
    maxErrors = 0
}
```

### Configuration File

The Checkstyle rules are defined in `config/checkstyle/checkstyle.xml` and include:

- **Naming Conventions**: Proper naming for classes, methods, variables, and constants
- **Import Checks**: Avoiding star imports, removing unused imports, preventing illegal imports
- **Size Violations**: Method length and parameter number limits
- **Whitespace**: Consistent spacing and formatting
- **Modifier Checks**: Proper modifier order and redundancy checks
- **Block Checks**: Proper brace usage and block structure
- **Coding Problems**: Common coding issues like empty statements, equals/hashCode consistency
- **Class Design**: Interface design, utility class patterns, visibility modifiers
- **Miscellaneous**: Array type style, final parameters, TODO comments

## Available Gradle Tasks

### Main Tasks

- `./gradlew checkstyleMain` - Run Checkstyle analysis for main source code
- `./gradlew checkstyleTest` - Run Checkstyle analysis for test source code
- `./gradlew checkstyleAot` - Run Checkstyle analysis for AOT classes
- `./gradlew checkstyleAotTest` - Run Checkstyle analysis for AOT test classes

### Running All Checkstyle Tasks

```bash
./gradlew check
```

This will run all verification tasks including Checkstyle, tests, and other quality checks.

## Reports

Checkstyle generates both XML and HTML reports:

- **HTML Report**: `build/reports/checkstyle/main.html` (human-readable)
- **XML Report**: `build/reports/checkstyle/main.xml` (for CI/CD integration)

## Configuration Behavior

- **Strict Mode**: The configuration is set to fail the build on any warnings or errors
- **Zero Tolerance**: `maxWarnings = 0` and `maxErrors = 0` ensure all violations must be fixed
- **Line Length**: Maximum line length is set to 120 characters
- **File Extensions**: Checks Java, properties, and XML files

## Common Violations and Solutions

### 1. Design for Extension

**Issue**: Classes designed for extension need proper documentation
**Solution**: Add final modifier to classes not meant for extension or add proper JavaDoc

### 2. Final Parameters

**Issue**: Method parameters should be final
**Solution**: Add `final` keyword to method parameters

### 3. Magic Numbers

**Issue**: Numeric literals in code
**Solution**: Extract numbers to named constants

### 4. Line Length

**Issue**: Lines longer than 120 characters
**Solution**: Break long lines into multiple lines

### 5. Hidden Fields

**Issue**: Parameter names hide instance fields
**Solution**: Use different parameter names or use `this.field = parameter`

## Integration with CI/CD

The Checkstyle configuration can be integrated into CI/CD pipelines:

```yaml
# Example GitHub Actions step
- name: Run Checkstyle
  run: ./gradlew checkstyleMain checkstyleTest
```

## Suppressing Violations (When Necessary)

For cases where violations cannot be avoided, you can use suppression files:

- Create `config/checkstyle/checkstyle-suppressions.xml`
- Configure specific suppression rules

## IDE Integration

### IntelliJ IDEA

1. Install the Checkstyle plugin
2. Configure it to use the project's checkstyle.xml file
3. Enable real-time checking

### Eclipse

1. Install the Checkstyle plugin
2. Import the checkstyle.xml configuration
3. Enable project-specific settings

## Best Practices

1. **Run Checkstyle Early**: Check your code before committing
2. **Fix Violations Promptly**: Don't accumulate technical debt
3. **Understand Rules**: Learn why rules exist rather than just fixing violations
4. **Team Consistency**: Ensure all team members use the same configuration
5. **Regular Updates**: Keep Checkstyle version updated for latest checks

## Troubleshooting

### Build Failures

If the build fails due to Checkstyle violations:

1. Check the HTML report for detailed violation descriptions
2. Fix violations one by one
3. Re-run the Checkstyle task to verify fixes

### Performance Issues

If Checkstyle slows down the build:

1. Consider excluding generated files
2. Run Checkstyle only on changed files in CI
3. Use parallel execution where possible

## Configuration Customization

To modify rules, edit `config/checkstyle/checkstyle.xml`:

- Add new modules for additional checks
- Modify severity levels (error, warning, info)
- Configure rule-specific properties
- Add file exclusions if needed

Remember to test configuration changes thoroughly before applying to the entire codebase.