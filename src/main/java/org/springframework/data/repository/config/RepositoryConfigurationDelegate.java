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

import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.AutowireCandidateResolver;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ContextAnnotationAutowireCandidateResolver;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.Environment;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.log.LogMessage;
import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.core.metrics.StartupStep;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.AbstractRepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.util.ClassUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StopWatch;

/**
 * Delegate for configuration integration to reuse the general way of detecting repositories. Customization is done by
 * providing a configuration format specific {@link RepositoryConfigurationSource} (currently either XML or annotations
 * are supported). The actual registration can then be triggered for different {@link RepositoryConfigurationExtension}
 * s.
 *
 * @author Oliver Gierke
 * @author Jens Schauder
 * @author Mark Paluch
 * @author Christoph Strobl
 * @author John Blum
 */
public class RepositoryConfigurationDelegate {

	private static final String REPOSITORY_REGISTRATION = "Spring Data %s - Registering repository: %s - Interface: %s - Factory: %s";
	private static final String MULTIPLE_MODULES = "Multiple Spring Data modules found, entering strict repository configuration mode";
	private static final String NON_DEFAULT_AUTOWIRE_CANDIDATE_RESOLVER = "Non-default AutowireCandidateResolver (%s) detected. Skipping the registration of LazyRepositoryInjectionPointResolver. Lazy repository injection will not be working";

	private static final List<Class<?>> DEFAULT_AUTOWIRE_CANDIDATE_RESOLVERS = List
			.of(ContextAnnotationAutowireCandidateResolver.class, LazyRepositoryInjectionPointResolver.class);

	private static final Log logger = LogFactory.getLog(RepositoryConfigurationDelegate.class);

	private final RepositoryConfigurationSource configurationSource;
	private final ResourceLoader resourceLoader;
	private final Environment environment;
	private final boolean isXml;
	private final boolean inMultiStoreMode;

	/**
	 * Creates a new {@link RepositoryConfigurationDelegate} for the given {@link RepositoryConfigurationSource} and
	 * {@link ResourceLoader} and {@link Environment}.
	 *
	 * @param configurationSource must not be {@literal null}.
	 * @param resourceLoader must not be {@literal null}.
	 * @param environment must not be {@literal null}.
	 */
	// 为给定的 {@link RepositoryConfigurationSource} 和 {@link ResourceLoader} 和 {@link Environment} 创建一个新的 {@link RepositoryConfigurationDelegate}。
	public RepositoryConfigurationDelegate(RepositoryConfigurationSource configurationSource,
			ResourceLoader resourceLoader, Environment environment) {

		// configurationSource 是否是基于 XML 的 RepositoryConfigurationSource
		this.isXml = configurationSource instanceof XmlRepositoryConfigurationSource;
		boolean isAnnotation = configurationSource instanceof AnnotationRepositoryConfigurationSource;

		Assert.isTrue(isXml || isAnnotation,
				"Configuration source must either be an Xml- or an AnnotationBasedConfigurationSource");
		Assert.notNull(resourceLoader, "ResourceLoader must not be null");

		this.configurationSource = configurationSource;
		this.resourceLoader = resourceLoader;
		this.environment = defaultEnvironment(environment, resourceLoader);
		this.inMultiStoreMode = multipleStoresDetected();
	}

	/**
	 * Defaults the environment in case the given one is null. Used as fallback, in case the legacy constructor was
	 * invoked.
	 *
	 * @param environment can be {@literal null}.
	 * @param resourceLoader can be {@literal null}.
	 * @return the given {@link Environment} if not {@literal null}, a configured {@link Environment}, or a default
	 *         {@link Environment}.
	 */
	private static Environment defaultEnvironment(@Nullable Environment environment,
			@Nullable ResourceLoader resourceLoader) {

		if (environment != null) {
			return environment;
		}

		return resourceLoader instanceof EnvironmentCapable capable ? capable.getEnvironment() : new StandardEnvironment();
	}

	/**
	 * Registers the discovered repositories in the given {@link BeanDefinitionRegistry}.
	 *
	 * @param registry {@link BeanDefinitionRegistry} in which to register the repository bean.
	 * @param extension {@link RepositoryConfigurationExtension} for the module.
	 * @return {@link BeanComponentDefinition}s for all repository bean definitions found.
	 * @see org.springframework.data.repository.config.RepositoryConfigurationExtension
	 * @see org.springframework.beans.factory.support.BeanDefinitionRegistry
	 */
	// 在给定的 {@link BeanDefinitionRegistry} 中注册发现的存储库。
	public List<BeanComponentDefinition> registerRepositoriesIn(BeanDefinitionRegistry registry,
			RepositoryConfigurationExtension extension) {

		if (logger.isInfoEnabled()) { // 以 %s 模式引导 Spring Data %s 存储库。
			logger.info(LogMessage.format("Bootstrapping Spring Data %s repositories in %s mode.", //
					extension.getModuleName(), configurationSource.getBootstrapMode().name()));
		}

		// 用于为仓库根节点注册其他 bean 定义的回调。
		extension.registerBeansForRoot(registry, configurationSource);

		// 从给定的 BeanDefinitionRegistry、RepositoryConfigurationExtension 和 ResourceLoader 创建一个新的 RepositoryBeanDefinitionBuilder。
		RepositoryBeanDefinitionBuilder builder = new RepositoryBeanDefinitionBuilder(registry, extension,
				configurationSource, resourceLoader, environment);

		if (logger.isDebugEnabled()) { // 正在扫描包 %s 中的 %s 存储库
			logger.debug(LogMessage.format("Scanning for %s repositories in packages %s.", //
					extension.getModuleName(), //
					configurationSource.getBasePackages().stream().collect(Collectors.joining(", "))));
		}

		StopWatch watch = new StopWatch();
		ApplicationStartup startup = getStartup(registry);
		StartupStep repoScan = startup.start("spring.data.repository.scanning");

		repoScan.tag("dataModule", extension.getModuleName());
		repoScan.tag("basePackages",
				() -> configurationSource.getBasePackages().stream().collect(Collectors.joining(", ")));
		watch.start();

		// 返回通过给定的 RepositoryConfigurationSource 获得的所有 RepositoryConfiguration
		Collection<RepositoryConfiguration<RepositoryConfigurationSource>> configurations = extension
				.getRepositoryConfigurations(configurationSource, resourceLoader, inMultiStoreMode);

		List<BeanComponentDefinition> definitions = new ArrayList<>();

		// 按存储库名称存储配置
		Map<String, RepositoryConfiguration<?>> configurationsByRepositoryName = new HashMap<>(configurations.size());
		// 按存储库名称存储元数据
		Map<String, RepositoryConfigurationAdapter<?>> metadataByRepositoryBeanName = new HashMap<>(configurations.size());

		// RepositoryConfiguration --> 单个存储库实例的配置信息
		for (RepositoryConfiguration<? extends RepositoryConfigurationSource> configuration : configurations) {

			// key: 存储库的接口名称；value：RepositoryConfiguration
			configurationsByRepositoryName.put(configuration.getRepositoryInterface(), configuration);

			// 从给定的 BeanDefinitionRegistry 和 ResourceLoader 构建一个新的 BeanDefinitionBuilder
			BeanDefinitionBuilder definitionBuilder = builder.build(configuration);
			// 回调以对 BeanDefinition 进行后期处理，并在必要时调整配置。
			extension.postProcess(definitionBuilder, configurationSource);

			if (isXml) {
				// 回调以对从 XML 构建的 BeanDefinition 进行后期处理，并在必要时调整配置。
				extension.postProcess(definitionBuilder, (XmlRepositoryConfigurationSource) configurationSource);
			} else {
				// 回调以对由注释构建的 BeanDefinition 进行后期处理，并在必要时调整配置。
				extension.postProcess(definitionBuilder, (AnnotationRepositoryConfigurationSource) configurationSource);
			}

			RootBeanDefinition beanDefinition = (RootBeanDefinition) definitionBuilder.getBeanDefinition();
			// getRepositoryFactoryBeanType() --> 从给定的 RepositoryConfiguration 返回存储库工厂 bean 类型作为加载的 Class。
			beanDefinition.setTargetType(getRepositoryFactoryBeanType(configuration));
			beanDefinition.setResourceDescription(configuration.getResourceDescription());

			// 返回 beanDefinition 的名称
			String beanName = configurationSource.generateBeanName(beanDefinition);

			if (logger.isTraceEnabled()) {
				logger.trace(LogMessage.format(REPOSITORY_REGISTRATION, extension.getModuleName(), beanName,
						configuration.getRepositoryInterface(), configuration.getRepositoryFactoryBeanClassName()));
			}

			metadataByRepositoryBeanName.put(beanName, builder.buildMetadata(configuration));
			registry.registerBeanDefinition(beanName, beanDefinition);
			definitions.add(new BeanComponentDefinition(beanDefinition, beanName));
		}

		// 在默认的 ContextAnnotationAutowireCandidateResolver 上注册一个 LazyRepositoryInjectionPointResolver，使惰性存储库的注入点也变为惰性。
		potentiallyLazifyRepositories(configurationsByRepositoryName, registry, configurationSource.getBootstrapMode());

		watch.stop();
		repoScan.tag("repository.count", Integer.toString(configurations.size()));
		repoScan.end();

		if (logger.isInfoEnabled()) {
			logger.info(
					LogMessage.format("Finished Spring Data repository scanning in %s ms. Found %s %s repository interface%s.",
							watch.lastTaskInfo().getTimeMillis(), configurations.size(), extension.getModuleName(),
							configurations.size() == 1 ? "" : "s"));
		}

		// TODO: AOT Processing -> guard this one with a flag so it's not always present
		// TODO: With regard to AOT Processing, perhaps we need to be smart and detect whether "core" AOT components are
		// (or rather configuration is) present on the classpath to enable Spring Data AOT component registration.
		// --> 译文：
		// TODO：AOT 处理 -> 使用标志保护此组件，使其并非始终存在
		// TODO：关于 AOT 处理，也许我们需要更智能地检测“核心”AOT 组件
		// （或者更确切地说是配置）是否存在于类路径中，以启用 Spring Data AOT 组件注册。
		registerAotComponents(registry, extension, metadataByRepositoryBeanName);

		return definitions;
	}

	private void registerAotComponents(BeanDefinitionRegistry registry, RepositoryConfigurationExtension extension,
			Map<String, RepositoryConfigurationAdapter<?>> metadataByRepositoryBeanName) {

		BeanDefinitionBuilder repositoryAotProcessor = BeanDefinitionBuilder
				.rootBeanDefinition(extension.getRepositoryAotProcessor()).setRole(BeanDefinition.ROLE_INFRASTRUCTURE);

		repositoryAotProcessor.addPropertyValue("configMap", metadataByRepositoryBeanName);

		// module-specific repository aot processor
		String repositoryAotProcessorBeanName = String.format("data-%s.repository-aot-processor",
				extension.getModuleIdentifier());
		registry.registerBeanDefinition(BeanDefinitionReaderUtils.uniqueBeanName(repositoryAotProcessorBeanName, registry),
				repositoryAotProcessor.getBeanDefinition());
	}

	/**
	 * Registers a {@link LazyRepositoryInjectionPointResolver} over the default
	 * {@link ContextAnnotationAutowireCandidateResolver} to make injection points of lazy repositories lazy, too. Will
	 * augment the {@link LazyRepositoryInjectionPointResolver}'s configuration if there already is one configured.
	 *
	 * @param configurations must not be {@literal null}.
	 * @param registry must not be {@literal null}.
	 */
	// 在默认的 {@link ContextAnnotationAutowireCandidateResolver} 上注册一个 {@link LazyRepositoryInjectionPointResolver}，使惰性存储库的注入点也变为惰性。
	// 如果已配置了 {@link LazyRepositoryInjectionPointResolver} 的配置，则会对其进行扩展。
	private static void potentiallyLazifyRepositories(Map<String, RepositoryConfiguration<?>> configurations,
			BeanDefinitionRegistry registry, BootstrapMode mode) {

		if (!(registry instanceof DefaultListableBeanFactory beanFactory) || BootstrapMode.DEFAULT.equals(mode)) {
			return;
		}

		AutowireCandidateResolver resolver = beanFactory.getAutowireCandidateResolver();

		if (!DEFAULT_AUTOWIRE_CANDIDATE_RESOLVERS.contains(resolver.getClass())) {

			logger.warn(LogMessage.format(NON_DEFAULT_AUTOWIRE_CANDIDATE_RESOLVER, resolver.getClass().getName()));
			return;
		}

		AutowireCandidateResolver newResolver = resolver instanceof LazyRepositoryInjectionPointResolver lazy //
				? lazy.withAdditionalConfigurations(configurations) //
				: new LazyRepositoryInjectionPointResolver(configurations);

		beanFactory.setAutowireCandidateResolver(newResolver);

		if (mode.equals(BootstrapMode.DEFERRED)
				&& !beanFactory.containsBean(DeferredRepositoryInitializationListener.class.getName())) {

			logger.debug("Registering deferred repository initialization listener.");
			beanFactory.registerSingleton(DeferredRepositoryInitializationListener.class.getName(),
					new DeferredRepositoryInitializationListener(beanFactory));
		}
	}

	/**
	 * Scans {@code repository.support} packages for implementations of {@link RepositoryFactorySupport}. Finding more
	 * than a single type is considered a multi-store configuration scenario which will trigger stricter repository
	 * scanning.
	 *
	 * @return {@literal true} if multiple data store repository implementations are present in the application. This
	 *         typically means a Spring application is using more than 1 type of data store.
	 */
	private boolean multipleStoresDetected() {

		boolean multipleModulesFound = SpringFactoriesLoader
				.loadFactoryNames(RepositoryFactorySupport.class, resourceLoader.getClassLoader()).size() > 1;

		if (multipleModulesFound) {
			logger.info(MULTIPLE_MODULES);
		}

		return multipleModulesFound;
	}

	private static ApplicationStartup getStartup(BeanDefinitionRegistry registry) {

		if (registry instanceof ConfigurableBeanFactory) {
			return ((ConfigurableBeanFactory) registry).getApplicationStartup();
		}

		if (registry instanceof GenericApplicationContext) {
			return ((GenericApplicationContext) registry).getDefaultListableBeanFactory().getApplicationStartup();
		}

		return ApplicationStartup.DEFAULT;
	}

	/**
	 * Returns the repository factory bean type from the given {@link RepositoryConfiguration} as loaded {@link Class}.
	 *
	 * @param configuration must not be {@literal null}.
	 * @return can be {@literal null}.
	 */
	// 从给定的 {@link RepositoryConfiguration} 返回存储库工厂 bean 类型作为加载的 {@link Class}。
	@Nullable
	private ResolvableType getRepositoryFactoryBeanType(RepositoryConfiguration<?> configuration) {

		// 返回存储库的接口名称
		String interfaceName = configuration.getRepositoryInterface();
		ClassLoader classLoader = resourceLoader.getClassLoader() == null
				? org.springframework.util.ClassUtils.getDefaultClassLoader()
				: resourceLoader.getClassLoader();

		classLoader = classLoader != null ? classLoader : getClass().getClassLoader();

		// 使用给定的 ClassLoader 加载具有给定名称的类。
		Class<?> repositoryInterface = ClassUtils.loadIfPresent(interfaceName, classLoader);

		if (repositoryInterface == null) {
			return null;
		}

		// 返回要使用的存储库工厂 bean 类的名称。
		Class<?> factoryBean = ClassUtils.loadIfPresent(configuration.getRepositoryFactoryBeanClassName(), classLoader);

		if (factoryBean == null) {
			return null;
		}

		// 为给定的存储库接口创建一个新的 RepositoryMetadata
		RepositoryMetadata metadata = AbstractRepositoryMetadata.getMetadata(repositoryInterface);
		List<Class<?>> types = List.of(repositoryInterface, metadata.getDomainType(), metadata.getIdType());

		ResolvableType[] declaredGenerics = ResolvableType.forClass(factoryBean).getGenerics();
		ResolvableType[] parentGenerics = ResolvableType.forClass(RepositoryFactoryBeanSupport.class, factoryBean)
				.getGenerics();
		List<ResolvableType> resolvedGenerics = new ArrayList<>(factoryBean.getTypeParameters().length);

		for (int i = 0; i < parentGenerics.length; i++) {

			ResolvableType parameter = parentGenerics[i];

			if (parameter.getType() instanceof TypeVariable<?>) {
				resolvedGenerics.add(i < types.size() ? ResolvableType.forClass(types.get(i)) : parameter);
			}
		}

		if (resolvedGenerics.size() < declaredGenerics.length) {
			resolvedGenerics.addAll(Arrays.asList(declaredGenerics).subList(parentGenerics.length, declaredGenerics.length));
		}

		return ResolvableType.forClassWithGenerics(factoryBean,
				resolvedGenerics.subList(0, declaredGenerics.length).toArray(ResolvableType[]::new));
	}

	/**
	 * Customer {@link ContextAnnotationAutowireCandidateResolver} that also considers all injection points for lazy
	 * repositories lazy.
	 *
	 * @author Oliver Gierke
	 * @since 2.1
	 */
	// 客户 {@link ContextAnnotationAutowireCandidateResolver} 也认为所有惰性存储库的注入点都是惰性的。
	static class LazyRepositoryInjectionPointResolver extends ContextAnnotationAutowireCandidateResolver {

		private static final Log logger = LogFactory.getLog(LazyRepositoryInjectionPointResolver.class);

		private final Map<String, RepositoryConfiguration<?>> configurations;

		public LazyRepositoryInjectionPointResolver(Map<String, RepositoryConfiguration<?>> configurations) {
			this.configurations = configurations;
		}

		/**
		 * Returns a new {@link LazyRepositoryInjectionPointResolver} that will have its configurations augmented with the
		 * given ones.
		 *
		 * @param configurations must not be {@literal null}.
		 * @return a new {@link LazyRepositoryInjectionPointResolver} that will have its configurations augmented with the
		 *         given ones.
		 */
		LazyRepositoryInjectionPointResolver withAdditionalConfigurations(
				Map<String, RepositoryConfiguration<?>> configurations) {

			Map<String, RepositoryConfiguration<?>> map = new HashMap<>(this.configurations);
			map.putAll(configurations);

			return new LazyRepositoryInjectionPointResolver(map);
		}

		@Override
		protected boolean isLazy(DependencyDescriptor descriptor) {

			Class<?> type = descriptor.getDependencyType();

			RepositoryConfiguration<?> configuration = configurations.get(type.getName());

			if (configuration == null) {
				return super.isLazy(descriptor);
			}

			boolean lazyInit = configuration.isLazyInit();

			if (lazyInit) {
				logger
						.debug(LogMessage.format("Creating lazy injection proxy for %s…", configuration.getRepositoryInterface()));
			}

			return lazyInit;
		}

	}
}
