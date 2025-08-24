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

package com.combo.plugin.sample.example.screen

import android.content.ContentUris
import android.content.ContentValues
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.combo.core.ext.deletePlugin
import com.combo.core.ext.insertPlugin
import com.combo.core.ext.queryPlugin
import com.combo.core.ext.registerPluginObserver
import com.combo.core.ext.unregisterPluginObserver
import com.combo.core.ext.updatePlugin
import com.combo.plugin.sample.example.provider.Book
import com.combo.plugin.sample.example.provider.BookProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val BOOKS_URI: Uri = "content://${BookProvider.AUTHORITY}/books".toUri()

/**
 * 重构后的内容提供者示例屏幕
 * 主要变更:
 * 1. 使用 FloatingActionButton (FAB) 和 ModalBottomSheet 替代了原有的顶部固定表单。
 * 2. 界面更加简洁，核心内容（书籍列表）得以突出。
 * 3. 添加/编辑操作在底部抽屉中完成，不干扰主列表的浏览。
 * 4. 优化了列表为空时的显示，提供更友好的用户提示。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentProviderScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // --- 状态管理 ---
    var books by remember { mutableStateOf<List<Book>>(emptyList()) }
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    // `editingBook` 用于跟踪当前正在编辑的书籍。如果为 null，则表示是“添加”模式。
    var editingBook by remember { mutableStateOf<Book?>(null) }

    // --- 数据查询逻辑 ---
    val queryBooks = {
        coroutineScope.launch {
            val bookList = withContext(Dispatchers.IO) {
                val cursor = context.contentResolver.queryPlugin(
                    BOOKS_URI, null, null, null, null
                )
                mutableListOf<Book>().apply {
                    cursor?.use {
                        while (it.moveToNext()) {
                            add(
                                Book(
                                    it.getInt(it.getColumnIndexOrThrow("_id")),
                                    it.getString(it.getColumnIndexOrThrow("title")),
                                    it.getString(it.getColumnIndexOrThrow("author"))
                                )
                            )
                        }
                    }
                }
            }
            books = bookList
        }
    }

    // --- 数据监听 ---
    // 使用 DisposableEffect 来注册和注销 ContentObserver
    DisposableEffect(context) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                queryBooks()
            }
        }
        context.contentResolver.registerPluginObserver(
            BOOKS_URI, true, observer
        )
        queryBooks() // 初始加载数据

        onDispose {
            context.contentResolver.unregisterPluginObserver(observer)
        }
    }

    // --- UI 布局 ---
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("内容提供者示例", fontWeight = FontWeight.Bold) }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("添加新书") },
                icon = { Icon(Icons.Filled.Add, contentDescription = "添加新书") },
                onClick = {
                    editingBook = null // 清空状态，进入“添加”模式
                    showBottomSheet = true
                }
            )
        }
    ) { paddingValues ->
        // 使用 AnimatedContent 来处理列表为空和有数据时的切换动画
        AnimatedContent(
            targetState = books.isEmpty(),
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            label = "list-animation"
        ) { isEmpty ->
            if (isEmpty) {
                // 列表为空时的友好提示
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "书库是空的，\n点击右下角按钮添加一本吧！",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // 书籍列表
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(books, key = { it.id }) { book ->
                        BookListItem(
                            book = book,
                            onClick = {
                                // 点击列表项，进入“编辑”模式
                                editingBook = book
                                showBottomSheet = true
                            },
                            onDelete = {
                                // 删除书籍
                                coroutineScope.launch(Dispatchers.IO) {
                                    val deleteUri =
                                        ContentUris.withAppendedId(BOOKS_URI, book.id.toLong())
                                    context.contentResolver.deletePlugin(
                                        deleteUri, null, null
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }

        // --- 添加/编辑功能的底部抽屉 ---
        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState
            ) {
                AddEditBookSheetContent(
                    editingBook = editingBook,
                    onSave = { title, author ->
                        // 保存书籍（添加或更新）
                        coroutineScope.launch(Dispatchers.IO) {
                            val values = ContentValues().apply {
                                put("title", title)
                                put("author", author)
                            }
                            if (editingBook != null) {
                                // 更新
                                val updateUri = ContentUris.withAppendedId(
                                    BOOKS_URI,
                                    editingBook!!.id.toLong()
                                )
                                context.contentResolver.updatePlugin(
                                    updateUri, values, null, null
                                )
                            } else {
                                // 添加
                                context.contentResolver.insertPlugin(
                                    BOOKS_URI, values
                                )
                            }
                            // 操作完成后，在主线程关闭抽屉
                            withContext(Dispatchers.Main) {
                                showBottomSheet = false
                            }
                        }
                    },
                    onCancel = {
                        showBottomSheet = false
                    }
                )
            }
        }
    }
}

/**
 * 底部抽屉的内容
 * @param editingBook 当前正在编辑的书籍，如果为 null 则为添加模式
 * @param onSave 保存按钮的回调，返回标题和作者
 * @param onCancel 取消按钮的回调
 */
@Composable
private fun AddEditBookSheetContent(
    editingBook: Book?,
    onSave: (title: String, author: String) -> Unit,
    onCancel: () -> Unit
) {
    var title by remember { mutableStateOf(editingBook?.title ?: "") }
    var author by remember { mutableStateOf(editingBook?.author ?: "") }
    val isEditing = editingBook != null

    // 当输入参数 editingBook 变化时，重置输入框内容
    LaunchedEffect(editingBook) {
        title = editingBook?.title ?: ""
        author = editingBook?.author ?: ""
    }

    Column(
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 标题
        Text(
            text = if (isEditing) "编辑书籍" else "添加新书",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        // 输入框
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("书名") },
            leadingIcon = { Icon(Icons.Default.AccountBox, contentDescription = null) },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = author,
            onValueChange = { author = it },
            label = { Text("作者") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 操作按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.End)
        ) {
            TextButton(onClick = onCancel) {
                Text("取消")
            }
            Button(
                onClick = { onSave(title, author) },
                // 只有当标题不为空时才允许保存
                enabled = title.isNotBlank()
            ) {
                Text(if (isEditing) "更新" else "添加")
            }
        }
    }
}

/**
 * 重构后的书籍列表项
 * @param book 书籍数据
 * @param onClick 整个列表项的点击事件
 * @param onDelete 删除按钮的点击事件
 */
@Composable
private fun BookListItem(book: Book, onClick: () -> Unit, onDelete: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        ListItem(
            headlineContent = { Text(book.title, fontWeight = FontWeight.SemiBold) },
            supportingContent = { Text(book.author) },
            leadingContent = {
                Icon(
                    Icons.Default.AccountBox,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingContent = {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            },
            colors = ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        )
    }
}