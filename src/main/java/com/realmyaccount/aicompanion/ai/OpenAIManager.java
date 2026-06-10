package com.realmyaccount.aicompanion.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class OpenAIManager
{
    private static final Logger LOGGER = LogManager.getLogger(OpenAIManager.class);
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private final String apiKey;
    private final OkHttpClient httpClient;
    private static OpenAIManager instance;

    private OpenAIManager(String apiKey)
    {
        this.apiKey = apiKey;
        this.httpClient = new OkHttpClient();
    }

    public static OpenAIManager getInstance(String apiKey)
    {
        if (instance == null)
        {
            instance = new OpenAIManager(apiKey);
        }
        return instance;
    }

    public String getAIResponse(String worldState, String playerCommand) throws IOException
    {
        String systemPrompt = buildSystemPrompt();
        String userMessage = buildUserMessage(worldState, playerCommand);

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", "gpt-4-turbo-preview");
        requestBody.addProperty("temperature", 0.7);
        requestBody.addProperty("max_tokens", 500);

        JsonArray messages = new JsonArray();

        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", systemPrompt);
        messages.add(systemMsg);

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", userMessage);
        messages.add(userMsg);

        requestBody.add("messages", messages);

        RequestBody body = RequestBody.create(
                MediaType.parse("application/json"),
                requestBody.toString()
        );

        Request request = new Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute())
        {
            if (!response.isSuccessful())
            {
                LOGGER.error("OpenAI API Error: " + response.code() + " - " + response.body().string());
                return "ACTION:idle";
            }

            String responseBody = response.body().string();
            JsonObject responseJson = JsonParser.parseString(responseBody).getAsJsonObject();
            return responseJson.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();
        }
    }

    private String buildSystemPrompt()
    {
        return "You are an AI companion in Minecraft survival mode. You must respond with ONLY action commands in this format:\\nACTION:command\\n\\nAvailable actions:\\n- move_forward / move_backward / move_left / move_right\\n- jump / crouch / sprint\\n- look_up / look_down / look_left / look_right\\n- mine / attack / interact / place_block\\n- use_item / open_inventory / craft\\n- chat:<message>\\n- idle\\n\\nBe smart, observe the world state, and act like a real player. You roleplay as a friendly AI companion helping the player survive. You can talk through chat command when appropriate. Always prioritize safety and cooperation with the player.";
    }

    private String buildUserMessage(String worldState, String playerCommand)
    {
        return "World State:\\n" + worldState + "\\n\\nPlayer Command: " + (playerCommand.isEmpty() ? "Continue your current task or explore" : playerCommand) + "\\n\\nWhat do you do next? Respond with ACTION: command";
    }
}
