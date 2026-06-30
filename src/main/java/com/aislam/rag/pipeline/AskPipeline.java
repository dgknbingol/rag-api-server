package com.aislam.rag.pipeline;

import com.aislam.rag.dto.AskResponse;

public interface AskPipeline {

    String id();

    AskResponse ask(String question);
}
