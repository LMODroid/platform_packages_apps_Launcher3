/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.quickstep.util

import android.app.WindowConfiguration.WINDOWING_MODE_MULTI_WINDOW
import android.util.Log
import android.view.WindowManager.TRANSIT_CHANGE
import android.view.WindowManager.TRANSIT_OPEN
import android.view.WindowManager.TRANSIT_TO_FRONT
import android.window.TransitionInfo
import android.window.TransitionInfo.Change
import android.window.TransitionInfo.FLAG_FIRST_CUSTOM
import com.android.launcher3.util.SplitConfigurationOptions
import com.android.wm.shell.shared.split.SplitBounds

class SplitScreenUtils {
    companion object {
        private const val TAG = "SplitScreenUtils"

        // TODO(b/254378592): Remove these methods when the two classes are reunited
        /** Converts the shell version of SplitBounds to the launcher version */
        @JvmStatic
        fun convertShellSplitBoundsToLauncher(shellSplitBounds: SplitBounds) =
            SplitConfigurationOptions.SplitBounds(
                shellSplitBounds.leftTopBounds,
                shellSplitBounds.rightBottomBounds,
                shellSplitBounds.leftTopTaskId,
                shellSplitBounds.rightBottomTaskId,
                shellSplitBounds.snapPosition,
            )

        /**
         * Given a TransitionInfo, generates the tree structure for those changes and extracts out
         * the top most root and it's two immediate children. Changes can be provided in any order.
         *
         * @return a [Pair] where first -> top most split root, second -> [List] of 2,
         *   leftTop/bottomRight stage roots
         */
        fun extractTopParentAndChildren(
            transitionInfo: TransitionInfo
        ): Pair<Change, List<Change>>? {
            val parentToChildren = mutableMapOf<Change, MutableList<Change>>()
            val hasParent = mutableSetOf<Change>()
            // A task returning from freeform can stand in for an omitted stage root.
            fun isCrossDisplaySplitTask(change: Change): Boolean =
                change.mode == TRANSIT_CHANGE &&
                    change.startDisplayId != change.endDisplayId &&
                    change.taskInfo?.windowingMode == WINDOWING_MODE_MULTI_WINDOW &&
                    change.parent?.let(transitionInfo::getChange) != null

            val taskChanges: List<Change> =
                transitionInfo.changes
                    .filter { change ->
                        (change.mode == TRANSIT_OPEN ||
                            change.mode == TRANSIT_TO_FRONT ||
                            isCrossDisplaySplitTask(change)) &&
                            change.flags < FLAG_FIRST_CUSTOM
                    }
                    .toList()

            // A reported stage can also be CHANGE when its child returns from freeform.
            val changedStages =
                taskChanges
                    .filter(::isCrossDisplaySplitTask)
                    .mapNotNull { task ->
                        task.parent?.let(transitionInfo::getChange)?.takeIf { stage ->
                            stage.mode == TRANSIT_CHANGE &&
                                stage.flags < FLAG_FIRST_CUSTOM &&
                                stage.parent != null &&
                                stage.taskInfo?.taskId == task.taskInfo?.parentTaskId
                        }
                    }
                    .toSet()

            // 1. Build Parent-Child Relationships
            for (change in taskChanges + changedStages) {
                // TODO (b/316490565): Replace this logic when SplitBounds is available to
                //  startAnimation() and we can know the precise taskIds of launching tasks.
                change.parent?.let { parent ->
                    parentToChildren
                        .getOrPut(transitionInfo.getChange(parent)!!) { mutableListOf() }
                        .add(change)
                    hasParent.add(change)
                }
            }

            // 2. Find Top Parent
            val openingRoot = taskChanges.firstOrNull { it !in hasParent }
            // The shared root of a cross-display split launch can itself be a CHANGE.
            val topParent =
                if (openingRoot != null && parentToChildren[openingRoot]?.size == 2) {
                    openingRoot
                } else {
                    parentToChildren.entries.singleOrNull { (root, children) ->
                        root.parent == null &&
                            root.mode == TRANSIT_CHANGE &&
                            children.size == 2 &&
                            children.any { it in changedStages || isCrossDisplaySplitTask(it) }
                    }?.key ?: openingRoot
                }

            // 3. Extract Immediate Children
            return if (topParent != null) {
                val immediateChildren = parentToChildren.getOrDefault(topParent, emptyList())
                if (immediateChildren.size != 2) {
                    Log.w(
                        TAG,
                        "Expected 2 split stage roots, found ${immediateChildren.size}: " +
                            transitionInfo,
                    )
                    return null
                }
                Pair(topParent, immediateChildren)
            } else {
                Log.w(TAG, "No top parent found")
                null
            }
        }
    }
}
