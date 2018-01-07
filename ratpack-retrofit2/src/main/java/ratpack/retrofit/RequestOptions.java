/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ratpack.retrofit;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Configure the options for the underlying {@link ratpack.http.client.HttpClient} when making a request using this client.
 *
 * @see RatpackRetrofit
 * @since 1.6
 */
@Documented
@Target(METHOD)
@Retention(RUNTIME)
public @interface RequestOptions {

  /**
   * If non-zero, configures the underlying {@link ratpack.http.client.RequestSpec} with the specified connection timeout in milliseconds.
   *
   * @return the configured connection timeout in milliseconds
   */
  int connectTimeoutInMillis() default -1;

  /**
   * If non-zero, configures the underlying {@link ratpack.http.client.RequestSpec} with the specified read timeout in milliseconds.
   *
   * @return the configured read timeout in milliseconds
   */
  int readTimeoutInMillis() default -1;
}
