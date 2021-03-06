/**
 * Copyright 2017 Confluent Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/

package io.confluent.ksql.parser.tree;

import io.confluent.ksql.GenericRow;
import io.confluent.ksql.function.UdafAggregator;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.streams.kstream.Initializer;
import org.apache.kafka.streams.kstream.KGroupedStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.TimeWindows;

public class HoppingWindowExpression extends KsqlWindowExpression {

  private final long size;
  private final TimeUnit sizeUnit;
  private final long advanceBy;
  private final TimeUnit advanceByUnit;

  public HoppingWindowExpression(
      final long size,
      final TimeUnit sizeUnit,
      final long advanceBy,
      final TimeUnit advanceByUnit
  ) {
    this(Optional.empty(), size, sizeUnit, advanceBy, advanceByUnit);
  }

  private HoppingWindowExpression(
      final Optional<NodeLocation> location,
      final long size,
      final TimeUnit sizeUnit,
      final long advanceBy,
      final TimeUnit advanceByUnit
  ) {
    super(location);
    this.size = size;
    this.sizeUnit = sizeUnit;
    this.advanceBy = advanceBy;
    this.advanceByUnit = advanceByUnit;
  }

  public long getSize() {
    return size;
  }

  public TimeUnit getSizeUnit() {
    return sizeUnit;
  }

  public long getAdvanceBy() {
    return advanceBy;
  }

  public TimeUnit getAdvanceByUnit() {
    return advanceByUnit;
  }

  @Override
  public <R, C> R accept(final AstVisitor<R, C> visitor, final C context) {
    return visitor.visitHoppingWindowExpression(this, context);
  }

  @Override
  public String toString() {
    return " HOPPING ( SIZE " + size + " " + sizeUnit + " , ADVANCE BY "
        + advanceBy + " " + "" + advanceByUnit + " ) ";
  }

  @Override
  public int hashCode() {
    return Objects.hash(size, sizeUnit, advanceBy, advanceByUnit);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final HoppingWindowExpression hoppingWindowExpression = (HoppingWindowExpression) o;
    return hoppingWindowExpression.size == size && hoppingWindowExpression.sizeUnit == sizeUnit
        && hoppingWindowExpression.advanceBy == advanceBy && hoppingWindowExpression
        .advanceByUnit == advanceByUnit;
  }

  @SuppressWarnings("unchecked")
  @Override
  public KTable applyAggregate(
      final KGroupedStream groupedStream,
      final Initializer initializer,
      final UdafAggregator aggregator,
      final Materialized<String, GenericRow, ?> materialized
  ) {
    final TimeWindows windows = TimeWindows
        .of(Duration.ofMillis(sizeUnit.toMillis(size)))
        .advanceBy(Duration.ofMillis(advanceByUnit.toMillis(advanceBy)));

    return groupedStream
        .windowedBy(windows)
        .aggregate(initializer, aggregator, materialized);
  }
}
