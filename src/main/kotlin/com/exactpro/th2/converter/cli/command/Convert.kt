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

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.path
import com.github.ajalt.mordant.rendering.TextStyles.*
import com.github.ajalt.mordant.terminal.Terminal
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource
import kotlin.io.path.absolutePathString
import kotlin.io.path.deleteIfExists
import kotlin.io.path.inputStream
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.outputStream


class Convert : CliktCommand(
    name = "convert",
    printHelpOnEmptyArgs = true,
) {
    private val inputDirectory: Path by option(
        "-i", "--in",
        help = "directory with QFJ dictionaries to convert",
        helpTags = mapOf(
            "naming" to "dictionary name must be in format FIXT?<version>_<suffix>.xml",
        ),
    ).path(mustExist = true, canBeDir = true, canBeFile = false)
        .required()

    private val outputDirectory: Path by option("-o", "--out", help = "directory to save resulting dictionaries")
        .path(mustExist = true, canBeDir = true, canBeFile = false)
        .required()

    private val quiet: Boolean by option("--quiet").flag()

    private val pattern: String by option(
        "-p", "--pattern",
        help = "pattern to match QFJ dictionary files in the provided directory",
    ).default("*.xml")
        .validate { require(it.isNotBlank()) { "pattern is blank" } }

    private val terminal: Terminal by requireObject()

    override fun run() {
        val factory = TransformerFactory.newInstance()
        val tmpTypesFile = Files.createTempFile("types", "fix")
        Files.copy(
            loadResource(TYPES_XML),
            tmpTypesFile,
            StandardCopyOption.REPLACE_EXISTING,
        )
        try {
            val transformer =
                factory.newTransformer(StreamSource(loadResource(QFJ_2_DICT_XSL)))
            val qfjDictionaries = hashSetOf<Path>()
            for (file in inputDirectory.listDirectoryEntries(glob = pattern)) {
                transformer.reset()
                qfjDictionaries.clear()
                qfjDictionaries.add(file)

                transformer.apply {
                    if (file.name.startsWith(FIX50_START)) {
                        val fileSuffix: String = extractFileSuffix(file)

                        val sessionDictionary = inputDirectory.resolve("${FIXT_START}11$fileSuffix")
                        qfjDictionaries.add(sessionDictionary)
                        setParameter("sessionDictionary", sessionDictionary.absolutePathString())
                    }
                    setParameter("nsprefix", "CONVERTED_")
                    setParameter("fixtypeSource", tmpTypesFile.absolutePathString())
                }
                if (file.name.startsWith(FIXT_START)) {
                    continue
                }
                val outputDictionaryName = "${file.nameWithoutExtension}_converted.xml"
                outputDirectory.resolve(outputDictionaryName).outputStream().use {
                    transformer.transform(
                        StreamSource(file.inputStream()),
                        StreamResult(it),
                    )
                }

                if (!quiet) {
                    terminal.info(
                        "Dictionaries ${qfjDictionaries.joinToString { bold(it.fileName.toString()) }} converted into $outputDictionaryName"
                    )
                }
            }
        } finally {
            tmpTypesFile.deleteIfExists()
        }
    }

    private fun extractFileSuffix(file: Path): String =
        file.name.removePrefix(FIXT_START).run {
            val separatorIndex = indexOf('_')
            if (separatorIndex > 0) {
                substring(separatorIndex)
            } else {
                this
            }
        }

    private companion object {
        private const val QFJ_2_DICT_XSL = "qfj2dict.xsl"
        private const val TYPES_XML = "types.xml"

        /**
         * Prefix for QFJ dictionaries that follows FIX 5.0
         */
        private const val FIX50_START = "FIX50"

        /**
         * Prefix for QFJ dictionaries for session messages in FIX 5.0
         */
        private const val FIXT_START = "FIXT"

        private fun loadResource(name: String): InputStream =
            requireNotNull(Convert::class.java.classLoader.getResourceAsStream(name)) {
                "cannot load resource '$name'"
            }
    }
}