package org.virtuslab.bazel.tweaks.sources

import com.google.idea.blaze.java.sync.model.AttachSourcesFilter
import com.google.idea.blaze.java.sync.model.BlazeJarLibrary


class LibrarySourceAttachStrategy : AttachSourcesFilter {
    override fun shouldAlwaysAttachSourceJar(library: BlazeJarLibrary): Boolean {
        return true
    }
}
