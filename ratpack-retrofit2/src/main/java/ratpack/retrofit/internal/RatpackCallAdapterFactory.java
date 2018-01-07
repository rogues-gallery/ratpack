/*
 * Copyright 2015 the original author or authors.
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

package ratpack.retrofit.internal;

import com.google.common.reflect.TypeToken;
import ratpack.exec.Promise;
import ratpack.func.Factory;
import ratpack.http.client.HttpClient;
import ratpack.http.client.HttpClientReadTimeoutException;
import ratpack.http.client.ReceivedResponse;
import ratpack.retrofit.RatpackRetrofitCallException;
import ratpack.retrofit.RequestOptions;
import ratpack.util.Exceptions;
import retrofit2.*;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;

public class RatpackCallAdapterFactory extends CallAdapter.Factory {

  private final Factory<? extends HttpClient> factory;

  private RatpackCallAdapterFactory(Factory<? extends HttpClient> factory) {
    this.factory = factory;
  }

  public static RatpackCallAdapterFactory with(Factory<? extends HttpClient> factory) {
    return new RatpackCallAdapterFactory(factory);
  }

  @Override
  public CallAdapter<?> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {
    Optional<RequestOptions> requestOptions = getRequestOptions(annotations);
    TypeToken<?> rawType = TypeToken.of(returnType);
    if (rawType.getRawType() != Promise.class) {
      return null;
    }
    if (!(returnType instanceof ParameterizedType)) {
      throw new IllegalStateException("Promise return type must be parameterized"
        + " as Promise<Foo> or Promise<? extends Foo>");
    }
    return getCallAdapter((ParameterizedType) returnType, requestOptions);
  }

  // returnType is the parameterization of Promise
  protected CallAdapter<Promise<?>> getCallAdapter(ParameterizedType returnType, Optional<RequestOptions> requestOptions) {
    Type parameterType = Utils.getSingleParameterUpperBound(returnType);
    TypeToken<?> parameterTypeToken = TypeToken.of(parameterType);
    //Promising a Response type, need the actual value
    if (parameterTypeToken.getRawType() == Response.class) {
      if (!(parameterType instanceof ParameterizedType)) {
        throw new IllegalStateException("Response return type must be parameterized"
          + " as Response<Foo> or Response<? extends Foo>");
      }
      Type responseType = Utils.getSingleParameterUpperBound((ParameterizedType) parameterType);
      return new ResponseCallAdapter(responseType, factory, requestOptions);
    } else if (parameterTypeToken.getRawType() == ReceivedResponse.class) {
      return new ReceivedResponseCallAdapter(parameterType, factory, requestOptions);
    }
    //Else we're just promising a value
    return new SimpleCallAdapter(parameterType, factory, requestOptions);
  }

  private Optional<RequestOptions> getRequestOptions(Annotation[] annotations) {
    for (Annotation a : annotations) {
      if (a.annotationType() == RequestOptions.class) {
        return Optional.of((RequestOptions) a);
      }
    }
    return Optional.empty();
  }

  static final class ReceivedResponseCallAdapter implements CallAdapter<Promise<?>> {

    private final Type responseType;
    private final ratpack.func.Factory<? extends HttpClient> factory;
    private final Optional<RequestOptions> requestOptions;

    ReceivedResponseCallAdapter(Type responseType, ratpack.func.Factory<? extends HttpClient> factory, Optional<RequestOptions> requestOptions) {
      this.responseType = responseType;
      this.factory = factory;
      this.requestOptions = requestOptions;
    }

    @Override
    public Type responseType() {
      return responseType;
    }

    @Override
    public <R> Promise<ReceivedResponse> adapt(Call<R> call) {
      return ((RatpackCallFactory.RatpackCall) RatpackCallFactory.with(factory).newCall(call.request())).with(requestOptions).promise();
    }
  }

  static final class ResponseCallAdapter implements CallAdapter<Promise<?>> {
    private final Type responseType;
    private final ratpack.func.Factory<? extends HttpClient> factory;
    private final Optional<RequestOptions> requestOptions;

    ResponseCallAdapter(Type responseType, ratpack.func.Factory<? extends HttpClient> factory, Optional<RequestOptions> requestOptions) {
      this.responseType = responseType;
      this.factory = factory;
      this.requestOptions = requestOptions;
    }

    @Override
    public Type responseType() {
      return responseType;
    }

    @Override
    public <R> Promise<Response<?>> adapt(Call<R> call) {

      return Promise.async(downstream ->
        call.enqueue(new Callback<R>() {

          @Override
          public void onResponse(Call<R> call, Response<R> response) {
            downstream.success(response);
          }

          @Override
          public void onFailure(Call<R> call, Throwable t) {
            if (t.getCause() instanceof HttpClientReadTimeoutException) {
              downstream.error(t.getCause());
            } else {
              downstream.error(t);
            }
          }
        })
      );
    }
  }

  static final class SimpleCallAdapter implements CallAdapter<Promise<?>> {
    private final Type responseType;
    private final ratpack.func.Factory<? extends HttpClient> factory;
    private final Optional<RequestOptions> requestOptions;

    SimpleCallAdapter(Type responseType, ratpack.func.Factory<? extends HttpClient> factory, Optional<RequestOptions> requestOptions) {
      this.responseType = responseType;
      this.factory = factory;
      this.requestOptions = requestOptions;
    }

    @Override
    public Type responseType() {
      return responseType;
    }

    @Override
    public <R> Promise<?> adapt(Call<R> call) {
      return Promise.async(downstream ->
        call.enqueue(new Callback<R>() {

          @Override
          public void onResponse(Call<R> call, Response<R> response) {
            if (response.isSuccessful()) {
              downstream.success(response.body());
            } else {
              Exceptions.uncheck(() ->
                downstream.error(RatpackRetrofitCallException.cause(call, response))
              );
            }
          }

          @Override
          public void onFailure(Call<R> call, Throwable t) {
            if (t.getCause() instanceof HttpClientReadTimeoutException) {
              downstream.error(t.getCause());
            } else {
              downstream.error(t);
            }
          }
        })
      );
    }
  }
}
