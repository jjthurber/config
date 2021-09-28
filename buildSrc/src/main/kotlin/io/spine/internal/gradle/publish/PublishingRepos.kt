/*
 * Copyright 2021, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package io.spine.internal.gradle.publish

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.artifactregistry.auth.DefaultCredentialProvider
import io.spine.internal.gradle.Credentials
import io.spine.internal.gradle.Repository
import java.io.IOException
import org.gradle.api.Project

/**
 * Repositories to which we may publish. Normally, only one repository will be used.
 *
 * See `publish.gradle` for details of the publishing process.
 */
object PublishingRepos {

    private const val CLOUD_ARTIFACT_REGISTRY = "https://europe-maven.pkg.dev/spine-event-engine"

    @Suppress("HttpUrlsUsage") // HTTPS is not supported by this repository.
    val mavenTeamDev = Repository(
        name = "maven.teamdev.com",
        releases = "http://maven.teamdev.com/repository/spine",
        snapshots = "http://maven.teamdev.com/repository/spine-snapshots",
        credentialsFile = "credentials.properties"
    )
    val cloudRepo = Repository(
        name = "CloudRepo",
        releases = "https://spine.mycloudrepo.io/public/repositories/releases",
        snapshots = "https://spine.mycloudrepo.io/public/repositories/snapshots",
        credentialsFile = "cloudrepo.properties"
    )

    /**
     * The experimental Google Cloud Artifact Registry repository.
     *
     * In order to successfully publish into this repository, a service account key is needed.
     * The published must create a service account, grant it the permission to write into
     * Artifact Registry, and generate a JSON key.
     * Then, the key must be placed somewhere on the file system and the environment variable
     * `GOOGLE_APPLICATION_CREDENTIALS` must be set to point at the key file.
     * Once these preconditions are met, publishing becomes possible.
     *
     * Google provides a Gradle plugin for configuring the publishing repository credentials
     * automatically. We achieve the same goal by assembling the credentials manually. We do so
     * in order to fit the Google Cloud Artifact Registry repository into the standard frame of
     * the Maven [Repository]-s. Applying the plugin would take a substantial effort due to the fact
     * that both our publishing scripts and the Google's plugin use `afterEvaluate { }` hooks.
     * Ordering said hooks is a non-trivial operation and the result is usually quite fragile.
     * Thus, we choose to do this small piece of configuration manually.
     */
    val cloudArtifactRegistry = Repository(
        releases = "$CLOUD_ARTIFACT_REGISTRY/releases",
        snapshots = "$CLOUD_ARTIFACT_REGISTRY/snapshots",
        credentialValues = this::fetchGoogleCredentials
    )

    private fun fetchGoogleCredentials(p: Project): Credentials? {
        return try {
            val googleCreds = DefaultCredentialProvider()
            val creds = googleCreds.credential as GoogleCredentials
            creds.refreshIfExpired()
            Credentials("oauth2accesstoken", creds.accessToken.tokenValue)
        } catch (e: IOException) {
            p.logger.info("Unable to fetch credentials for Google Cloud Artifact Registry." +
                    " Reason: '${e.message}'." +
                    " The debug output may contain more details.")
            null
        }
    }

    fun gitHub(repoName: String): Repository {
        val githubActor: String = gitHubActor()
        return Repository(
            name = "GitHub Packages",
            releases = "https://maven.pkg.github.com/SpineEventEngine/$repoName",
            snapshots = "https://maven.pkg.github.com/SpineEventEngine/$repoName",
            credentialValues = { project -> project.credentialsWithToken(githubActor) }
        )
    }

    private fun gitHubActor(): String {
        var githubActor: String? = System.getenv("GITHUB_ACTOR")
        githubActor = if (githubActor.isNullOrEmpty()) {
            "developers@spine.io"
        } else {
            githubActor
        }
        return githubActor
    }

    /**
     * This is a trick. Gradle only supports password or AWS credentials.
     * Thus, we pass the GitHub token as a "password".
     *
     * See https://docs.github.com/en/actions/guides/publishing-java-packages-with-gradle#publishing-packages-to-github-packages
     */
    private fun Project.credentialsWithToken(githubActor: String) = Credentials(
        username = githubActor,
        password = readGitHubToken()
    )

    private fun Project.readGitHubToken(): String {
        val githubToken: String? = System.getenv("GITHUB_TOKEN")
        return if (githubToken.isNullOrEmpty()) {
            // Use the personal access token for the `developers@spine.io` account.
            // Only has the permission to read public GitHub packages.
            val targetDir = "${buildDir}/token"
            file(targetDir).mkdirs()
            val fileToUnzip = "${rootDir}/buildSrc/aus.weis"

            logger.info("GitHub Packages: reading token " +
                    "by unzipping `$fileToUnzip` into `$targetDir`.")
            exec {
                // Unzip with password "123", allow overriding, quietly,
                // into the target dir, the given archive.
                commandLine("unzip", "-P", "123", "-oq", "-d", targetDir, fileToUnzip)
            }
            val file = file("$targetDir/token.txt")
            file.readText()
        } else {
            githubToken
        }
    }
}
