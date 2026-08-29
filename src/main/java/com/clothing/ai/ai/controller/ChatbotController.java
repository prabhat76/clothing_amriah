package com.clothing.ai.ai.controller;

import com.clothing.ai.ai.chatbot.ChatbotService;
import com.clothing.ai.ai.chatbot.ChatbotService.*;
import com.clothing.ai.common.response.ApiResponse;
import com.clothing.ai.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Conversational shopping assistant — "Clothie".
 */
@RestController
@RequestMapping("/ai/chatbot")
@RequiredArgsConstructor
@Tag(name = "AI - Chatbot",
        description = "Conversational AI shopping assistant (Clothie) with multi-turn session memory")
@SecurityRequirement(name = "BearerAuth")
public class ChatbotController {

    private final ChatbotService chatbot;

    @Operation(
            summary = "Send a message to Clothie",
            description = """
                    Sends a chat message to the AI shopping assistant and receives a reply.

                    Clothie can:
                    - Answer questions about specific products
                    - Recommend outfits and combinations
                    - Help with size and fit queries
                    - Explain return/shipping policies

                    **Session memory:** pass `sessionId` from a previous response to continue a conversation.
                    Omit `sessionId` to start a new session.

                    Responses are streamed internally; the HTTP response is returned once complete.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "AI response returned",
                    content = @Content(schema = @Schema(implementation = ChatResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "sessionId": "550e8400-e29b-41d4-a716-446655440000",
                                        "message": "I'd recommend the Classic Slim-Fit Chinos in Navy — they pair beautifully with the Oxford shirt you were looking at.",
                                        "suggestedProducts": [{ "id": "…", "name": "Classic Slim-Fit Chinos" }]
                                      }
                                    }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Empty message"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "503", description = "AI service temporarily unavailable")
    })
    @PostMapping("/chat")
    public ApiResponse<ChatResponse> chat(@RequestBody ChatRequest req) {
        return ApiResponse.success(chatbot.chat(SecurityUtils.currentUserId(), req));
    }

    @Operation(
            summary = "Clear a chat session",
            description = """
                    Deletes the conversation history for the given session UUID.
                    The next message with this session ID will start a fresh conversation.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Session cleared"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Session not found")
    })
    @DeleteMapping("/sessions/{sessionId}")
    public ApiResponse<Void> clear(
            @Parameter(description = "Chat session UUID") @PathVariable UUID sessionId) {
        chatbot.clearSession(sessionId);
        return ApiResponse.success("Session cleared", null);
    }

    @Operation(
            summary = "List active chat sessions for the current user",
            description = "Returns all open chat session IDs for the authenticated user.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Session list returned")
    })
    @GetMapping("/sessions")
    public ApiResponse<java.util.List<UUID>> sessions() {
        return ApiResponse.success(chatbot.activeSessions(SecurityUtils.currentUserId()));
    }
}
