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

package com.combo.plugin.sample.example.provider

data class Book(val id: Int, val title: String, val author: String)

object BookRepository {
    val books = mutableListOf(
        Book(1, "Compose 插件化开发", "ComboLite"),
        Book(2, "Kotlin 协程入门", "佚名")
    )
    private var nextId = 3

    fun addBook(title: String, author: String): Book {
        val newBook = Book(nextId++, title, author)
        books.add(newBook)
        return newBook
    }

    fun deleteBook(id: Int): Boolean {
        return books.removeAll { it.id == id }
    }

    fun updateBook(id: Int, newTitle: String, newAuthor: String): Boolean {
        val book = books.find { it.id == id } ?: return false
        val index = books.indexOf(book)
        books[index] = book.copy(title = newTitle, author = newAuthor)
        return true
    }
}