#!/usr/bin/env groovy
/*
 * Licensed Materials - Property of IBM Corp.
 * IBM UrbanCode Build
 * IBM UrbanCode Deploy
 * IBM UrbanCode Release
 * IBM AnthillPro
 * (c) Copyright IBM Corporation 2002, 2013. All Rights Reserved.
 *
 * U.S. Government Users Restricted Rights - Use, duplication or disclosure restricted by
 * GSA ADP Schedule Contract with IBM Corp.
 */

import com.urbancode.air.*

final def apTool = new AirPluginTool(this.args[0], this.args[1]);
final def props = apTool.getStepProperties()
final def workDir = new File('.').canonicalFile

final def directoryOffset = props['directoryOffset']
final def antFileName = props['antFileName']
final def targetNames = props['targetNames']
final def antProperties = props['antProperties']
final def properties = props['properties']
final def scriptContent = props['scriptContent']
final def antOpts = props['antOpts']
final def ANT_HOME = props['antHome']?.trim()
final def JAVA_HOME = props['javaHome']

//
// Validation
//
if (directoryOffset) {
    workDir = new File(workDir, directoryOffset).canonicalFile
}

if (workDir.isFile()) {
    throw new IllegalArgumentException("Working directory ${workDir} is a file!")
}

if (antFileName == null) {
    throw new IllegalStateException("Ant Script File not specified.");
}

if (ANT_HOME == null) {
    throw new IllegalStateException("ANT_HOME not specified");
}

if (JAVA_HOME == null) {
    throw new IllegalStateException("JAVA_HOME not specified");
}

//
// Create workDir and antFile
//

// ensure work-dir exists
workDir.mkdirs()

final def antFile = new File(workDir, antFileName)

// check if script content was specified, if so, we need to write out the file
boolean deleteOnExit = false
if (scriptContent) {
    antFile.text = scriptContent
    deleteOnExit = true
}

int exitCode = -1;
try {
    def antExe = new File(ANT_HOME, "bin/ant" + (apTool.isWindows ? ".bat" : ""))

    //
    // Build Command Line
    //
    def commandLine = []
    commandLine.add(antExe.absolutePath)

    if (antProperties) {
        antProperties.readLines().each() { antProperty ->
            if (antProperty) {
                commandLine.add(antProperty)
            }
        }
    }

    commandLine.add("-f")
    commandLine.add(antFile.absolutePath)

    if (properties) {
        properties.split("[\\r\\n]+").each() { property ->
            if (property) {
                if (!property.startsWith("-D")) {
                    property = "-D" + property
                }
                commandLine.add(property)
            }
        }
    }

    if (targetNames) {
        targetNames.split("\\s+").each() { targetName ->
            if (targetName) {
                commandLine.add(targetName)
            }
        }
    }

    CommandHelper cmdHelper = new CommandHelper(workDir)
    cmdHelper.addEnvironmentVariable("ANT_HOME", ANT_HOME)
    cmdHelper.addEnvironmentVariable("JAVA_HOME", JAVA_HOME)
    cmdHelper.addEnvironmentVariable("JAVACMD", "") // ant.bat uses this over the JAVA_HOME
    if (antOpts) {
        def parsedAntOptions = antOpts.readLines().join(' ')
        cmdHelper.addEnvironmentVariable("ANT_OPTS", parsedAntOptions)
    }

    exitCode = cmdHelper.runCommand("Building project", commandLine)
}
finally {
    if (deleteOnExit) {
        antFile.delete()
    }
}
System.exit(exitCode);
