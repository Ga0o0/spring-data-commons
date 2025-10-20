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
package org.springframework.data.repository;

import org.springframework.stereotype.Indexed;

/**
 * Central repository marker interface. Captures the domain type to manage as well as the domain type's id type. General
 * purpose is to hold type information as well as being able to discover interfaces that extend this one during
 * classpath scanning for easy Spring bean creation.
 * <p>
 * Domain repositories extending this interface can selectively expose CRUD methods by simply declaring methods of the
 * same signature as those declared in {@link CrudRepository}.
 * 
 * @see CrudRepository
 * @param <T> the domain type the repository manages
 * @param <ID> the type of the id of the entity the repository manages
 * @author Oliver Gierke
 */
// 中央存储库标记接口。捕获要管理的域类型以及域类型的 ID 类型。
// 其主要用途是保存类型信息，并能够在类路径扫描期间发现扩展此接口的接口，以便轻松创建 Spring Bean。
// <p>
// 扩展此接口的域存储库可以通过简单地声明与 {@link CrudRepository} 中声明的方法具有相同签名的方法，选择性地公开 CRUD 方法。
//
// @see CrudRepository
// @param <T> 存储库管理的域类型
// @param <ID> 存储库管理的实体 ID 的类型
@Indexed
public interface Repository<T, ID> {

}
