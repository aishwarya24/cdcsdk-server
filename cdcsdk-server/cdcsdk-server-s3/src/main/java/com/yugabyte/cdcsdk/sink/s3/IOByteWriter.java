/*
 * Copyright 2022 Confluent Inc.
 *
 * Licensed under the Confluent Community License (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 *
 * http://www.confluent.io/confluent-community-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.yugabyte.cdcsdk.sink.s3;

import java.io.IOException;

/**
 * Interface which duplicates RecordWriter, yet with
 * IOException throwing signatures.
 */
public interface IOByteWriter {
    void write(byte[] bytes) throws IOException;

    void write(byte[] bytes, int offset, int length) throws IOException;

    void close() throws IOException;

    void commit() throws IOException;
}