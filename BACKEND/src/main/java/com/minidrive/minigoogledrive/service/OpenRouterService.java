package com.minidrive.minigoogledrive.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OpenRouterService {

        private final RestTemplate restTemplate = new RestTemplate();

        private static final String OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions";

        // =========================================================
        // MODELS
        // =========================================================

        /*
         * Normal AI / File AI / Rename
         */
        private static final String TEXT_MODEL = "openrouter/free";

        /*
         * IMAGE / VISION AI
         */
        private static final String VISION_MODEL = "nvidia/nemotron-3-nano-omni-30b-a3b-reasoning:free";

        @Value("${openrouter.api.key}")
        private String apiKey;

        // =========================================================
        // MINI GOOGLE DRIVE AI - CORE INSTRUCTIONS
        // =========================================================

        private static final String SYSTEM_PROMPT = """

                        You are Mini Google Drive AI, an intelligent AI assistant
                        built into a personal cloud-drive application.

                        Your job is to help the user understand, analyze, solve,
                        summarize and work with their files.

                        =========================================================
                        CORE BEHAVIOR
                        =========================================================

                        1. Be accurate before being fast.

                        2. Understand the user's actual question before answering.

                        3. Never invent information.

                        4. Never invent numbers, equations, values, units,
                           file contents or facts that are not available.

                        5. If information is missing, clearly state what is missing.

                        6. If information in an image is unclear or unreadable,
                           do NOT guess it.

                        7. Use the uploaded file as the primary source when the
                           user asks about a file.

                        8. Answer naturally and clearly.

                        9. Do not unnecessarily repeat the user's question.

                        10. Give the final answer clearly.

                        =========================================================
                        MATHEMATICS
                        =========================================================

                        When solving mathematics problems:

                        - First identify the given information.
                        - Identify what must be found.
                        - Write the relevant formula or equation.
                        - Substitute the known values.
                        - Show important intermediate calculations.
                        - Simplify carefully.
                        - Check the result when practical.
                        - Give the final answer clearly.
                        - Include units when applicable.

                        Never skip directly from the question to the final answer
                        when the user asks for a solution or steps.

                        For numerical calculations, calculate carefully and do not
                        casually approximate intermediate values.

                        If there are multiple possible answers, give all relevant
                        answers and explain which ones are valid.

                        For equations, distinguish between exact and approximate
                        answers.

                        IMPORTANT FORMATTING RULE:

                        Do NOT use LaTeX delimiters such as:

                        $x=5$

                        \\(x=5\\)

                        \\[x=5\\]

                        Do NOT put equations inside curly braces.

                        Write simple mathematical expressions normally.

                        Example:

                        Given:
                        2x + 5 = 15

                        Step 1:
                        2x + 5 - 5 = 15 - 5

                        Step 2:
                        2x = 10

                        Step 3:
                        x = 5

                        Therefore:
                        x = 5

                        =========================================================
                        ENGINEERING
                        =========================================================

                        For engineering problems:

                        1. Clearly identify the given data.
                        2. Identify the required quantity.
                        3. State the correct engineering formula.
                        4. Check units.
                        5. Substitute values carefully.
                        6. Show calculations.
                        7. State assumptions if necessary.
                        8. Give the final answer with units.

                        If a diagram is provided, carefully interpret the diagram
                        before calculating.

                        Do not assume dimensions or loads that are not visible
                        unless the user explicitly asks you to assume values.

                        If assumptions are necessary, clearly label them as:

                        ASSUMPTION:

                        =========================================================
                        PHYSICS
                        =========================================================

                        For physics problems:

                        - Identify known quantities.
                        - Identify the unknown.
                        - Select the appropriate physical law or formula.
                        - Check units and dimensions.
                        - Substitute values.
                        - Calculate step-by-step.
                        - Give the final answer with units.

                        =========================================================
                        PROGRAMMING
                        =========================================================

                        For programming questions:

                        - Explain the approach first when useful.
                        - Give correct code.
                        - Explain important parts of the code.
                        - Identify bugs clearly.
                        - Do not invent APIs or library behavior.
                        - If the user provides code, analyze that code directly.

                        =========================================================
                        FILE ANALYSIS
                        =========================================================

                        When analyzing a PDF, DOC, DOCX or TXT file:

                        - Use the extracted file content.
                        - Answer questions using that content.
                        - Preserve important details.
                        - If the file contains several questions, separate them.
                        - Do not fabricate missing portions of the document.

                        If the user asks for a summary:
                        provide a structured summary.

                        If the user asks for questions and answers:
                        clearly separate each question and answer.

                        If the user asks to solve problems:
                        solve them step-by-step.

                        =========================================================
                        IMAGE ANALYSIS
                        =========================================================

                        When analyzing an uploaded image:

                        IMPORTANT:

                        The image itself is the primary source.

                        Do NOT assume that every image is a document.

                        The image may contain:

                        - People
                        - Clothes
                        - Objects
                        - Animals
                        - Buildings
                        - Vehicles
                        - Food
                        - Landscapes
                        - Screenshots
                        - Documents
                        - Handwritten text
                        - Printed text
                        - Mathematical problems
                        - Engineering diagrams
                        - Tables

                        Carefully inspect the actual visual content.

                        If the user asks about a person's clothing,
                        directly inspect the clothing and answer.

                        Do NOT respond that OCR failed.

                        Do NOT treat a normal photograph as an OCR-only task.

                        If the user asks about colors, identify the visible color
                        as accurately as possible.

                        If lighting affects the color, mention that briefly.

                        If the image contains text, read the visible text when
                        possible.

                        If the image contains a mathematical problem,
                        identify the visible equation and solve it step-by-step.

                        If the image contains an engineering diagram,
                        identify visible dimensions, loads, supports and other
                        relevant information.

                        Do not invent missing information.

                        If something is genuinely unclear, clearly state what
                        part is unclear.

                        =========================================================
                        IMAGE QUESTION PRIORITY
                        =========================================================

                        Always answer the user's actual image question first.

                        For example:

                        User:
                        What color shirt is he wearing?

                        Answer:
                        He is wearing a blue shirt.

                        Do NOT unnecessarily describe OCR,
                        extracted text, document quality or image processing.

                        If the user asks about an object, inspect the object.

                        If the user asks about a person, inspect the person.

                        If the user asks about colors, inspect colors.

                        If the user asks about text, inspect text.

                        If the user asks about a diagram, inspect the diagram.

                        =========================================================
                        ANSWER QUALITY
                        =========================================================

                        Prefer:

                        Clear headings
                        Short paragraphs
                        Numbered steps
                        Normal mathematical notation
                        Tables when useful
                        Clearly marked final answers

                        Avoid:

                        Unnecessary filler
                        Repeating the same explanation
                        Fake certainty
                        Invented information
                        Unsupported assumptions
                        Extremely complicated wording

                        =========================================================
                        ACCURACY CHECK
                        =========================================================

                        Before giving the final answer:

                        - Check whether the answer actually answers the question.
                        - Check important arithmetic.
                        - Check signs (+/-).
                        - Check units.
                        - Check equations.
                        - Check whether assumptions were clearly identified.
                        - Check whether the conclusion follows from the given data.

                        For image questions:

                        - Look at the actual image.
                        - Answer from visible information.
                        - Do not replace visual analysis with OCR.
                        - Do not claim the image is unreadable unless it genuinely is.

                        =========================================================
                        RESPONSE STYLE
                        =========================================================

                        Be like a very good personal tutor.

                        Explain things so that a college student can understand them.

                        Be detailed enough to learn from, but do not add unnecessary
                        information.

                        If the user asks for "just the answer", give the concise answer.

                        If the user asks for "step-by-step", provide a complete
                        step-by-step solution.

                        If the user asks "explain", explain the reasoning clearly.

                        Always prioritize correctness and clarity.
                        """;

        // =========================================================
        // NORMAL AI
        // =========================================================

        public String askAI(String question) {

                if (question == null ||
                                question.trim().isEmpty()) {

                        throw new RuntimeException(
                                        "Question cannot be empty");
                }

                String prompt = SYSTEM_PROMPT
                                + "\n\n"
                                + "=========================================================\n"
                                + "USER QUESTION\n"
                                + "=========================================================\n\n"
                                + question;

                return sendTextRequest(
                                prompt,
                                TEXT_MODEL);
        }

        // =========================================================
        // CHAT WITH TEXT / PDF / DOC / DOCX
        // =========================================================

        public String askAboutFile(
                        String question,
                        String fileName,
                        String content) {

                if (question == null ||
                                question.trim().isEmpty()) {

                        throw new RuntimeException(
                                        "Question cannot be empty");
                }

                if (content == null) {
                        content = "";
                }

                /*
                 * IMPORTANT:
                 *
                 * This prompt makes it very clear that the retrieved RAG
                 * content is the source of truth.
                 *
                 * It also prevents the model from returning things like:
                 *
                 * "User Safety: safe"
                 *
                 * when answering normal file questions.
                 */

                String prompt = SYSTEM_PROMPT

                                + "\n\n"
                                + "=========================================================\n"
                                + "FILE QUESTION ANSWERING MODE\n"
                                + "=========================================================\n\n"

                                + "The user is asking a question about an uploaded file.\n\n"

                                + "You MUST answer the user's question using the "
                                + "RETRIEVED FILE CONTENT below.\n\n"

                                + "The retrieved file content is evidence extracted "
                                + "from the user's file.\n\n"

                                + "Do NOT replace the file content with general knowledge "
                                + "when the answer should come from the file.\n\n"

                                + "Do NOT classify the user's question as safe, unsafe, "
                                + "harmful, or anything similar.\n\n"

                                + "NEVER output:\n"
                                + "\"User Safety: safe\"\n"
                                + "\"User Safety: unsafe\"\n"
                                + "or similar safety classifications.\n\n"

                                + "If the user asks for a person's name, search the "
                                + "retrieved content carefully for names.\n\n"

                                + "If the user asks for a date of birth, search the "
                                + "retrieved content for DOB, Date of Birth, birth date, "
                                + "or equivalent information.\n\n"

                                + "If the user asks what a profession, trade, object, "
                                + "term or topic is, use the relevant section of the file.\n\n"

                                + "If the requested information genuinely does not appear "
                                + "in the retrieved content, say:\n\n"

                                + "I could not find that information in the file.\n\n"

                                + "Do NOT invent information.\n\n"

                                + "=========================================================\n"
                                + "FILE NAME\n"
                                + "=========================================================\n\n"

                                + fileName

                                + "\n\n"

                                + "=========================================================\n"
                                + "RETRIEVED FILE CONTENT\n"
                                + "=========================================================\n\n"

                                + content

                                + "\n\n"

                                + "=========================================================\n"
                                + "USER QUESTION\n"
                                + "=========================================================\n\n"

                                + question

                                + "\n\n"

                                + "=========================================================\n"
                                + "FINAL ANSWER RULES\n"
                                + "=========================================================\n\n"

                                + "1. Answer the user's exact question.\n"
                                + "2. Use the retrieved file content.\n"
                                + "3. Search the entire provided context before deciding "
                                + "the answer is missing.\n"
                                + "4. Do not mention RAG.\n"
                                + "5. Do not mention embeddings.\n"
                                + "6. Do not mention chunks.\n"
                                + "7. Do not mention OpenRouter.\n"
                                + "8. Do not mention internal processing.\n"
                                + "9. Do not output safety classifications.\n"
                                + "10. Do not invent information.\n"
                                + "11. Keep simple questions concise.\n"
                                + "12. If the user asks for steps, provide steps.\n"
                                + "13. If the answer is a person's name, give the name "
                                + "directly and clearly.\n"
                                + "14. If the answer is not present, clearly say that "
                                + "it could not be found in the file.";

                return sendTextRequest(
                                prompt,
                                TEXT_MODEL);
        }

        // =========================================================
        // IMAGE AI
        // =========================================================

        public String askAboutImage(
                        String question,
                        byte[] imageBytes,
                        String mimeType) {

                if (question == null ||
                                question.trim().isEmpty()) {

                        throw new RuntimeException(
                                        "Question cannot be empty");
                }

                if (imageBytes == null ||
                                imageBytes.length == 0) {

                        throw new RuntimeException(
                                        "Image data is empty");
                }

                if (mimeType == null ||
                                !mimeType.startsWith("image/")) {

                        throw new RuntimeException(
                                        "Invalid image MIME type");
                }

                // -----------------------------------------------------
                // BASE64 IMAGE
                // -----------------------------------------------------

                String base64Image = Base64
                                .getEncoder()
                                .encodeToString(imageBytes);

                // -----------------------------------------------------
                // IMAGE PART
                // -----------------------------------------------------

                Map<String, Object> imagePart = new HashMap<>();

                imagePart.put(
                                "type",
                                "image_url");

                Map<String, String> imageUrl = new HashMap<>();

                imageUrl.put(
                                "url",
                                "data:"
                                                + mimeType
                                                + ";base64,"
                                                + base64Image);

                imagePart.put(
                                "image_url",
                                imageUrl);

                // -----------------------------------------------------
                // TEXT PART
                // -----------------------------------------------------

                Map<String, Object> textPart = new HashMap<>();

                textPart.put(
                                "type",
                                "text");

                String imagePrompt = SYSTEM_PROMPT

                                + "\n\n"
                                + "=========================================================\n"
                                + "IMAGE ANALYSIS MODE\n"
                                + "=========================================================\n\n"

                                + "The user has uploaded an image.\n\n"

                                + "Analyze the actual visual content of the image.\n\n"

                                + "The user's question is the most important instruction.\n\n"

                                + "Do NOT perform OCR-only analysis.\n\n"

                                + "Do NOT assume this is a document.\n\n"

                                + "Do NOT say that the image is garbled merely because "
                                + "there is little or no readable text.\n\n"

                                + "If the user asks about a person's appearance, "
                                + "clothing, colors, objects, background or scene, "
                                + "answer from the actual visual information.\n\n"

                                + "If the user asks about text, read the visible text.\n\n"

                                + "If the user asks about mathematics or engineering, "
                                + "inspect the relevant equation or diagram and solve it.\n\n"

                                + "Never invent information that cannot be determined "
                                + "from the image.\n\n"

                                + "If a visual detail is genuinely unclear, "
                                + "say exactly what is unclear.\n\n"

                                + "USER QUESTION:\n"
                                + question;

                textPart.put(
                                "text",
                                imagePrompt);

                // -----------------------------------------------------
                // MESSAGE CONTENT
                // -----------------------------------------------------

                List<Object> content = new ArrayList<>();

                content.add(textPart);
                content.add(imagePart);

                // -----------------------------------------------------
                // MESSAGE
                // -----------------------------------------------------

                Map<String, Object> message = new HashMap<>();

                message.put(
                                "role",
                                "user");

                message.put(
                                "content",
                                content);

                // -----------------------------------------------------
                // SEND USING VISION MODEL
                // -----------------------------------------------------

                return sendRequest(
                                List.of(message),
                                VISION_MODEL);
        }

        // =========================================================
        // SEND TEXT REQUEST
        // =========================================================

        private String sendTextRequest(
                        String prompt,
                        String model) {

                Map<String, Object> message = new HashMap<>();

                message.put(
                                "role",
                                "user");

                message.put(
                                "content",
                                prompt);

                return sendRequest(
                                List.of(message),
                                model);
        }

        // =========================================================
        // SEND REQUEST TO OPENROUTER
        // =========================================================

        private String sendRequest(
                        List<Map<String, Object>> messages,
                        String model) {

                if (apiKey == null ||
                                apiKey.trim().isEmpty()) {

                        throw new RuntimeException(
                                        "OpenRouter API key is missing");
                }

                Map<String, Object> request = new HashMap<>();

                request.put(
                                "model",
                                model);

                request.put(
                                "messages",
                                messages);

                /*
                 * Keep temperature low for accurate file answers.
                 */
                request.put(
                                "temperature",
                                0.1);

                request.put(
                                "stream",
                                false);

                // -----------------------------------------------------
                // REASONING
                // -----------------------------------------------------

                if (model.equals(VISION_MODEL)) {

                        Map<String, Object> reasoning = new HashMap<>();

                        reasoning.put(
                                        "enabled",
                                        false);

                        request.put(
                                        "reasoning",
                                        reasoning);
                }

                // -----------------------------------------------------
                // HEADERS
                // -----------------------------------------------------

                HttpHeaders headers = new HttpHeaders();

                headers.setContentType(
                                MediaType.APPLICATION_JSON);

                headers.setBearerAuth(
                                apiKey);

                headers.set(
                                "HTTP-Referer",
                                "https://minigoogledrive-r6yw.vercel.app");

                headers.set(
                                "X-Title",
                                "Mini Google Drive");

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(
                                request,
                                headers);

                // -----------------------------------------------------
                // REQUEST
                // -----------------------------------------------------

                try {

                        ResponseEntity<Map> response = restTemplate.postForEntity(
                                        OPENROUTER_URL,
                                        entity,
                                        Map.class);

                        if (response.getBody() == null) {

                                throw new RuntimeException(
                                                "OpenRouter returned an empty response");
                        }

                        // -------------------------------------------------
                        // ERROR
                        // -------------------------------------------------

                        Object errorObject = response.getBody().get("error");

                        if (errorObject != null) {

                                throw new RuntimeException(
                                                "OpenRouter error: "
                                                                + errorObject);
                        }

                        // -------------------------------------------------
                        // CHOICES
                        // -------------------------------------------------

                        Object choicesObject = response.getBody().get("choices");

                        if (!(choicesObject instanceof List)) {

                                throw new RuntimeException(
                                                "OpenRouter returned no choices: "
                                                                + response.getBody());
                        }

                        List<?> choices = (List<?>) choicesObject;

                        if (choices.isEmpty()) {

                                throw new RuntimeException(
                                                "OpenRouter returned no AI answer");
                        }

                        // -------------------------------------------------
                        // FIRST CHOICE
                        // -------------------------------------------------

                        Object firstChoice = choices.get(0);

                        if (!(firstChoice instanceof Map)) {

                                throw new RuntimeException(
                                                "Invalid OpenRouter response");
                        }

                        Map<?, ?> choice = (Map<?, ?>) firstChoice;

                        // -------------------------------------------------
                        // MESSAGE
                        // -------------------------------------------------

                        Object messageObject = choice.get("message");

                        if (!(messageObject instanceof Map)) {

                                throw new RuntimeException(
                                                "OpenRouter returned invalid message");
                        }

                        Map<?, ?> message = (Map<?, ?>) messageObject;

                        // -------------------------------------------------
                        // CONTENT
                        // -------------------------------------------------

                        Object content = message.get("content");

                        if (content == null) {

                                throw new RuntimeException(
                                                "OpenRouter returned empty content");
                        }

                        String answer = content.toString().trim();

                        if (answer.isEmpty()) {

                                throw new RuntimeException(
                                                "OpenRouter returned a blank answer");
                        }

                        return answer;

                } catch (Exception e) {

                        e.printStackTrace();

                        throw new RuntimeException(
                                        "OpenRouter AI request failed: "
                                                        + e.getMessage(),
                                        e);
                }
        }

        // =========================================================
        // AI RENAME
        // =========================================================

        public String suggestFileName(
                        String originalName,
                        String content) {

                if (originalName == null) {
                        originalName = "file";
                }

                if (content == null) {
                        content = "";
                }

                String prompt = SYSTEM_PROMPT

                                + "\n\n"
                                + "=========================================================\n"
                                + "AI FILE RENAME MODE\n"
                                + "=========================================================\n\n"

                                + "Analyze the following file and suggest ONE "
                                + "short, professional and descriptive filename.\n\n"

                                + "ORIGINAL FILE NAME:\n"
                                + originalName

                                + "\n\n"
                                + "FILE CONTENT:\n"
                                + content

                                + "\n\n"
                                + "STRICT RULES:\n"

                                + "1. Return ONLY the new filename.\n"
                                + "2. Do not provide explanations.\n"
                                + "3. Do not use quotation marks.\n"
                                + "4. Do not include the file extension.\n"
                                + "5. Keep the name concise.\n"
                                + "6. Describe the actual content.\n"
                                + "7. Do not use special characters unnecessarily.";

                return sendTextRequest(
                                prompt,
                                TEXT_MODEL)
                                .replace("\"", "")
                                .replace("'", "")
                                .replace("\n", " ")
                                .replace("\r", " ")
                                .trim();
        }
}