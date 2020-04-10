/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle.importing

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import org.gradle.tooling.model.kotlin.dsl.EditorReportSeverity
import org.gradle.tooling.model.kotlin.dsl.KotlinDslScriptsModel
import org.jetbrains.kotlin.idea.KotlinIdeaGradleBundle
import org.jetbrains.kotlin.idea.scripting.gradle.GradleScriptInputsWatcher
import org.jetbrains.kotlin.idea.scripting.gradle.GradleScriptingSupport
import org.jetbrains.kotlin.idea.scripting.gradle.getGradleScriptInputsStamp
import org.jetbrains.kotlin.scripting.definitions.findScriptDefinition
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper
import org.jetbrains.kotlin.scripting.resolve.VirtualFileScriptSource
import org.jetbrains.kotlin.scripting.resolve.adjustByDefinition
import org.jetbrains.plugins.gradle.service.project.ProjectResolverContext
import java.io.File
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.jdkHome
import kotlin.script.experimental.jvm.jvm

fun processScriptModel(
    resolverCtx: ProjectResolverContext,
    model: KotlinDslScriptsModel,
    projectName: String
) {
    if (model is BrokenKotlinDslScriptsModel) {
        LOG.error(
            "Couldn't get KotlinDslScriptsModel for $projectName:\n${model.message}\n${model.stackTrace}"
        )
    } else {
        val project = resolverCtx.externalSystemTaskId.findProject() ?: return
        val models = model.toListOfScriptModels(project)

        saveScriptModels(resolverCtx, models)

        if (models.containsErrors()) {
            throw IllegalStateException(KotlinIdeaGradleBundle.message("title.kotlin.build.script"))
        }
    }
}

private fun Collection<KotlinDslScriptModel>.containsErrors(): Boolean {
    return any { it.messages.any { it.severity == KotlinDslScriptModel.Severity.ERROR } }
}

private fun KotlinDslScriptsModel.toListOfScriptModels(project: Project): List<KotlinDslScriptModel> =
    scriptModels.map { (file, model) ->
        val messages = mutableListOf<KotlinDslScriptModel.Message>()

        model.exceptions.forEach {
            val fromException = parsePositionFromException(it)
            if (fromException != null) {
                val (filePath, _) = fromException
                if (filePath != file.path) return@forEach
            }
            messages.add(
                KotlinDslScriptModel.Message(
                    KotlinDslScriptModel.Severity.ERROR,
                    it.substringBefore(System.lineSeparator()),
                    it,
                    fromException?.second
                )
            )
        }

        model.editorReports.forEach {
            messages.add(
                KotlinDslScriptModel.Message(
                    when (it.severity) {
                        EditorReportSeverity.WARNING -> KotlinDslScriptModel.Severity.WARNING
                        else -> KotlinDslScriptModel.Severity.ERROR
                    },
                    it.message,
                    position = KotlinDslScriptModel.Position(it.position?.line ?: 0, it.position?.column ?: 0)
                )
            )
        }

        // TODO: NPE
        val virtualFile = VfsUtil.findFile(file.toPath(), true)!!

        // todo(KT-34440): take inputs snapshot before starting import
        KotlinDslScriptModel(
            file.absolutePath,
            // TODO: NPE
            getGradleScriptInputsStamp(project, virtualFile)!!,
            model.classPath.map { it.absolutePath },
            model.sourcePath.map { it.absolutePath },
            model.implicitImports,
            messages
        )
    }

class GradleKtsContext(val project: Project, val javaHome: File?)

fun KotlinDslScriptModel.toScriptConfiguration(context: GradleKtsContext): ScriptCompilationConfigurationWrapper? {
    val scriptFile = File(file)
    val virtualFile = VfsUtil.findFile(scriptFile.toPath(), true)!!

    val definition = virtualFile.findScriptDefinition(context.project) ?: return null

    return ScriptCompilationConfigurationWrapper.FromCompilationConfiguration(
        VirtualFileScriptSource(virtualFile),
        definition.compilationConfiguration.with {
            if (context.javaHome != null) {
                jvm.jdkHome(context.javaHome)
            }
            defaultImports(imports)
            dependencies(JvmDependency(classPath.map { File(it) }))
            ide.dependenciesSources(JvmDependency(sourcePath.map { File(it) }))
        }.adjustByDefinition(definition)
    )
}

fun saveScriptModels(
    resolverContext: ProjectResolverContext,
    models: List<KotlinDslScriptModel>
) {
    val task = resolverContext.externalSystemTaskId
    val project = task.findProject() ?: return
    val settings = resolverContext.settings ?: return

    val errorReporter = KotlinGradleDslErrorReporter(project, task)

    val javaHome = settings.javaHome?.let { File(it) }
    val context = GradleKtsContext(project, javaHome)

    models.forEach { model ->
        errorReporter.reportError(File(model.file), model)
    }

    project.service<GradleScriptInputsWatcher>().saveGradleProjectRootsAfterImport(
        models.map { File(it.file).parent }.toSet()
    )

    GradleScriptingSupport.getInstance(project).replace(context, models)
    project.service<GradleScriptInputsWatcher>().clearState()
}