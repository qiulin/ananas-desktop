package org.ananas.runner.kernel.paginate;

import org.ananas.runner.kernel.model.Dataframe;
import org.apache.beam.sdk.schemas.Schema;
import org.apache.beam.sdk.values.Row;
import org.apache.commons.lang3.tuple.MutablePair;

public interface Paginator {

  MutablePair<Schema, Iterable<Row>> paginateRows(Integer page, Integer pageSize);

  Dataframe paginate(Integer page, Integer pageSize);
}
