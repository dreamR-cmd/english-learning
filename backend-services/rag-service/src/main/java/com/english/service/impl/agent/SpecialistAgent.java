package com.english.service.impl.agent;

import com.english.dto.RagAnswerResponse;
import com.english.service.RagService;

public interface SpecialistAgent {
    RagAnswerResponse ask(AgentRequestContext context);

    void askStream(AgentRequestContext context, RagService.RagStreamHandler handler, Runnable fallback);
}
