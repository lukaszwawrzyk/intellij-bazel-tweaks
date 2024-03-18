package org.virtuslab.bazel.tweaks.sources

import com.google.common.collect.ImmutableList
import com.google.idea.blaze.base.command.buildresult.LocalFileArtifact
import com.google.idea.blaze.base.ideinfo.ArtifactLocation
import com.google.idea.blaze.base.ideinfo.LibraryArtifact
import com.google.idea.blaze.base.model.BlazeLibrary
import com.google.idea.blaze.base.sync.libraries.BlazeLibrarySorter
import com.google.idea.blaze.java.sync.model.BlazeJarLibrary
import java.nio.file.Files
import java.nio.file.Path


class MissingLibrarySourceAttacher : BlazeLibrarySorter {
    override fun sort(libraries: List<BlazeLibrary>): List<BlazeLibrary> =
        libraries.map { addMissingSourceJar(it) }

    private fun addMissingSourceJar(bazelLibrary: BlazeLibrary): BlazeLibrary {
        if (bazelLibrary !is BlazeJarLibrary) {
            return bazelLibrary
        }

        if (bazelLibrary.libraryArtifact.sourceJars.isNotEmpty()) {
            return bazelLibrary
        }

        val classJar = bazelLibrary.libraryArtifact.classJar
        val interfaceJar = bazelLibrary.libraryArtifact.interfaceJar
        val sourceJar = findMissingSourceJar(classJar) ?: findMissingSourceJar(interfaceJar)

        if (sourceJar == null) {
            return bazelLibrary
        }

        val libraryWithSourceJar = LibraryArtifact(
            bazelLibrary.libraryArtifact.interfaceJar,
            bazelLibrary.libraryArtifact.classJar,
            ImmutableList.of(sourceJar)
        )

        return BlazeJarLibrary(libraryWithSourceJar, bazelLibrary.targetKey)
    }

    private fun findMissingSourceJar(jarLocation: ArtifactLocation?): ArtifactLocation? {
        if (jarLocation == null) {
            return null
        }

        return resolveToPaths(jarLocation)
            .mapNotNull { tryResolveSourcesSuffix(it) }
            .map { suffix -> replaceSuffix(jarLocation, suffix) }
            .firstOrNull()
    }

    private fun resolveToPaths(jarLocation: ArtifactLocation): Sequence<Path> {
        // This extension point doesn't get access to BazelProjectData which is necessary
        // to resolve Path from ArtifactLocation. But it is available to BazelSyncPlugin.
        // Both of these endpoints are called in BlazeLibraryCollector, so ProjectDataInterceptor
        // is a BazelSyncPlugin, and it intercepts the project data, and then it is used here.
        val bazelProjectData = ProjectDataInterceptor.interceptedProjectData()
        return bazelProjectData.asSequence()
            .map { it.artifactLocationDecoder.resolveOutput(jarLocation) }
            .mapNotNull { (it as? LocalFileArtifact)?.file?.toPath() }
            .distinct()
    }

    private fun tryResolveSourcesSuffix(path: Path): String? {
        val name = path.fileName.toString()
        for (suffix in listOf("-sources.jar", "-src.jar")) {
            val source = path.resolveSibling(name.replace(".jar", suffix))
            if (Files.exists(source)) {
                return suffix
            }
        }
        return null
    }

    private fun replaceSuffix(jarLocation: ArtifactLocation, suffix: String?): ArtifactLocation {
        val proto = jarLocation.toProto()
        val replacedRelativePath = proto.relativePath.replace(".jar", suffix!!)
        return ArtifactLocation.builder()
            .setRootExecutionPathFragment(proto.rootExecutionPathFragment)
            .setRelativePath(replacedRelativePath)
            .setIsSource(proto.isSource)
            .setIsExternal(proto.isExternal)
            .build()
    }
}
