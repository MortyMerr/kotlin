/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.konan.diagnostics

import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.rendering.Renderers

private val DIAGNOSTIC_FACTORY_TO_RENDERER by lazy {
    DiagnosticFactoryToRendererMap("Native").apply {
        put(ErrorsNative.THROWS_LIST_EMPTY, "@Throws must have non-empty class list")
        put(
            ErrorsNative.INCOMPATIBLE_THROWS_OVERRIDE, "Member overrides different @Throws filter from {0}",
            Renderers.NAME
        )
        put(
            ErrorsNative.INCOMPATIBLE_THROWS_INHERITED, "Member inherits different @Throws filters from {0}",
            Renderers.commaSeparated(Renderers.NAME)
        )
        put(ErrorsNative.MUTABLE_ENUM, "Enum mutation after init is not possible")
        put(ErrorsNative.MUTABLE_SINGLETON, "Singleton mutation without @ThreadLocal is not possible")
        put(ErrorsNative.USELESS_SHARED_IMMUTABLE, "SharedImmutable is only needed for globally declared `val` or `var` with delegate")
        put(ErrorsNative.USELESS_THREAD_LOCAL, "ThreadLocal is only needed for globally declared `val` or `var` with delegate")
        put(ErrorsNative.PREFER_ATOMIC_REFERENCE, "Prefer explicitly frozen references usage")
    }
}

class DefaultErrorMessagesNative : DefaultErrorMessages.Extension {
    override fun getMap(): DiagnosticFactoryToRendererMap = DIAGNOSTIC_FACTORY_TO_RENDERER
}
