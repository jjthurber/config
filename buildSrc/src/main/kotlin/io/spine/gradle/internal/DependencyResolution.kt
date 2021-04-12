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

package io.spine.gradle.internal

import io.spine.gradle.internal.dependency.AutoService
import io.spine.gradle.internal.dependency.AutoValue
import io.spine.gradle.internal.dependency.CheckerFramework
import io.spine.gradle.internal.dependency.CommonsCli
import io.spine.gradle.internal.dependency.CommonsLogging
import io.spine.gradle.internal.dependency.ErrorProne
import io.spine.gradle.internal.dependency.Gson
import io.spine.gradle.internal.dependency.Guava
import io.spine.gradle.internal.dependency.J2ObjC
import io.spine.gradle.internal.dependency.JUnit
import io.spine.gradle.internal.dependency.Kotlin
import io.spine.gradle.internal.dependency.Okio
import io.spine.gradle.internal.dependency.Plexus
import io.spine.gradle.internal.dependency.Protobuf
import io.spine.gradle.internal.dependency.Truth
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.dsl.RepositoryHandler

@Suppress("unused")
object DependencyResolution {

    fun forceConfiguration(configurations: ConfigurationContainer) {
        configurations.all {
            resolutionStrategy {
                failOnVersionConflict()
                cacheChangingModulesFor(0, "seconds")

                @Suppress("DEPRECATION") // Force SLF4J version.
                Deps.build.apply {
                    force(
                        animalSniffer,
                        autoCommon,
                        AutoService.annotations,
                        CheckerFramework.annotations,
                        ErrorProne.annotations,
                        Guava.lib,
                        jsr305Annotations,
                        Kotlin.reflect,
                        Kotlin.stdLib,
                        Kotlin.stdLibCommon,
                        Kotlin.stdLibJdk8,
                        Protobuf.libs,
                        Protobuf.gradlePlugin,
                        slf4j
                    )
                }

                Deps.test.apply {
                    force(
                        guavaTestlib,
                        JUnit.api,
                        JUnit.platformCommons,
                        JUnit.platformLauncher,
                        junit4,
                        Truth.libs
                    )
                }

                // Force transitive dependencies of 3rd party components that we don't use directly.
                force(
                    AutoValue.annotations,
                    Gson.lib,
                    J2ObjC.lib,
                    Plexus.utils,
                    Okio.lib,
                    CommonsCli.lib,
                    CheckerFramework.compatQual,
                    CommonsLogging.lib
                )
            }
        }
    }

    @Suppress("unused")
    fun excludeProtobufLite(configurations: ConfigurationContainer) {
        excludeProtoLite(configurations, "runtime")
        excludeProtoLite(configurations, "testRuntime")
    }

    private fun excludeProtoLite(
        configurations: ConfigurationContainer,
        configurationName: String
    ) {
        configurations
            .named(configurationName).get()
            .exclude(mapOf("group" to "com.google.protobuf", "module" to "protobuf-lite"))
    }

    @Suppress("unused")
    @Deprecated(
        "Please use `applyStandard(repositories)` instead.",
        replaceWith = ReplaceWith("applyStandard(repositories)")
    )
    fun defaultRepositories(repositories: RepositoryHandler) {
        applyStandard(repositories)
    }
}
