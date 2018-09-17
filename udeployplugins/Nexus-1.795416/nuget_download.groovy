/*
 * Licensed Materials - Property of IBM Corp.
 * IBM UrbanCode Deploy
 * (c) Copyright IBM Corporation 2016. All Rights Reserved.
 *
 * U.S. Government Users Restricted Rights - Use, duplication or disclosure restricted by
 * GSA ADP Schedule Contract with IBM Corp.
 */

import groovy.json.JsonException

import com.urbancode.air.AirPluginTool
import com.urbancode.air.plugin.nexus.NexusHelper
import com.urbancode.commons.httpcomponentsutil.HttpClientBuilder
import com.urbancode.commons.util.IO

final def REST_ENDPOINT = 'service/local'
final def workDir = new File('.').canonicalFile

// Get step properties
def apTool = new AirPluginTool(this.args[0], this.args[1])
def inProps = apTool.getStepProperties()
def nexusUrl  = inProps['nexusUrl']
def packages  = inProps['packages'].split('\n')
def checkHash = inProps['checkHash']
def username  = inProps['username']
def password  = inProps['password']

// Sanitize inputs
while (nexusUrl.endsWith('/')) {
    nexusUrl = nexusUrl.substring(0, nexusUrl.length() - 1)
}

// Build Client
if (username) {
    NexusHelper.createClient(username, password)
}
else {
    NexusHelper.createClient()
}

// Loop through packages
int successfulDownloads = 0
for (int i = 0; i < packages.length; i++) {
    println ("\n---- Working on package ${i + 1}/${packages.length} -----")

    // Get Package Info
    String[] packageInfo = packages[i].split('/')
    String packageRepo = ''
    String packageId = ''
    String packageVersion = ''
    try {
        packageRepo = packageInfo[0]
        packageId = packageInfo[1]
        packageVersion = packageInfo[2]
    }
    catch (IndexOutOfBoundsException obe) {
        println ('[error]  Could not get package information.')
        println ('[possible solution] Please update the entry to be in the form of "repositoryName/packageId/packageVersion" ' +
                 "for line: ${packages[i].toString()}")
        continue
    }

    // Get Repo Id
    String repoId = ''
    try {
        String repoUrl = "${nexusUrl}/${REST_ENDPOINT}/all_repositories"
        repoId = NexusHelper.getRepoId(repoUrl, packageRepo)
    }
    catch (IOException ioe) {
        println ("[error]  Could not determine repository id: ${ioe.getClass().getSimpleName()} - ${ioe.getMessage()}")
    }
    catch (JsonException jse) {
        println ("[error]  Could not determind repository id: ${jse.getClass.getSimpleName()} - ${jse.getMessage()}")
    }

    if (repoId != '') {
        println ('[ok]  Repository id found')

        // Download Package
        String packageLocation = "${packageId}/${packageVersion}/${packageId}-${packageVersion}.nupkg"
        String downloadUrl = "${nexusUrl}/${REST_ENDPOINT}/repositories/${repoId}/content/${packageLocation}"
        File tempFile = null
        try {
            tempFile = NexusHelper.downloadNugetFile(downloadUrl)
        }
        catch (IOException ioe) {
            println ("[error]  Could not download package: ${ioe.getClass().getSimpleName()} - ${ioe.getMessage()}")
        }
        catch (SecurityException se) {
            println ("[error]  Could not download package: ${se.getClass().getSimpleName()} - ${se.getMessage()}")
        }

        if (tempFile != null) {
            println ('[ok]  Downloaded package from repository')

            // Move to correct directory
            File finalFile = new File(workDir, "${packageId}-${packageVersion}.nupkg")
            try {
                IO.move(tempFile, finalFile)
                successfulDownloads++
                println ('[ok]  Moved downloaded package to the working directory')
            }
            catch (IOException ioe) {
                println ("[error]  Could not move downloaded file to requested directory: ${ioe.getMessage()}")
            }
            try {
                tempFile.delete()
                println ('[ok]  Deleted temporary file')
            }
            catch (IOException ioe) {
                println ("[error]  Could not delete temporary file: ${ioe.getMessage()}")
            }
        }
        else {
            println ('[error]  Could not download the package.')
        }
    }
    else {
        println ('[error]  Could not determine repository id, cannot proceed to download package.')
    }
}

println ("\n\nSummary: ${successfulDownloads}/${packages.length} packages successfully downloaded")
if (successfulDownloads == packages.length) {
    System.exit(0)
}
else {
    System.exit(1)
}