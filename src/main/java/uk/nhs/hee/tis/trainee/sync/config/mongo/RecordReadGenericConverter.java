/*
 * The MIT License (MIT)
 *
 * Copyright 2021 Crown Copyright (Health Education England)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package uk.nhs.hee.tis.trainee.sync.config.mongo;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.bson.Document;
import org.springframework.context.ApplicationContext;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.trainee.sync.model.Record;

@Component
public class RecordReadGenericConverter implements GenericConverter {

  private final ApplicationContext context;

  RecordReadGenericConverter(ApplicationContext context) {
    this.context = context;
  }

  @Override
  public Set<ConvertiblePair> getConvertibleTypes() {
    return Collections.singleton(new ConvertiblePair(Document.class, Record.class));
  }

  @Override
  public Object convert(Object sourceObject, TypeDescriptor sourceType, TypeDescriptor targetType) {
    Record target = (Record) context.getBean(targetType.getType());
    Document source = (Document) sourceObject;

    target.setTisId(source.getString("_id"));

    Map<String, String> data = source.keySet().stream()
        .filter(key -> !key.startsWith("_"))
        .collect(Collectors.toMap(Function.identity(), source::getString));
    target.setData(data);

    return target;
  }
}
