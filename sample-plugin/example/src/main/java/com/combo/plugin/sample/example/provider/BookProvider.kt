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

package com.combo.plugin.sample.example.provider

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import androidx.core.net.toUri
import timber.log.Timber

class BookProvider : ContentProvider() {

    companion object {
        const val AUTHORITY = "com.combo.plugin.sample.example.provider.books"
        val CONTENT_URI: Uri = "content://$AUTHORITY/books".toUri()

        private const val BOOKS = 1
        private const val BOOK_ID = 2

        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, "books", BOOKS)
            addURI(AUTHORITY, "books/#", BOOK_ID)
        }
    }

    override fun onCreate(): Boolean {
        Timber.d("插件 BookProvider 已创建")
        return true
    }

    override fun query(
        uri: Uri, projection: Array<String>?, selection: String?,
        selectionArgs: Array<String>?, sortOrder: String?
    ): Cursor {
        val cursor = MatrixCursor(arrayOf("_id", "title", "author"))
        when (uriMatcher.match(uri)) {
            BOOKS -> {
                BookRepository.books.forEach {
                    cursor.addRow(arrayOf<Any>(it.id, it.title, it.author))
                }
            }

            BOOK_ID -> {
                val id = uri.lastPathSegment?.toIntOrNull()
                BookRepository.books.find { it.id == id }?.let {
                    cursor.addRow(arrayOf<Any>(it.id, it.title, it.author))
                }
            }
        }
        return cursor
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        return when (uriMatcher.match(uri)) {
            BOOKS -> {
                val title = values?.getAsString("title") ?: return null
                val author = values.getAsString("author") ?: return null
                val newBook = BookRepository.addBook(title, author)
                ContentUris.withAppendedId(CONTENT_URI, newBook.id.toLong())
            }

            else -> null
        }
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        return when (uriMatcher.match(uri)) {
            BOOK_ID -> {
                val id = uri.lastPathSegment?.toIntOrNull() ?: return 0
                if (BookRepository.deleteBook(id)) {
                    1
                } else {
                    0
                }
            }

            else -> 0
        }
    }

    override fun update(
        uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?
    ): Int {
        return when (uriMatcher.match(uri)) {
            BOOK_ID -> {
                val id = uri.lastPathSegment?.toIntOrNull() ?: return 0
                val title = values?.getAsString("title") ?: return 0
                val author = values.getAsString("author") ?: return 0
                if (BookRepository.updateBook(id, title, author)) {
                    1
                } else {
                    0
                }
            }

            else -> 0
        }
    }

    override fun getType(uri: Uri): String? {
        return when (uriMatcher.match(uri)) {
            BOOKS -> "vnd.android.cursor.dir/vnd.$AUTHORITY.books"
            BOOK_ID -> "vnd.android.cursor.item/vnd.$AUTHORITY.books"
            else -> null
        }
    }
}