package org.virtuslab.bazel.tweaks.sources

import com.google.idea.blaze.base.model.BlazeProjectData
import com.google.idea.blaze.base.projectview.ProjectViewSet
import com.google.idea.blaze.base.sync.BlazeSyncPlugin
import com.google.idea.blaze.base.sync.libraries.LibrarySource
import kotlinx.collections.immutable.toImmutableSet
import java.util.*


class ProjectDataInterceptor : BlazeSyncPlugin {
    override fun getLibrarySource(
        projectViewSet: ProjectViewSet,
        blazeProjectData: BlazeProjectData
    ): LibrarySource? {
        INTERCEPTED_PROJECT_DATA.add(blazeProjectData)
        return super.getLibrarySource(projectViewSet, blazeProjectData)
    }

    companion object {
        private val INTERCEPTED_PROJECT_DATA: MutableSet<BlazeProjectData> = HashSet()

        fun interceptedProjectData(): Set<BlazeProjectData> =
            INTERCEPTED_PROJECT_DATA.toImmutableSet()
    }
}
