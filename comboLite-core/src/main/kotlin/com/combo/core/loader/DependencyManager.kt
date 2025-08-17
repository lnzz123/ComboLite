/*
 *
 *  * Copyright (c) 2025, 贵州君城网络科技有限公司
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.combo.core.loader

import com.combo.core.manager.PluginManager
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

/**
 * 插件依赖关系和类查找的核心管理器。
 *
 * 这个类被设计为 PluginManager 的一个内部组件，它完全封装了依赖图的实现细节。
 * 它负责在插件A尝试加载插件B的类时，动态地记录下 A -> B 的依赖关系，
 * 并提供方法来查询这些依赖关系。
 *
 * @property stateProvider 一个提供插件当前状态（如已加载插件列表、类索引）的接口。
 */
internal class DependencyManager(
    private val stateProvider: IPluginStateProvider
) : IPluginFinder {

    /**
     * 正向依赖图
     * Key: 插件ID (依赖方)
     * Value: 它所依赖的插件ID集合
     * e.g., { "pluginA": ["pluginB", "pluginC"] } 表示 A 依赖 B 和 C
     */
    private val dependencyGraph = ConcurrentHashMap<String, MutableSet<String>>()

    /**
     * 反向依赖图（或称"被依赖图"）
     * Key: 插件ID (被依赖方)
     * Value: 依赖于 Key 的插件ID集合
     * e.g., { "pluginB": ["pluginA", "pluginD"] } 表示 A 和 D 依赖 B
     */
    private val dependentGraph = ConcurrentHashMap<String, MutableSet<String>>()

    /**
     * 当一个插件的类加载器无法在本地找到类时，此方法被调用。
     * 它会查询全局类索引来定位持有该类的目标插件，然后从目标插件的类加载器中加载类，
     * 并在这个过程中记录下请求方和目标方之间的依赖关系。
     *
     * @param className 需要查找的类的完全限定名。
     * @param requester 发起类查找请求的插件类加载器。
     * @return 如果找到，则返回加载的 Class 对象；否则返回 null。
     */
    override fun findClass(className: String, requester: PluginClassLoader): Class<*>? {
        val requesterPluginId = requester.pluginId
        val classIndex = stateProvider.getClassIndex()
        val loadedPlugins = stateProvider.getLoadedPlugins()

        // 1. 通过全局类索引查找哪个插件持有这个类
        val targetPluginId = classIndex[className]
        if (targetPluginId == null) {
            // 类索引中不存在，说明这个类不属于任何一个插件
            return null
        }

        // 2. 检查目标插件是否已加载
        val targetPluginInfo = loadedPlugins[targetPluginId]
        if (targetPluginInfo == null) {
            Timber.e(
                "类索引不一致! 类 '$className' 指向插件 '$targetPluginId'，但该插件当前未加载。"
            )
            return null
        }

        // 3. 动态记录依赖关系 (A -> B)
        addDependency(requesterPluginId, targetPluginId)

        // 4. 从目标插件的ClassLoader中加载类（使用findClassLocally避免无限递归）
        return try {
            Timber.d("为插件 [$requesterPluginId] 查找类 [$className]，在插件 [$targetPluginId] 中找到。")
            targetPluginInfo.classLoader.findClassLocally(className)
        } catch (_: ClassNotFoundException) {
            Timber.e(
                "类索引不一致! 在插件 '$targetPluginId' 的DEX中未找到其本应包含的类 '$className'。"
            )
            null
        }
    }

    /**
     * 添加一条依赖关系记录。
     *
     * @param dependingPluginId 发起依赖的插件ID (e.g., pluginA)
     * @param dependentPluginId 被依赖的插件ID (e.g., pluginB)
     */
    private fun addDependency(dependingPluginId: String, dependentPluginId: String) {
        // 防止插件依赖自身
        if (dependingPluginId == dependentPluginId) return

        // 更新正向依赖图: A -> B
        dependencyGraph.getOrPut(dependingPluginId) { ConcurrentHashMap.newKeySet() }
            .add(dependentPluginId)

        // 更新反向依赖图: B <- A
        dependentGraph.getOrPut(dependentPluginId) { ConcurrentHashMap.newKeySet() }
            .add(dependingPluginId)

        Timber.i("动态依赖关系已建立: [$dependingPluginId] -> [$dependentPluginId]")
    }

    /**
     * 当一个插件被卸载时，彻底清除其在依赖图中所有的相关记录。
     * 这包括它对其他插件的依赖，以及其他插件对它的依赖。
     *
     * @param pluginId 要清理的插件ID。
     */
    internal fun clearDependenciesFor(pluginId: String) {
        // 1. 清理 pluginId 对其他插件的依赖 (正向)
        // e.g., A -> B, A -> C
        dependencyGraph[pluginId]?.forEach { dependentId ->
            // 从 B 和 C 的被依赖列表中移除 A
            dependentGraph[dependentId]?.remove(pluginId)
        }
        dependencyGraph.remove(pluginId) // 移除 A 的整个依赖条目

        // 2. 清理其他插件对 pluginId 的依赖 (反向)
        // e.g., D -> A, E -> A
        dependentGraph[pluginId]?.forEach { dependingId ->
            // 从 D 和 E 的依赖列表中移除 A
            dependencyGraph[dependingId]?.remove(pluginId)
        }
        dependentGraph.remove(pluginId) // 移除 A 的整个被依赖条目

        Timber.d("已清理插件 [$pluginId] 的所有依赖关系。")
    }

    /**
     * 递归查找并返回指定插件所依赖的所有插件ID（直接和间接依赖）。
     *
     * @param pluginId 起始插件的ID。
     * @return 一个包含所有依赖项ID的列表，不包含起始插件自身。
     */
    internal fun findDependenciesRecursive(pluginId: String): List<String> {
        val allNodes = recursiveSearch(pluginId, dependencyGraph)
        allNodes.remove(pluginId)
        return allNodes.toList()
    }

    /**
     * 递归查找并返回依赖于指定插件的所有插件ID（直接和间接依赖）。
     *
     * @param pluginId 起始插件的ID。
     * @return 一个包含所有依赖方ID的列表，不包含起始插件自身。
     */
    internal fun findDependentsRecursive(pluginId: String): List<String> {
        val allNodes = recursiveSearch(pluginId, dependentGraph)
        allNodes.remove(pluginId)
        return allNodes.toList()
    }

    /**
     * 通用的深度优先搜索（DFS）算法，用于遍历依赖图。
     *
     * @param startNode 开始遍历的节点（插件ID）。
     * @param graph 要遍历的图（可以是正向或反向依赖图）。
     * @return 一个包含从起始节点出发可达的所有节点（包括起始节点）的集合。
     */
    private fun recursiveSearch(
        startNode: String,
        graph: Map<String, Set<String>>
    ): MutableSet<String> {
        val visited = mutableSetOf<String>()
        val stack = ArrayDeque<String>()

        stack.addLast(startNode)
        visited.add(startNode)

        while (stack.isNotEmpty()) {
            val currentNode = stack.removeLast()
            graph[currentNode]?.forEach { neighbor ->
                if (visited.add(neighbor)) {
                    stack.addLast(neighbor)
                }
            }
        }
        return visited
    }
}

/**
 * 一个用于向 DependencyManager 提供必要状态的接口。
 * 由 PluginManager 实现。
 */
internal interface IPluginStateProvider {
    fun getClassIndex(): Map<String, String>
    fun getLoadedPlugins(): Map<String, PluginManager.LoadedPluginInfo>
}