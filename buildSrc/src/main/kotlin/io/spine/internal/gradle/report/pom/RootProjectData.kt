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

package io.spine.internal.gradle.report.pom

import org.gradle.api.Project
import org.gradle.api.plugins.ExtraPropertiesExtension

/**
 * Information about the root project.
 *
 * In this context, root project is the Gradle project for which the `pom.xml` generation
 * is performed
 *
 * Includes group ID, artifact name, and the version.
 */
internal class RootProjectData
private constructor(
    val project: Project,
    val groupId: String,
    val artifactId: String,
    val version: String
) {

    internal companion object {

        /**
         * Creates a new instance of `RootProjectData`.
         *
         * The required information is first retrieved from the passed [project]. And if
         * a property is missing from the `project`, it is taken from the passed [extension].
         */
        fun fromEither(
            project: Project, /* or */
            extension: ExtraPropertiesExtension
        ): RootProjectData {
            val groupId: String = groupId(project, extension)
            val name: String = name(project, extension)
            val version: String = version(project, extension)
            return RootProjectData(project, groupId, name, version)
        }

        private fun version(project: Project, extension: ExtraPropertiesExtension): String {
            val projectVersion = project.version.toString()
            val versionMissingFromProject = projectVersion.isEmpty()
            val version: String = if (versionMissingFromProject) {
                extension.get("version").toString()
            } else {
                projectVersion
            }
            return version
        }

        private fun name(project: Project, extension: ExtraPropertiesExtension): String {
            val projectName = project.name
            val nameMissingFromProject = projectName.isEmpty()
            val name: String = if (nameMissingFromProject) {
                extension.get("artifactId").toString()
            } else {
                projectName
            }
            return name
        }

        private fun groupId(project: Project, extension: ExtraPropertiesExtension): String {
            val projectGroup = project.group.toString()
            val groupMissingFromProject = projectGroup.isEmpty()
            val groupId: String = if (groupMissingFromProject) {
                extension.get("groupId").toString()
            } else {
                projectGroup
            }
            return groupId
        }
    }
}
