package com.ai.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class ModelService {

    private static final String API_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation ";
    private static final String API_KEY = "your API Key";
    private static final String OLLAMA_URL = "http://localhost:11434/api/generate"; // Ollama服务地址
    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        // 避免空类或未知字段导致 Jackson 抛异常
        objectMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    // ================== 请求 DTO ==================
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private static class QwenInput {
        private String prompt;

        public QwenInput(String prompt) {
            this.prompt = prompt;
        }

        public String getPrompt() {
            return prompt;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private static class QwenParameters {
        private Double temperature;
        private Double top_p;
        private Integer max_tokens;
        private Boolean stream;

        public QwenParameters(Double temperature, Double top_p, Integer max_tokens, Boolean stream) {
            this.temperature = temperature;
            this.top_p = top_p;
            this.max_tokens = max_tokens;
            this.stream = stream;
        }

        public Double getTemperature() {
            return temperature;
        }

        public Double getTop_p() {
            return top_p;
        }

        public Integer getMax_tokens() {
            return max_tokens;
        }

        public Boolean getStream() {
            return stream;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private static class QwenRequest {
        private String model;
        private QwenInput input;
        private QwenParameters parameters;

        public QwenRequest(String model, String prompt, QwenParameters parameters) {
            this.model = model;
            this.input = new QwenInput(prompt);
            this.parameters = parameters;
        }

        public String getModel() {
            return model;
        }

        public QwenInput getInput() {
            return input;
        }

        public QwenParameters getParameters() {
            return parameters;
        }
    }

    // ================== 响应 DTO ==================
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private static class QwenMessage {
        private String content;
        private String role;

        public String getContent() {
            return content;
        }

        public String getRole() {
            return role;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private static class QwenChoice {
        private QwenMessage message;

        public QwenMessage getMessage() {
            return message;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private static class QwenOutput {
        private List<QwenChoice> choices;

        public List<QwenChoice> getChoices() {
            return choices;
        }

        public String getText() {
            if (choices != null && !choices.isEmpty()) {
                return choices.get(0).getMessage().getContent();
            }
            return "";
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private static class QwenUsage {
        private Integer total_tokens;
        private Integer input_tokens;
        private Integer output_tokens;

        public Integer getTotalTokens() {
            return total_tokens;
        }
    }

    @Data
    private static class QwenResponse {
        private QwenOutput output;
        private QwenUsage usage;
        private String request_id;

        public String getOutputText() {
            String text = output != null ? output.getText() : "";
            return removeStartAndEndMarkers(text);
        }

        private String removeStartAndEndMarkers(String text) {
            if (text == null || text.isEmpty()) {
                return text;
            }
            return text
                    .replace("[START]", "")
                    .replace("[END]", "")
                    .trim();
        }
    }
    // ================== 同步调用 ==================
    public String callModel(String prompt) {
        try {
            QwenRequest request = new QwenRequest(
                    "qwen3-32b",
                    prompt,
                    new QwenParameters(0.7, 0.8, 1024, false)
            );

            String inputJson = objectMapper.writeValueAsString(request);

            URL url = new URL(API_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + API_KEY);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] inputBytes = inputJson.getBytes(StandardCharsets.UTF_8);
                os.write(inputBytes, 0, inputBytes.length);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }

                    QwenResponse qwenResponse = objectMapper.readValue(response.toString(), QwenResponse.class);
                    return qwenResponse.getOutputText();
                }
            } else {
                try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
                    StringBuilder errorResponse = new StringBuilder();
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    throw new RuntimeException("HTTP request failed with code " + responseCode +
                            ": " + errorResponse.toString());
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Error calling the model API", e);
        }
    }

    // ================== 流式调用（SSE）==================
    public void streamCallModel(String prompt, Consumer<String> onMessage) {
        try {
            QwenRequest request = new QwenRequest(
                    "qwen3-32b",
                    prompt,
                    new QwenParameters(0.7, 0.8, 1024, true)
            );

            String inputJson = objectMapper.writeValueAsString(request);

            URL url = new URL(API_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + API_KEY);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "text/event-stream");
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] inputBytes = inputJson.getBytes(StandardCharsets.UTF_8);
                os.write(inputBytes, 0, inputBytes.length);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String line;
                    while ((line = in.readLine()) != null) {
                        if (line.startsWith("data:")) {
                            String data = line.substring(5).trim();
                            if (!data.equals("[DONE]") && !data.contains("field not ignorable")) {
                                try {
                                    QwenResponse qwenResponse = objectMapper.readValue(data, QwenResponse.class);
                                    String rawText = qwenResponse.getOutputText();

                                    if (rawText != null && !rawText.isEmpty()) {
                                        String cleanedText = rawText
                                                .replace("[START]", "")
                                                .replace("[END]", "")
                                                .replace("[ERROR]", "")
                                                .trim();

                                        if (!cleanedText.isEmpty()) {
                                            onMessage.accept(cleanedText);
                                        }
                                    }
                                } catch (JsonProcessingException e) {
                                    onMessage.accept("[ERROR] " + e.getMessage());
                                }
                            }
                        }
                    }
                }
            } else {
                try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
                    StringBuilder errorResponse = new StringBuilder();
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    throw new RuntimeException("HTTP request failed with code " + responseCode +
                            ": " + errorResponse.toString());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error in streaming call to the model API", e);
        }
    }

    // 调用Ollama模型
    public String callOllamaModel(String prompt) {
        try {
            URL url = new URL(OLLAMA_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            // 构建请求体
            String requestBody = "{\"model\": \"deepseek-r1:14b\",\"prompt\": \"" + prompt + "\"}";
            try (OutputStream os = connection.getOutputStream()) {
                byte[] inputBytes = requestBody.getBytes(StandardCharsets.UTF_8);
                os.write(inputBytes, 0, inputBytes.length);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    // 这里简单假设响应中包含文本字段，实际需要根据Ollama的响应结构调整
                    // 示例：假设响应是JSON，文本字段名为 "text"
                    // 这里简单返回整个响应，需要根据实际情况解析
                    return response.toString();
                }
            } else {
                try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
                    StringBuilder errorResponse = new StringBuilder();
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    throw new RuntimeException("HTTP request to Ollama failed with code " + responseCode +
                            ": " + errorResponse.toString());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error calling Ollama model API", e);
        }
    }
    // 流式调用Ollama模型
    public void streamCallOllamaModel(String prompt, Consumer<String> onMessage) {
        try {
            // 构建Ollama请求体
            OllamaRequest request = new OllamaRequest();
            request.setModel("deepseek-r1:14b"); // 假设使用DeepSeek模型
            request.setPrompt(prompt);
            request.setStream(true);

            String inputJson = objectMapper.writeValueAsString(request);

            URL url = new URL(OLLAMA_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] inputBytes = inputJson.getBytes(StandardCharsets.UTF_8);
                os.write(inputBytes, 0, inputBytes.length);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String line;
                    while ((line = in.readLine()) != null) {
                        if (!line.isEmpty()) {
                            try {
                                OllamaResponse ollamaResponse = objectMapper.readValue(line, OllamaResponse.class);
                                String text = ollamaResponse.getResponse();
                                if (text != null && !text.isEmpty()) {
                                    onMessage.accept(text);
                                }
                            } catch (JsonProcessingException e) {
                                onMessage.accept("[ERROR] " + e.getMessage());
                            }
                        }
                    }
                }
            } else {
                try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
                    StringBuilder errorResponse = new StringBuilder();
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    onMessage.accept("[ERROR] HTTP request failed with code " + responseCode + ": " + errorResponse.toString());
                }
            }
        } catch (Exception e) {
            onMessage.accept("[ERROR] " + e.getMessage());
        }
    }

    // Ollama请求DTO
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Data
    private static class OllamaRequest {
        private String model;
        private String prompt;
        private boolean stream;
    }

    // Ollama响应DTO
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Data
    private static class OllamaResponse {
        private String response;
    }
}