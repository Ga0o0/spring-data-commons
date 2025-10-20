/*
 * Copyright 2012-2025 the original author or authors.
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

import java.lang.annotation.Annotation;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

/**
 * Base class to implement {@link ImportBeanDefinitionRegistrar}s to enable repository
 *
 * @author Oliver Gierke
 */
public abstract class RepositoryBeanDefinitionRegistrarSupport
		implements ImportBeanDefinitionRegistrar, ResourceLoaderAware, EnvironmentAware {

	private @SuppressWarnings("null") @NonNull ResourceLoader resourceLoader;
	private @SuppressWarnings("null") @NonNull Environment environment;

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	/**
	 * Forwarding to {@link #registerBeanDefinitions(AnnotationMetadata, BeanDefinitionRegistry, BeanNameGenerator)} for
	 * backwards compatibility reasons so that tests in downstream modules do not accidentally invoke the super type's
	 * default implementation.
	 *
	 * @see org.springframework.context.annotation.ImportBeanDefinitionRegistrar#registerBeanDefinitions(org.springframework.core.type.AnnotationMetadata,
	 *      org.springframework.beans.factory.support.BeanDefinitionRegistry)
	 * @deprecated since 2.2, call
	 *             {@link #registerBeanDefinitions(AnnotationMetadata, BeanDefinitionRegistry, BeanNameGenerator)}
	 *             instead.
	 * @see ConfigurationClassPostProcessor#IMPORT_BEAN_NAME_GENERATOR
	 */
	// 出于向后兼容的原因，转发到 {@link #registerBeanDefinitions(AnnotationMetadata, BeanDefinitionRegistry, BeanNameGenerator)}，以便下游模块中的测试不会意外调用超类型的默认实现。
	@Override
	@Deprecated
	public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
		registerBeanDefinitions(metadata, registry, ConfigurationClassPostProcessor.IMPORT_BEAN_NAME_GENERATOR);
	}

	@Override
	public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry,
			BeanNameGenerator generator) {

		Assert.notNull(metadata, "AnnotationMetadata must not be null");
		Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
		Assert.notNull(resourceLoader, "ResourceLoader must not be null");

		// Guard against calls for sub-classes --> 译文：防止调用子类
		if (metadata.getAnnotationAttributes(getAnnotation().getName()) == null) {
			return;
		}

		// 根据给定的 AnnotationMetadata 和注释创建一个新的 AnnotationRepositoryConfigurationSource。
		AnnotationRepositoryConfigurationSource configurationSource = new AnnotationRepositoryConfigurationSource(metadata,
				getAnnotation(), resourceLoader, environment, registry, generator);

		// 返回 RepositoryConfigurationExtension 用于存储特定的回调和 BeanDefinition 后处理。
		RepositoryConfigurationExtension extension = getExtension();
		// 注册给定的 RepositoryConfigurationExtension 以指示特定存储（通过扩展的具体类型表示）的存储库配置已发生。
		RepositoryConfigurationUtils.exposeRegistration(extension, registry, configurationSource);

		// 为给定的 RepositoryConfigurationSource、ResourceLoader 和 Environment 创建一个新的 RepositoryConfigurationDelegate。
		RepositoryConfigurationDelegate delegate = new RepositoryConfigurationDelegate(configurationSource, resourceLoader,
				environment);

		// 在给定的 BeanDefinitionRegistry 中注册发现的存储库。
		delegate.registerRepositoriesIn(registry, extension);
	}

	/**
	 * Return the annotation to obtain configuration information from. Will be wrappen into an
	 * {@link AnnotationRepositoryConfigurationSource} so have a look at the constants in there for what annotation
	 * attributes it expects.
	 *
	 * @return
	 */
	protected abstract Class<? extends Annotation> getAnnotation();

	/**
	 * Returns the {@link RepositoryConfigurationExtension} for store specific callbacks and {@link BeanDefinition}
	 * post-processing.
	 *
	 * @see RepositoryConfigurationExtensionSupport
	 * @return
	 */
	// 返回 {@link RepositoryConfigurationExtension} 用于存储特定的回调和 {@link BeanDefinition} 后处理。
	protected abstract RepositoryConfigurationExtension getExtension();
}
