/*
 * Copyright 2023 Exactpro (Exactpro Systems Limited)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exactpro.th2.converter.cli.command

import com.exactpro.sf.common.messages.structures.IDictionaryStructure
import com.exactpro.sf.common.messages.structures.loaders.XmlDictionaryStructureLoader
import com.exactpro.sf.configuration.dictionary.DictionaryValidationError
import com.exactpro.sf.configuration.dictionary.FullFIXDictionaryValidatorFactory
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.path
import com.github.ajalt.mordant.rendering.TextStyles.bold
import com.github.ajalt.mordant.terminal.Terminal
import java.nio.file.Path
import kotlin.io.path.inputStream
import kotlin.io.path.listDirectoryEntries

class Validate : CliktCommand(
    name = "validate",
    help = "validates Sailfish dictionaries against FIX validator",
) {
    private val dictionariesDir: Path by option(
        "-d", "--dictionaries",
        help = "directory with dictionaries",
    ).path(mustExist = true, canBeFile = false, canBeDir = true)
        .required()

    private val stacktrace: Boolean by option(
        "--stacktrace",
        help = "print stacktrace in case of error during dictionary loading",
    ).flag()

    private val pattern: String by option(
        "-p", "--pattern",
        help = "pattern to match dictionary files in the provided directory",
    ).default("*.xml")
        .validate { require(it.isNotBlank()) { "pattern is blank" } }

    private val terminal: Terminal by requireObject()

    override fun run() {
        val validator = FullFIXDictionaryValidatorFactory().createDictionaryValidator()
        for (dictionary in dictionariesDir.listDirectoryEntries(glob = pattern)) {
            terminal.info("${bold("${dictionary.fileName}")} - validating dictionary")
            val structure: IDictionaryStructure = try {
                dictionary.inputStream().use(XmlDictionaryStructureLoader()::load)
            } catch (ex: Exception) {
                terminal.danger("${bold("${dictionary.fileName}")} - cannot load dictionary")
                terminal.danger("${ex.message}${ex.cause?.let { ": $it" } ?: ""}")
                if (stacktrace) {
                    ex.printStackTrace()
                }
                continue
            }
            val errors: List<DictionaryValidationError> = validator.validate(structure, true, null)
            if (errors.isEmpty()) {
                terminal.success("${bold("${dictionary.fileName}")} - dictionary is valid")
                continue
            }
            terminal.danger("${bold("${dictionary.fileName}")} - dictionary has ${errors.size} error(s):")
            errors.sortedBy { it.error }
                .groupBy { it.message ?: "Dictionary" }
                .forEach { (message, messageErrors) ->
                    terminal.danger(message)
                    messageErrors.forEach {
                        terminal.printDictionaryError(it)
                    }
                }
        }
    }

    private fun Terminal.printDictionaryError(error: DictionaryValidationError) {
        val location = when {
            error.message != null && error.field != null ->
                "${bold(error.message)} message's field ${bold(error.field)}"

            error.field != null ->
                "dictionary field ${bold(error.field)}"

            error.message != null ->
                "message ${bold(error.message)}"

            else -> "in dictionary"
        }
        danger(
            "Error '${bold(error.error)}' in $location of type ${error.type} (${error.level})"
        )
    }
}