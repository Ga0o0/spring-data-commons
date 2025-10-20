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

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Set;

import org.springframework.data.repository.core.support.RepositoryFragment;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.util.TypeInformation;

/**
 * Metadata for repository interfaces.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Alessandro Nistico
 */
// 存储库接口的元数据。
public interface RepositoryMetadata {

	/**
	 * Returns the raw id class the given class is declared for.
	 *
	 * @return the raw id class of the entity managed by the repository.
	 */
	// 返回指定类声明的原始 ID 类。
	default Class<?> getIdType() {
		return getIdTypeInformation().getType();
	}

	/**
	 * Returns the raw domain class the repository is declared for.
	 *
	 * @return the raw domain class the repository is handling.
	 */
	// 返回存储库声明的原始域类。
	default Class<?> getDomainType() {
		return getDomainTypeInformation().getType();
	}

	/**
	 * Returns the {@link TypeInformation} of the id type of the repository.
	 *
	 * @return the {@link TypeInformation} class of the identifier of the entity managed by the repository. Will never be
	 *         {@literal null}.
	 * @since 2.7
	 */
	// 返回存储库 ID 类型的 {@link TypeInformation}。
	TypeInformation<?> getIdTypeInformation();

	/**
	 * Returns the {@link TypeInformation}of the domain type the repository is declared to manage. Will never be
	 * {@literal null}.
	 *
	 * @return the domain class the repository is handling.
	 * @since 2.7
	 */
	// 返回存储库声明管理的域类型的 {@link TypeInformation}。永远不会为 {@literal null}。
	TypeInformation<?> getDomainTypeInformation();

	/**
	 * Returns the repository interface.
	 *
	 * @return
	 */
	// 返回存储库接口。
	Class<?> getRepositoryInterface();

	/**
	 * Returns the type {@link Method} return type as it is declared in the repository. Considers suspended methods and
	 * does not unwrap component types but leaves those for further inspection.
	 *
	 * @param method
	 * @return
	 * @since 2.4
	 */
	// 返回存储库中声明的 {@link Method} 返回类型。考虑挂起的方法，不解包组件类型，而是将其留待进一步检查。
	TypeInformation<?> getReturnType(Method method);

	/**
	 * Returns the domain class returned by the given {@link Method}. In contrast to {@link #getReturnType(Method)}, this
	 * method extracts the type from {@link Collection}s and {@link org.springframework.data.domain.Page} as well.
	 *
	 * @param method
	 * @return
	 * @see #getReturnType(Method)
	 */
	// 返回给定 {@link Method} 返回的域类。与 {@link #getReturnType(Method)} 不同，
	// 此方法还会从 {@link Collection} 和 {@link org.springframework.data.domain.Page} 中提取类型。
	Class<?> getReturnedDomainClass(Method method);

	/**
	 * Returns {@link CrudMethods} meta information for the repository.
	 *
	 * @return
	 */
	// 返回存储库的 {@link CrudMethods} 元信息。
	CrudMethods getCrudMethods();

	/**
	 * Returns whether the repository is a paging one.
	 *
	 * @return
	 */
	// 返回存储库是否为分页存储库。
	boolean isPagingRepository();

	/**
	 * Returns the set of types the repository shall be discoverable for when trying to look up a repository by domain
	 * type.
	 *
	 * @see Repositories#getRepositoryFor(Class)
	 * @return the set of types the repository shall be discoverable for when trying to look up a repository by domain
	 *         type, must not be {@literal null}.
	 * @since 1.11
	 */
	// 返回当尝试通过域类型查找存储库时，存储库应可被发现的类型集合。
	Set<Class<?>> getAlternativeDomainTypes();

	/**
	 * Returns whether the repository is a reactive one, i.e. if it uses reactive types in one of its methods.
	 *
	 * @return
	 * @since 2.0
	 */
	// 返回存储库是否为反应式存储库，即它是否在其某个方法中使用反应式类型。
	boolean isReactiveRepository();

	/**
	 * @return
	 * @since 3.0
	 */
	Set<RepositoryFragment<?>> getFragments();
}
