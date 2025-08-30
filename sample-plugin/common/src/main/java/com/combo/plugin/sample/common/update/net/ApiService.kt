/*
 * Copyright (c) 2025, 贵州君城网络科技有限公司
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

package com.combo.plugin.sample.common.update.net

import com.combo.plugin.sample.common.update.model.PluginConfig
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url

interface ApiService {
    /**
     * 获取插件配置文件
     */
    @GET("lnzz123/combolite/main/updates/plugins.json")
    suspend fun fetchPluginConfig(): PluginConfig

    /**
     * 下载文件
     */
    @Streaming
    @GET
    suspend fun downloadFile(@Url fileUrl: String): ResponseBody
}