/*
 * Copyright 2014-2025 the original author or authors.
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
package org.springframework.data.repository.config;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.util.Lazy;
import org.springframework.data.util.StreamUtils;
import org.springframework.util.Assert;

/**
 * Detects the custom implementation for a {@link org.springframework.data.repository.Repository} instance. If
 * configured with a {@link ImplementationDetectionConfiguration} at construction time, the necessary component scan is
 * executed on first access, cached and its result is the filtered on every further implementation lookup according to
 * the given {@link ImplementationDetectionConfiguration}. If none is given initially, every invocation to
 * {@link #detectCustomImplementation(ImplementationLookupConfiguration)} will issue a new component scan.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Christoph Strobl
 * @author Peter Rietzler
 * @author Jens Schauder
 * @author Mark Paluch
 */
// 检测 {@link org.springframework.data.repository.Repository} 实例的自定义实现。
// 如果在构造时配置了 {@link ImplementationDetectionConfiguration}，则在首次访问时会执行必要的组件扫描，
// 并缓存扫描结果，之后每次根据给定的 {@link ImplementationDetectionConfiguration} 查找实现时都会过滤扫描结果。
// 如果初始未指定，则每次调用 {@link #detectCustomImplementation(ImplementationLookupConfiguration)} 时都会发起新的组件扫描。
public class CustomRepositoryImplementationDetector {

	private static final String CUSTOM_IMPLEMENTATION_RESOURCE_PATTERN = "**/*%s.class";
	private static final String AMBIGUOUS_CUSTOM_IMPLEMENTATIONS = "Ambiguous custom implementations detected; Found %s but expected a single implementation";

	private final Environment environment;
	private final ResourceLoader resourceLoader;
	private final Lazy<Set<BeanDefinition>> implementationCandidates;

	/**
	 * Creates a new {@link CustomRepositoryImplementationDetector} with the given {@link Environment},
	 * {@link ResourceLoader} and {@link ImplementationDetectionConfiguration}. The latter will be registered for a
	 * one-time component scan for implementation candidates that will the be used and filtered in all subsequent calls to
	 * {@link #detectCustomImplementation(ImplementationLookupConfiguration)}.
	 *
	 * @param environment must not be {@literal null}.
	 * @param resourceLoader must not be {@literal null}.
	 * @param configuration must not be {@literal null}.
	 */
	// 使用给定的 {@link Environment}、{@link ResourceLoader} 和 {@link ImplementationDetectionConfiguration}
	// 创建一个新的 {@link CustomRepositoryImplementationDetector}。
	// 后者将被注册用于一次性组件扫描，以查找实现候选对象，并在所有后续的
	// {@link #detectCustomImplementation(ImplementationLookupConfiguration)} 调用中使用这些候选对象并进行筛选。
	public CustomRepositoryImplementationDetector(Environment environment, ResourceLoader resourceLoader,
			ImplementationDetectionConfiguration configuration) {

		Assert.notNull(environment, "Environment must not be null");
		Assert.notNull(resourceLoader, "ResourceLoader must not be null");
		Assert.notNull(configuration, "ImplementationDetectionConfiguration must not be null");

		this.environment = environment;
		this.resourceLoader = resourceLoader;
		this.implementationCandidates = Lazy.of(() -> findCandidateBeanDefinitions(configuration));
	}

	/**
	 * Creates a new {@link CustomRepositoryImplementationDetector} with the given {@link Environment} and
	 * {@link ResourceLoader}. Calls to {@link #detectCustomImplementation(ImplementationLookupConfiguration)} will issue
	 * scans for
	 *
	 * @param environment must not be {@literal null}.
	 * @param resourceLoader must not be {@literal null}.
	 */
	public CustomRepositoryImplementationDetector(Environment environment, ResourceLoader resourceLoader) {

		Assert.notNull(environment, "Environment must not be null");
		Assert.notNull(resourceLoader, "ResourceLoader must not be null");

		this.environment = environment;
		this.resourceLoader = resourceLoader;
		this.implementationCandidates = Lazy.empty();
	}

	/**
	 * Tries to detect a custom implementation for a repository bean by classpath scanning.
	 *
	 * @param lookup must not be {@literal null}.
	 * @return the {@code AbstractBeanDefinition} of the custom implementation or {@literal null} if none found.
	 */
	// 尝试通过类路径扫描检测存储库 bean 的自定义实现。
	public Optional<AbstractBeanDefinition> detectCustomImplementation(ImplementationLookupConfiguration lookup) {

		Assert.notNull(lookup, "ImplementationLookupConfiguration must not be null");

		Set<BeanDefinition> definitions = implementationCandidates.getOptional()
				.orElseGet(() -> findCandidateBeanDefinitions(lookup)).stream() //
				.filter(lookup::matches) //
				.collect(StreamUtils.toUnmodifiableSet());

		return selectImplementationCandidate(lookup, definitions);
	}

	private static Optional<AbstractBeanDefinition> selectImplementationCandidate(
			ImplementationLookupConfiguration lookup, Set<BeanDefinition> definitions) {

		return SelectionSet //
				.of(definitions, c -> c.isEmpty() ? firstOrEmptyBeanDefinition(definitions) : throwAmbiguousCustomImplementationException(c)) //
				.filterIfNecessary(lookup::hasMatchingBeanName) //
				.uniqueResult() //
				.map(AbstractBeanDefinition.class::cast);
	}

	static Optional<BeanDefinition> firstOrEmptyBeanDefinition(Set<BeanDefinition> definitions) {
		return definitions.isEmpty() ? Optional.empty() : Optional.of(definitions.iterator().next());
	}

	private Set<BeanDefinition> findCandidateBeanDefinitions(ImplementationDetectionConfiguration config) {

		// 返回用于计算实现类型名称的后缀
		String postfix = config.getImplementationPostfix();

		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false,
				environment);
		provider.setResourceLoader(resourceLoader);
		provider.setResourcePattern(String.format(CUSTOM_IMPLEMENTATION_RESOURCE_PATTERN, postfix));
		provider.setMetadataReaderFactory(config.getMetadataReaderFactory());
		provider.addIncludeFilter((reader, factory) -> true);

		config.getExcludeFilters().forEach(it -> provider.addExcludeFilter(it));

		return config.getBasePackages().stream()//
				.flatMap(it -> provider.findCandidateComponents(it).stream())//
				.collect(Collectors.toSet());
	}

	private static Optional<BeanDefinition> throwAmbiguousCustomImplementationException(
			Collection<BeanDefinition> definitions) {

		String implementationNames = definitions.stream()//
				.map(BeanDefinition::getBeanClassName)//
				.collect(Collectors.joining(", "));

		throw new IllegalStateException(String.format(AMBIGUOUS_CUSTOM_IMPLEMENTATIONS, implementationNames));
	}
}
