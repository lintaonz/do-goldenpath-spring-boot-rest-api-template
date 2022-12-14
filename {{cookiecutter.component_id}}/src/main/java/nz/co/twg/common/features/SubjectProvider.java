package nz.co.twg.common.features;

import java.util.function.Supplier;

/** A provider of subject on who performed the query on the feature flag. */
public interface SubjectProvider extends Supplier<String> {}
