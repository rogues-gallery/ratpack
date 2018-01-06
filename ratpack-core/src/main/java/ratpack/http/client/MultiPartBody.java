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

package ratpack.http.client;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import ratpack.func.Action;

import java.util.ArrayList;
import java.util.List;

public class MultiPartBody {

  static Builder builder() {
    return new Builder();
  }

  enum Mode {

  }

  static class Part {
    private final String name;
    private final ByteBuf body;
    private final CharSequence type;

    public Part(String name, ByteBuf body, CharSequence type) {
      this.name = name;
      this.body = body;
      this.type = type;
    }

    public String getName() {
      return name;
    }

    public ByteBuf getBody() {
      return body;
    }

    public CharSequence getType() {
      return type;
    }

    public ByteBuf getPartBody() {
      CompositeByteBuf buffer = Unpooled.compositeBuffer();
      buffer.addComponent(true, Unpooled.wrappedBuffer(("Content-Disposition: form-data; name=\"" + name + "\"\r\n").getBytes()));
      buffer.addComponent(true, Unpooled.wrappedBuffer(("Content-Type: " + type + "\r\n\r\n").getBytes()));
      buffer.addComponent(true, body);
      buffer.addComponent(true, Unpooled.wrappedBuffer("\r\n".getBytes()));
      return buffer;
    }
  }

  static class Builder {

    private Mode mode;
    private CharSequence type;
    private CharSequence boundary;
    private List<Part> parts = new ArrayList<>();

    public Builder mode(Mode mode) {
      this.mode = mode;
      return this;
    }

    public Builder type(CharSequence contentType) {
      this.type = contentType;
      return this;
    }

    public Builder boundary(CharSequence boundary) {
      this.boundary = boundary;
      return this;
    }

    public Builder addPart(Part part) {
      parts.add(part);
      return this;
    }

    public Builder addTextPart(String name, CharSequence text, CharSequence type) {
      addPart(new Part(name, Unpooled.wrappedBuffer(String.valueOf(text).getBytes()), type));
      return this;
    }

    public Builder addTextPart(String name, CharSequence text) {
      return addTextPart(name, text, "text/plain;charset=iso-8859-1");
    }

    public Builder addBinaryPart(String name, byte[] bytes, CharSequence type) {
      return addBinaryPart(name, Unpooled.wrappedBuffer(bytes), type);
    }

    public Builder addBinaryPart(String name, byte[] bytes) {
      return addBinaryPart(name, bytes, "application/octet-stream");
    }

    public Builder addBinaryPart(String name, ByteBuf buffer, CharSequence type) {
      addPart(new Part(name, buffer, type));
      return this;
    }

    public Builder addBinaryPart(String name, ByteBuf buffer) {
      return addBinaryPart(name, buffer, "application/octet-steram");
    }

    private ByteBuf generateBody() {
      CompositeByteBuf buffers =  Unpooled.compositeBuffer();
      byte[] boundary = ("--" + this.boundary + "\r\n").getBytes();
      for (Part p : parts) {
        buffers.addComponent(true, Unpooled.wrappedBuffer(boundary));
        buffers.addComponent(true, p.getPartBody());
      }
      if (buffers.capacity() > 0) {
        buffers.addComponent(true, Unpooled.wrappedBuffer(("--" + this.boundary + "--").getBytes()));
      }
      return buffers;
    }

    public Action<? extends RequestSpec> build() {
      return spec -> {
        spec.body(body -> {
          if (type != null) {
            body.type(type);
//            body.bytes(generateBody().array());
            body.buffer(generateBody());
          }
        });
      };
    }
  }
}
