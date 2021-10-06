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

import groovy.xml.MarkupBuilder
import io.spine.internal.gradle.report.pom.PomFormatting.writeBlocks
import io.spine.internal.gradle.report.pom.PomFormatting.writeStart
import java.io.File
import java.io.FileWriter
import java.io.StringWriter
import org.gradle.api.Project
import org.gradle.kotlin.dsl.withGroovyBuilder

/**
 * Writes the dependencies of a Gradle project and its subprojects as a `pom.xml` file.
 *
 * The resulting file is not usable for `maven` build tasks, but serves rather as a description
 * of the first-level dependencies for each project/subproject. Their transitive dependencies
 * are not included into the result.
 */
internal class PomXmlWriter
private constructor(
    private val project: Project,
    private val groupId: String,
    private val artifactId: String,
    private val version: String
) {

    internal companion object {
        fun from(data: RootProjectData): PomXmlWriter {
            return PomXmlWriter(data.project, data.groupId, data.artifactId, data.version)
        }
    }

    /**
     * Writes the {@code pom.xml} file containing dependencies of this project and its subprojects to the specified
     * location.
     *
     * <p>If a file with the specified location exists, its contents will be substituted with a new
     * {@code pom.xml}.
     *
     * @param file a file to write {@code pom.xml} contents to
     */
    fun writeTo(file: File) {
        val fileWriter = FileWriter(file)
        val out = StringWriter()

        writeStart(out)
        writeBlocks(
            out,
            rootProjectData(),
            InceptionYear.asXml(),
            SpineLicense.asXml(),
            projectDependencies()
        )
        PomFormatting.writeEnd(out)

        fileWriter.write(out.toString())
        fileWriter.close()
    }

    /**
     * Obtains a string that contains project dependencies as XML.
     *
     * <p>Obtained string also contains a closing project tag.
     */
    private fun projectDependencies(): String {
        val destination = StringWriter()
        val dependencyWriter = DependencyWriter.of(project)
        dependencyWriter.writeXmlTo(destination)
        return destination.toString()
    }

    /**
     * Obtains a string that contains the name and the version of the current project.
     */
    private fun rootProjectData(): String {
        val writer = StringWriter()
        val xmlBuilder = MarkupBuilder(writer)
        xmlBuilder.withGroovyBuilder {
            "groupId" to groupId
            "artifactId" to artifactId
            "version" to version
        }
        return writer.toString()
    }
}
