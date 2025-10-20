/*
 * Copyright 2018-2025 the original author or authors.
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

/**
 * Enumeration to define in which way repositories are bootstrapped.
 *
 * @author Oliver Gierke
 * @see RepositoryConfigurationSource#getBootstrapMode()
 * @since 2.1
 * @soundtrack Dave Matthews Band - She (Come Tomorrow)
 */
// 枚举来定义存储库的引导方式。
public enum BootstrapMode {

	/**
	 * Repository proxies are instantiated eagerly, just like any other Spring bean, except explicitly marked as lazy.
	 * Thus, injection into repository clients will trigger initialization.
	 */
	// 存储库代理会像其他 Spring bean 一样立即实例化，除非明确标记为延迟实例化。因此，注入存储库客户端将触发初始化。
	DEFAULT,

	/**
	 * Repository bean definitions are considered lazy and clients will get repository proxies injected that will
	 * initialize on first access. Repository initialization is triggered on application context bootstrap completion.
	 */
	// 存储库 bean 定义被视为延迟实例化，客户端将注入存储库代理，并在首次访问时进行初始化。存储库初始化在应用上下文引导完成时触发。
	DEFERRED,

	/**
	 * Repository bean definitions are considered lazy, lazily injected and initialized only on first use, i.e., the
	 * application might have fully started without the repositories initialized.
	 */
	// 存储库 bean 定义被视为延迟实例化，仅在首次使用时延迟注入和初始化，也就是说，应用程序可能在存储库尚未初始化的情况下就已完全启动。
	LAZY;
}
