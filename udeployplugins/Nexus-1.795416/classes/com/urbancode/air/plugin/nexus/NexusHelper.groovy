/*
 * Licensed Materials - Property of IBM Corp.
 * IBM UrbanCode Deploy
 * (c) Copyright IBM Corporation 2016. All Rights Reserved.
 *
 * U.S. Government Users Restricted Rights - Use, duplication or disclosure restricted by
 * GSA ADP Schedule Contract with IBM Corp.
 */

package com.urbancode.air.plugin.nexus

import groovy.json.JsonSlurper

import java.security.NoSuchAlgorithmException

import org.apache.commons.lang.ObjectUtils
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.utils.URIBuilder
import org.apache.http.HttpResponse
import org.apache.http.HttpStatus
import org.apache.http.util.EntityUtils

import com.urbancode.commons.httpcomponentsutil.HttpClientBuilder
import com.urbancode.commons.fileutils.digest.DigestUtil
import com.urbancode.commons.fileutils.FileUtils


public class NexusHelper {
    private static final HttpClientBuilder builder = new HttpClientBuilder()
    private static def client

    /**
    *
    */
    public static void createClient() {
        client = builder.buildClient()
    }

    /**
    *  @param username The username associated with the HTTP connection authentication
    *  @param password The password associated with the username for HTTP connection authentication
    */
    public static void createClient(String username, String password) {
        builder.setPreemptiveAuthentication(true)
        builder.setUsername(username)
        builder.setPassword(password)
        createClient()
    }

    /**
    *  @param url The HTTP GET URL the client will attempt to get a response from
    *  @return The response recieved from the server if the return status is 200 OK
    */
    public static HttpResponse getOkResponse(String url) {
        HttpGet get = new HttpGet(url)
        get.addHeader('accept', 'application/json')
        HttpResponse response = client.execute(get)
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            return response
        }
        else {
            throw new IOException("Bad request status code ${response.getStatusLine().getStatusCode()} - " +
                                  "${response.toString().replace('\n', '')}")
        }
    }

    /**
    *  @param url The URL of the Nexus repository manager containing the repository
    *  @param repoName The name of the respository whose id will be found
    *  @return The internal nexus repository ID of the specified repository
    */
    public static String getRepoId(String url, String repoName) {
        println ("[action]  Attempting to find id for repository ${repoName}...")
        String repoId = ''
        HttpResponse response = getOkResponse(url)
        def data = new JsonSlurper().parseText(response.entity?.content?.text).data
        data.each {
            if (it.name == repoName) {
                repoId = it.id
            }
        }
        return repoId
    }

    /**
    *  @param url The URL of the Nexus repository manager containing the NuGet package file to download
    *  @return The NuGet package file specified
    */
    public static File downloadNugetFile(String url) {
        println ('[action]  Attempting to download NuGet package...')
        HttpResponse response = getOkResponse(url)
        File tempFile = File.createTempFile('nuget-', '.nupkg')
        FileUtils.writeInputToFile(response.entity?.content, tempFile)
        return tempFile
    }
}
