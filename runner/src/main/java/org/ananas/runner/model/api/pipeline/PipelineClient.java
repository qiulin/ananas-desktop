package org.ananas.runner.model.api.pipeline;

import org.ananas.runner.model.core.Pipeline;

import java.io.IOException;

public interface PipelineClient {

	Pipeline getPipeline(String id, String token, boolean getProjectId) throws IOException;
}