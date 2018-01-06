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

package ratpack.http.client

import ratpack.groovy.test.embed.GroovyEmbeddedApp
import ratpack.http.MediaType
import spock.lang.AutoCleanup
import spock.lang.Specification

//Duplicate tests from https://github.com/apache/httpcomponents-client/blob/master/httpclient5/src/test/java/org/apache/hc/client5/http/entity/mime/TestMultipartForm.java
class MultiPartBodySpec extends Specification {

  @AutoCleanup
  def echoServer = GroovyEmbeddedApp.of {
    handlers {
      all {
        render request.body.map {
          it.text
        }
      }
    }
  }

  def "multiple string parts"() {
    given:
    def configure = MultiPartBody
      .builder()
      .type("multipart/form-data")
      .boundary("foo")
      .addTextPart("field1", "this stuff")
      .addTextPart("field2", "that stuff", MediaType.PLAIN_TEXT_UTF8)
      .addTextPart("field3", "all kind of stuff")
      .build()

    when:
    def response = echoServer.httpClient.request(configure)

    then:
    response.body.text == [
      '--foo',
      'Content-Disposition: form-data; name="field1"',
      'Content-Type: text/plain;charset=iso-8859-1',
      '',
      'this stuff',
      '--foo',
      'Content-Disposition: form-data; name="field2"',
      'Content-Type: text/plain;charset=utf-8',
      '',
      'that stuff',
      '--foo',
      'Content-Disposition: form-data; name="field3"',
      'Content-Type: text/plain;charset=iso-8859-1',
      '',
      'all kind of stuff',
      '--foo--'
    ].join("\r\n")
  }
}
