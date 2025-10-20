/*
 * Copyright 2011-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.repository.core;

/**
 * Abstraction of a map of {@link NamedQueries} that can be looked up by their names.
 *
 * @author Oliver Gierke
 */
// 可以通过名称查找的 {@link NamedQueries} 映射的抽象。
public interface NamedQueries {

	/**
	 * Returns whether the map contains a named query for the given name. If this method returns {@literal true} you can
	 * expect {@link #getQuery(String)} to return a non-{@literal null} query for the very same name.
	 *
	 * @param queryName must not be {@literal null} or empty.
	 * @return
	 * @throws IllegalArgumentException in case the given name is {@literal null} or empty.
	 */
	// 返回映射是否包含指定名称的命名查询。
	// 如果此方法返回 {@literal true}，则 {@link #getQuery(String)} 可能会返回与指定名称不符的非 {@literal null} 查询。
	boolean hasQuery(String queryName);

	/**
	 * Returns the named query with the given name.
	 *
	 * @param queryName must not be {@literal null} or empty.
	 * @return
	 * @throws IllegalArgumentException in case no query with the given name exists.
	 */
	// 返回具有给定名称的命名查询。
	String getQuery(String queryName);
}
