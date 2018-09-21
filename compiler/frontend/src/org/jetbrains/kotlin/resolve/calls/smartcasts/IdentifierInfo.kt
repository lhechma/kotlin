/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.resolve.calls.smartcasts

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.lexer.KtToken
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValue.Kind.OTHER
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValue.Kind.STABLE_VALUE
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.KotlinType

interface IdentifierInfo {

    val kind: DataFlowValue.Kind get() = OTHER

    val canBeBound get() = false

    object NO : IdentifierInfo {
        override fun toString() = "NO_IDENTIFIER_INFO"
    }

    object NULL : IdentifierInfo {
        override fun toString() = "NULL"
    }

    object ERROR : IdentifierInfo {
        override fun toString() = "ERROR"
    }

    class Variable(
        val variable: VariableDescriptor,
        override val kind: DataFlowValue.Kind,
        val bound: DataFlowValue?
    ) : IdentifierInfo {
        override val canBeBound
            get() = kind == STABLE_VALUE

        override fun equals(other: Any?) = other is Variable && variable == other.variable

        override fun hashCode() = variable.hashCode()

        override fun toString() = variable.toString()
    }

    data class Receiver(val value: ReceiverValue) : IdentifierInfo {
        override val kind = STABLE_VALUE

        override fun toString() = value.toString()
    }

    data class PackageOrClass(val descriptor: DeclarationDescriptor) : IdentifierInfo {
        override val kind = STABLE_VALUE

        override fun toString() = descriptor.toString()
    }

    data class EnumEntry(val descriptor: ClassDescriptor) : IdentifierInfo {
        override val kind: DataFlowValue.Kind = STABLE_VALUE
    }

    class Qualified(
        val receiverInfo: IdentifierInfo,
        val selectorInfo: IdentifierInfo,
        val safe: Boolean,
        val receiverType: KotlinType?
    ) : IdentifierInfo {

        override val kind: DataFlowValue.Kind get() = if (receiverInfo.kind == STABLE_VALUE) selectorInfo.kind else OTHER

        override val canBeBound
            get() = receiverInfo.canBeBound

        override fun equals(other: Any?) = other is Qualified && receiverInfo == other.receiverInfo && selectorInfo == other.selectorInfo

        override fun hashCode() = 31 * receiverInfo.hashCode() + selectorInfo.hashCode()

        override fun toString() = "$receiverInfo${if (safe) "?." else "."}$selectorInfo"
    }

    data class SafeCast(val subjectInfo: IdentifierInfo, val subjectType: KotlinType?, val targetType: KotlinType?) : IdentifierInfo {
        override val kind get() = OTHER

        override val canBeBound get() = subjectInfo.canBeBound

        override fun toString() = "$subjectInfo as? ${targetType ?: "???"}"
    }

    class Expression(val expression: KtExpression, stableComplex: Boolean = false) : IdentifierInfo {
        override val kind = if (stableComplex) DataFlowValue.Kind.STABLE_COMPLEX_EXPRESSION else OTHER

        override fun equals(other: Any?) = other is Expression && expression == other.expression

        override fun hashCode() = expression.hashCode()

        override fun toString() = expression.text ?: "(empty expression)"
    }

    // For only ++ and -- postfix operations
    data class PostfixIdentifierInfo(val argumentInfo: IdentifierInfo, val op: KtToken) : IdentifierInfo {
        override val kind: DataFlowValue.Kind get() = argumentInfo.kind

        override fun toString() = "$argumentInfo($op)"
    }
}
