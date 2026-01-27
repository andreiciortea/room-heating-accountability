package org.hyperagents.demo;

import cartago.Artifact;
import cartago.OPERATION;
import cartago.OpFeedbackParam;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ASLPatcher extends Artifact {

    private static final String PROMPT_TEMPLATE = """
            You are an expert in Jason/AgentSpeak (ASL) programming for multi-agent systems.

            CURRENT SKILL CODE:
            ```asl
            %s
            ```

            EVALUATION FEEDBACK:
            %s

            AVAILABLE ARTIFACTS (from agent account):
            window
                    - observable properties: window_state(X), where X is "closed" or "tilted" (string literals)
                    - operations: tilt, close
            heater
                - observable properties:
                    - temp(X), where X is the current temperature value
                    - heating(V), where V is either true or false
                    - energyConsumed(E), where E is the amount of consumed energy in kWh
                - operations: startHeating, stopHeating

            TASK:
            Modify the skill code to address the issues identified in the feedback.

            Based on the feedback, you should consider:

            1. ADDING PRECONDITIONS to existing plans:
            - If feedback mentions missing checks, add them to plan contexts
            - Use beliefs that were identified as blindspots
            - Maintain existing context conditions while adding new ones

            2. ADDING CORRECTIVE ACTIONS to existing plans:
            - If feedback mentions environmental conditions that need fixing, add actions to correct them
            - Place corrective actions before the main actions in plan bodies
            - Use operations available in the AVAILABLE ARTIFACTS

            3. ADDING NEW PLANS to handle edge cases:
            - If feedback mentions scenarios not covered by current plans, create new plans
            - **CRITICAL**: Choose the right triggering event:
            * Look at existing plans to see which belief updates trigger them
            * Reuse triggering events from existing plans when adding related corrective plans
            * If multiple plans handle related aspects of the same goal, they should typically share the same trigger
            * Avoid creating plans triggered by beliefs that update infrequently
            * The triggering event should be something that occurs while the problem state exists
            - New plans should have the same triggering event as related existing plans
            - Use context conditions to differentiate when each plan applies
            - Ensure plan contexts are mutually exclusive when appropriate
            - **Order plans from most specific to least specific contexts**

            JASON/AGENTSPEAK SYNTAX REMINDERS:
            - Plan structure: +event : context <- body.
            - Context conditions combined with &
            - Actions in body separated by semicolons (;)
            - String literals use double quotes: "value"
            - Plans are evaluated in order; more specific contexts should come first

            IMPORTANT CONSTRAINTS:
            - Return ONLY the modified ASL code
            - Include ALL plans (modified and unmodified)
            - Do NOT include markdown code fences, explanations, or extra text
            - Do NOT include comments
            - Maintain valid Jason/AgentSpeak syntax
            - Make minimal necessary changes to address the feedback
            - Ensure all string literals use double quotes
            - When adding new plans for edge cases, use triggering events that will actually fire

            Modified skill code:
            """;

    private String provider;
    private String model;
    private String apiKey;
    private HttpClient httpClient;

    protected void init() {
        loadConfiguration();
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
        log("ASLPatcher initialized with provider: " + provider + ", model: " + model);
    }

    @OPERATION
    public void patchAgentSkill(String skillFilePath, String evaluationFeedback, 
        OpFeedbackParam<Boolean> success, OpFeedbackParam<String[]> plans) {
        
        try {
            // 1. Read the current skill file
            String currentCode = readFile(skillFilePath);
            if (currentCode == null) {
                log("Error: Could not read skill file: " + skillFilePath);
                success.set(false);
                return;
            }
            log("Read skill file: " + skillFilePath + " (" + currentCode.length() + " chars)");

            // 2. Create backup
            String backupPath = createBackup(skillFilePath);
            if (backupPath != null) {
                log("Created backup: " + backupPath);
            }

            // 3. Call LLM to get modified code
            String prompt = String.format(PROMPT_TEMPLATE, currentCode, evaluationFeedback);
            String modifiedCode = callLLM(prompt);

            if (modifiedCode == null || modifiedCode.isEmpty()) {
                log("Error: LLM returned empty response");
                success.set(false);
                return;
            }

            // 4. Clean up the response (remove any markdown fences if present)
            modifiedCode = cleanLLMResponse(modifiedCode);

            // 5. Basic validation
            if (!validateASL(modifiedCode)) {
                log("Error: Modified code failed validation");
                success.set(false);
                return;
            }

            // 6. Write the modified code
            if (writeFile(skillFilePath, modifiedCode)) {
                log("Successfully patched skill file: " + skillFilePath);
                signal("skillPatched", skillFilePath);
                success.set(true);

                List<String> skillPlans = parsePlans(modifiedCode);

                for (String sp : skillPlans) {
                    log(sp);
                }

                plans.set(skillPlans.toArray(new String[0]));
            } else {
                log("Error: Could not write modified skill file");
                success.set(false);
            }

        } catch (Exception e) {
            log("Error patching skill: " + e.getMessage());
            e.printStackTrace();
            success.set(false);
        }
    }

    @OPERATION
    public void restoreSkillFromBackup(String skillFilePath, OpFeedbackParam<Boolean> success) {
        try {
            Path original = Path.of(skillFilePath);
            Path backup = Path.of(skillFilePath + ".bak");

            if (!Files.exists(backup)) {
                log("Error: No backup file found for: " + skillFilePath);
                success.set(false);
                return;
            }

            Files.copy(backup, original, StandardCopyOption.REPLACE_EXISTING);
            log("Restored skill from backup: " + skillFilePath);
            success.set(true);

        } catch (IOException e) {
            log("Error restoring from backup: " + e.getMessage());
            success.set(false);
        }
    }

    private String readFile(String filePath) {
        try {
            return Files.readString(Path.of(filePath));
        } catch (IOException e) {
            log("Error reading file: " + e.getMessage());
            return null;
        }
    }

    private boolean writeFile(String filePath, String content) {
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(content);
            return true;
        } catch (IOException e) {
            log("Error writing file: " + e.getMessage());
            return false;
        }
    }

    private String createBackup(String filePath) {
        try {
            Path source = Path.of(filePath);
            Path backup = Path.of(filePath + ".bak");
            Files.copy(source, backup, StandardCopyOption.REPLACE_EXISTING);
            return backup.toString();
        } catch (IOException e) {
            log("Warning: Could not create backup: " + e.getMessage());
            return null;
        }
    }

    private String cleanLLMResponse(String response) {
        String cleaned = response.trim();

        // Remove markdown code fences if present
        if (cleaned.startsWith("```asl")) {
            cleaned = cleaned.substring(6);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }

        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }

        return cleaned.trim();
    }

    private boolean validateASL(String code) {
        // Basic validation checks for ASL syntax

        // Check for balanced parentheses
        int parenCount = 0;
        int bracketCount = 0;
        for (char c : code.toCharArray()) {
            if (c == '(') parenCount++;
            if (c == ')') parenCount--;
            if (c == '[') bracketCount++;
            if (c == ']') bracketCount--;

            if (parenCount < 0 || bracketCount < 0) {
                log("Validation error: Unbalanced brackets");
                return false;
            }
        }

        if (parenCount != 0) {
            log("Validation error: Unbalanced parentheses");
            return false;
        }

        if (bracketCount != 0) {
            log("Validation error: Unbalanced square brackets");
            return false;
        }

        // Check that it contains at least one plan (starts with + or -)
        if (!code.contains("+") && !code.contains("-")) {
            log("Validation error: No plans found in code");
            return false;
        }

        return true;
    }

    private List<String> parsePlans(String code) {
        List<String> plans = new ArrayList<>();
        StringBuilder currentPlan = null;
        boolean inString = false;
        boolean inComment = false;
        char prevChar = 0;

        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);

            // Handle line comments
            if (!inString && c == '/' && i + 1 < code.length() && code.charAt(i + 1) == '/') {
                inComment = true;
            }
            if (inComment && c == '\n') {
                inComment = false;
                prevChar = c;
                continue;
            }
            if (inComment) {
                prevChar = c;
                continue;
            }

            // Handle strings (to ignore dots inside strings)
            if (c == '"' && prevChar != '\\') {
                inString = !inString;
            }

            // Start of a plan
            if (!inString && (c == '+' || c == '-') && currentPlan == null) {
                // Check if it's the start of a plan (not inside an expression)
                // Plans start at beginning of line or after whitespace
                if (i == 0 || Character.isWhitespace(prevChar) || prevChar == '\n') {
                    currentPlan = new StringBuilder();
                    currentPlan.append(c);
                    prevChar = c;
                    continue;
                }
            }

            // End of a plan: '.' NOT followed by an identifier (which would be an internal action like .print)
            if (!inString && c == '.' && currentPlan != null) {
                // Check if next char is an identifier start (letter or underscore) - if so, it's an internal action
                boolean isInternalAction = false;
                if (i + 1 < code.length()) {
                    char nextChar = code.charAt(i + 1);
                    isInternalAction = Character.isLetter(nextChar) || nextChar == '_';
                }

                if (!isInternalAction) {
                    currentPlan.append(c);
                    plans.add(currentPlan.toString().trim());
                    currentPlan = null;
                    prevChar = c;
                    continue;
                }
            }

            // Accumulate plan content
            if (currentPlan != null) {
                currentPlan.append(c);
            }

            prevChar = c;
        }

        return plans;
    }

    private String callLLM(String prompt) throws IOException, InterruptedException {
        return switch (provider.toLowerCase()) {
            case "anthropic" -> callAnthropic(prompt);
            case "openai" -> callOpenAI(prompt);
            case "gemini" -> callGemini(prompt);
            default -> throw new IllegalArgumentException("Unknown provider: " + provider);
        };
    }

    private String callAnthropic(String prompt) throws IOException, InterruptedException {
        String requestBody = """
            {
                "model": "%s",
                "max_tokens": 2048,
                "messages": [
                    {"role": "user", "content": "%s"}
                ]
            }
            """.formatted(model, escapeJson(prompt));

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.anthropic.com/v1/messages"))
            .header("Content-Type", "application/json")
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .timeout(Duration.ofSeconds(120))
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
                "max_completion_tokens": 2048
            }
            """.formatted(model, escapeJson(prompt));

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.openai.com/v1/chat/completions"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .timeout(Duration.ofSeconds(120))
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
                    "maxOutputTokens": 2048
                }
            }
            """.formatted(escapeJson(prompt));

        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .timeout(Duration.ofSeconds(120))
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

    private String extractAnthropicContent(String json) {
        return extractJsonStringValue(json, "text");
    }

    private String extractOpenAIContent(String json) {
        return extractJsonStringValue(json, "content");
    }

    private String extractGeminiContent(String json) {
        return extractJsonStringValue(json, "text");
    }

    private String extractJsonStringValue(String json, String key) {
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
            return json;
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

    private void loadConfiguration() {
        Map<String, String> envFile = loadEnvFile(".env");

        this.provider = getConfig("LLM_PROVIDER", envFile, "anthropic");
        this.model = getConfig("LLM_MODEL", envFile, "claude-sonnet-4-20250514");
        this.apiKey = getConfig("LLM_API_KEY", envFile, null);

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
            return env;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int equalIndex = line.indexOf('=');
                if (equalIndex > 0) {
                    String key = line.substring(0, equalIndex).trim();
                    String value = line.substring(equalIndex + 1).trim();
                    if ((value.startsWith("\"") && value.endsWith("\"")) ||
                        (value.startsWith("'") && value.endsWith("'"))) {
                        value = value.substring(1, value.length() - 1);
                    }
                    env.put(key, value);
                }
            }
        } catch (IOException e) {
            // Ignore, will use system env vars
        }

        return env;
    }

    private String getConfig(String key, Map<String, String> envFile, String defaultValue) {
        String value = System.getenv(key);
        if (value != null && !value.isEmpty()) {
            return value;
        }
        value = envFile.get(key);
        if (value != null && !value.isEmpty()) {
            return value;
        }
        return defaultValue;
    }
}
