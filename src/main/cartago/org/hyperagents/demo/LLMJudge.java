package org.hyperagents.demo;

import cartago.Artifact;
import cartago.OPERATION;
import cartago.OpFeedbackParam;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class LLMJudge extends Artifact {

    private static final String PROMPT_TEMPLATE = """
        You are an expert evaluator analyzing agent performance in a Jason/AgentSpeak multi-agent system.

        AGENT'S ACCOUNT:
        %s

        TASK:
        Analyze the agent's behavior and provide a structured assessment.

        Your analysis should identify:

        1. ROOT CAUSE: What specific aspect of the agent's plans led to the inefficient or suboptimal outcome?
        - Focus on what conditions were missing from plan contexts or what actions were omitted from plan bodies
        - Reference specific plan elements from the SKILLS section of the account

        2. BLINDSPOTS: What information was available in the agent's beliefs but not utilized in decision-making?
        - Identify belief literals that exist but weren't checked in relevant plan contexts
        - Explain why each piece of information matters for the task

        3. CORRECTIVE ACTIONS: What actions should the agent perform to fix problematic environmental states?
        - Identify environmental conditions that block goal achievement
        - Specify what actions from AVAILABLE ARTIFACTS could correct these conditions
        - Explain when and why these corrections should occur

        4. PREVENTIVE ACTIONS: What modifications to plans would prevent this issue in the future?
        - Suggest additional context conditions for existing plans
        - Suggest new plans to handle edge cases or problematic states
        - **IMPORTANT**: When suggesting new plans, choose triggering events that will actually occur
        * Prefer triggering events that fire frequently (e.g., beliefs that update regularly)
        * Avoid triggering events for beliefs that rarely change
        * If a corrective action is needed during an ongoing process, trigger on beliefs that update during that process
        - Describe plan triggers and contexts in natural language

        Provide your analysis in the following JSON format:
        {
        "rootCause": "Detailed explanation of what went wrong",
        "blindspots": [
            "Specific belief or condition that was ignored"
        ],
        "correctiveActions": [
            "Action that should be taken and when"
        ],
        "preventiveActions": [
            "Plan modification or new plan needed"
        ]
        }

        IMPORTANT:
        - Base your analysis ONLY on information present in the agent's account
        - Focus on actionable, specific feedback
        - Consider both the agent's beliefs and the environment state
        - Reference actual plan structures from the SKILLS section
        - When suggesting new plans, explicitly state which belief update should trigger them
        """;


    private static final String PROMPT_TEMPLATE_HUMAN = """
        You are an expert evaluator analyzing agent performance in a Jason/AgentSpeak multi-agent system.

        HUMAN's ACCOUNT:
        %s

        AGENT'S ACCOUNT:
        %s

        TASK:
        Analyze the agent's behavior and provide a structured assessment.

        Your analysis should identify:

        1. ROOT CAUSE: What specific aspect of the agent's plans led to the inefficient or suboptimal outcome?
        - Focus on what conditions were missing from plan contexts or what actions were omitted from plan bodies
        - Reference specific plan elements from the SKILLS section of the account

        2. BLINDSPOTS: What information was available in the agent's beliefs but not utilized in decision-making?
        - Identify belief literals that exist but weren't checked in relevant plan contexts
        - Explain why each piece of information matters for the task

        3. CORRECTIVE ACTIONS: What actions should the agent perform to fix problematic environmental states?
        - Identify environmental conditions that block goal achievement
        - Specify what actions from AVAILABLE ARTIFACTS could correct these conditions
        - Explain when and why these corrections should occur

        4. PREVENTIVE ACTIONS: What modifications to plans would prevent this issue in the future?
        - Suggest additional context conditions for existing plans
        - Suggest new plans to handle edge cases or problematic states
        - **IMPORTANT**: When suggesting new plans, choose triggering events that will actually occur
        * Prefer triggering events that fire frequently (e.g., beliefs that update regularly)
        * Avoid triggering events for beliefs that rarely change
        * If a corrective action is needed during an ongoing process, trigger on beliefs that update during that process
        - Describe plan triggers and contexts in natural language

        Provide your analysis in the following JSON format:
        {
        "rootCause": "Detailed explanation of what went wrong",
        "blindspots": [
            "Specific belief or condition that was ignored"
        ],
        "correctiveActions": [
            "Action that should be taken and when"
        ],
        "preventiveActions": [
            "Plan modification or new plan needed"
        ]
        }

        IMPORTANT:
        - Base your analysis ONLY on information present in the agent's account
        - Focus on actionable, specific feedback
        - Consider both the agent's beliefs and the environment state
        - Reference actual plan structures from the SKILLS section
        - When suggesting new plans, explicitly state which belief update should trigger them
        """;

    private String provider;
    private String model;
    private String apiKey;
    private double temperature;
    private HttpClient httpClient;

    protected void init() {
        loadConfiguration();
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
        log("LLMJudge initialized with provider: " + provider + ", model: " + model + ", temperature: " + temperature);
    }

    private void loadConfiguration() {
        // Load .env file first
        Map<String, String> envFile = loadEnvFile(".env");

        // Priority: System env vars > .env file > defaults
        this.provider = getConfig("LLM_PROVIDER", envFile, "anthropic");
        this.model = getConfig("LLM_MODEL", envFile, "claude-sonnet-4-20250514");
        this.apiKey = getConfig("LLM_API_KEY", envFile, null);
        this.temperature = Double.parseDouble(getConfig("JUDGE_TEMPERATURE", envFile, "0"));

        // Provider-specific API key as fallback
        if (apiKey == null) {
            String providerKeyName = switch (provider.toLowerCase()) {
                case "anthropic" -> "ANTHROPIC_API_KEY";
                case "openai" -> "OPENAI_API_KEY";
                case "gemini" -> "GOOGLE_API_KEY";
                default -> null;
            };
            if (providerKeyName != null) {
                apiKey = getConfig(providerKeyName, envFile, null);
            }
        }
    }

    private Map<String, String> loadEnvFile(String filename) {
        Map<String, String> env = new HashMap<>();
        Path path = Path.of(filename);

        if (!Files.exists(path)) {
            log("Warning: .env file not found, using system environment variables");
            return env;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // Skip empty lines and comments
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int equalIndex = line.indexOf('=');
                if (equalIndex > 0) {
                    String key = line.substring(0, equalIndex).trim();
                    String value = line.substring(equalIndex + 1).trim();
                    // Remove surrounding quotes if present
                    if ((value.startsWith("\"") && value.endsWith("\"")) ||
                        (value.startsWith("'") && value.endsWith("'"))) {
                        value = value.substring(1, value.length() - 1);
                    }
                    env.put(key, value);
                }
            }
        } catch (IOException e) {
            log("Warning: Could not read .env file: " + e.getMessage());
        }

        return env;
    }

    private String getConfig(String key, Map<String, String> envFile, String defaultValue) {
        // System env vars take priority
        String value = System.getenv(key);
        if (value != null && !value.isEmpty()) {
            return value;
        }
        // Then .env file
        value = envFile.get(key);
        if (value != null && !value.isEmpty()) {
            return value;
        }
        // Finally default
        return defaultValue;
    }

    @OPERATION
    public void judgeAccount(String account1, String account2, OpFeedbackParam<String> evaluation) {
        String prompt = String.format(PROMPT_TEMPLATE_HUMAN, account1, account2);

        try {
            String response = switch (provider.toLowerCase()) {
                case "anthropic" -> callAnthropic(prompt);
                case "openai" -> callOpenAI(prompt);
                case "gemini" -> callGemini(prompt);
                default -> throw new IllegalArgumentException("Unknown provider: " + provider);
            };

            // Extract just the JSON evaluation object from the response
            String jsonEvaluation = extractJsonObject(response);
            evaluation.set(jsonEvaluation);
            log("LLM evaluation completed, extracted JSON evaluation");
        } catch (Exception e) {
            String error = "Error calling LLM: " + e.getMessage();
            log(error);
            evaluation.set(error);
        }
    }

    @OPERATION
    public void judgeAccount(String account, OpFeedbackParam<String> evaluation) {
        String prompt = String.format(PROMPT_TEMPLATE, account);

        try {
            String response = switch (provider.toLowerCase()) {
                case "anthropic" -> callAnthropic(prompt);
                case "openai" -> callOpenAI(prompt);
                case "gemini" -> callGemini(prompt);
                default -> throw new IllegalArgumentException("Unknown provider: " + provider);
            };

            // Extract just the JSON evaluation object from the response
            String jsonEvaluation = extractJsonObject(response);
            evaluation.set(jsonEvaluation);
            log("LLM evaluation completed, extracted JSON evaluation");
        } catch (Exception e) {
            String error = "Error calling LLM: " + e.getMessage();
            log(error);
            evaluation.set(error);
        }
    }

    private String extractJsonObject(String llmResponse) {
        // Find the first opening brace (start of JSON object)
        int braceStart = llmResponse.indexOf('{');

        if (braceStart == -1) {
            log("Warning: Could not find JSON object in LLM response, returning full response");
            return llmResponse;
        }

        // Find the matching closing brace
        int braceEnd = findMatchingBrace(llmResponse, braceStart);
        if (braceEnd == -1) {
            log("Warning: Could not find closing brace for JSON object, returning full response");
            return llmResponse;
        }

        return llmResponse.substring(braceStart, braceEnd + 1);
    }

    private int findMatchingBrace(String text, int openBracePos) {
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;

        for (int i = openBracePos; i < text.length(); i++) {
            char c = text.charAt(i);

            if (escaped) {
                escaped = false;
                continue;
            }

            if (c == '\\' && inString) {
                escaped = true;
                continue;
            }

            if (c == '"') {
                inString = !inString;
                continue;
            }

            if (!inString) {
                if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        return i;
                    }
                }
            }
        }

        return -1;
    }

    private String callAnthropic(String prompt) throws IOException, InterruptedException {
        String requestBody = """
            {
                "model": "%s",
                "max_tokens": 1024,
                "temperature": %s,
                "messages": [
                    {"role": "user", "content": "%s"}
                ]
            }
            """.formatted(model, temperature, escapeJson(prompt));

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.anthropic.com/v1/messages"))
            .header("Content-Type", "application/json")
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .timeout(Duration.ofSeconds(60))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Anthropic API error: " + response.statusCode() + " - " + response.body());
        }

        return extractAnthropicContent(response.body());
    }

    private String callOpenAI(String prompt) throws IOException, InterruptedException {
        String requestBody = """
            {
                "model": "%s",
                "messages": [
                    {"role": "user", "content": "%s"}
                ],
                "max_completion_tokens": 1024,
                "temperature": %s
            }
            """.formatted(model, escapeJson(prompt), temperature);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.openai.com/v1/chat/completions"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .timeout(Duration.ofSeconds(60))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("OpenAI API error: " + response.statusCode() + " - " + response.body());
        }

        return extractOpenAIContent(response.body());
    }

    private String callGemini(String prompt) throws IOException, InterruptedException {
        String requestBody = """
            {
                "contents": [
                    {"parts": [{"text": "%s"}]}
                ],
                "generationConfig": {
                    "maxOutputTokens": 1024,
                    "temperature": %s
                }
            }
            """.formatted(escapeJson(prompt), temperature);

        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .timeout(Duration.ofSeconds(60))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Gemini API error: " + response.statusCode() + " - " + response.body());
        }

        return extractGeminiContent(response.body());
    }

    private String escapeJson(String text) {
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

    // Simple JSON parsing without external dependencies
    private String extractAnthropicContent(String json) {
        // Extract content from: {"content":[{"type":"text","text":"..."}],...}
        return extractJsonStringValue(json, "text");
    }

    private String extractOpenAIContent(String json) {
        // Extract content from: {"choices":[{"message":{"content":"..."}}],...}
        return extractJsonStringValue(json, "content");
    }

    private String extractGeminiContent(String json) {
        // Extract content from: {"candidates":[{"content":{"parts":[{"text":"..."}]}}],...}
        return extractJsonStringValue(json, "text");
    }

    private String extractJsonStringValue(String json, String key) {
        // Look for "key": " or "key":" patterns
        String pattern1 = "\"" + key + "\":\"";
        String pattern2 = "\"" + key + "\": \"";

        int start = json.indexOf(pattern1);
        int keyLen = pattern1.length();

        if (start == -1) {
            start = json.indexOf(pattern2);
            keyLen = pattern2.length();
        }

        if (start == -1) {
            log("Warning: Could not find key '" + key + "' in JSON response");
            return json; // Return full response if parsing fails
        }

        start += keyLen;
        int end = findClosingQuote(json, start);

        if (end <= start) {
            log("Warning: Could not find closing quote for key '" + key + "'");
            return json;
        }

        return unescapeJson(json.substring(start, end));
    }

    private int findClosingQuote(String json, int start) {
        boolean escaped = false;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\') {
                escaped = true;
                continue;
            }
            if (c == '"') {
                return i;
            }
        }
        return json.length();
    }

    private String unescapeJson(String text) {
        StringBuilder result = new StringBuilder();
        boolean escaped = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (escaped) {
                switch (c) {
                    case 'n' -> result.append('\n');
                    case 'r' -> result.append('\r');
                    case 't' -> result.append('\t');
                    case '"' -> result.append('"');
                    case '\\' -> result.append('\\');
                    default -> result.append(c);
                }
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }
}
