package org.api.controller;
import org.api.services.PineconService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.json.JSONObject;

@RestController
@RequestMapping("/api")

    /**
     * Endpoint to perform a semantic search using Pinecone and generate a response with OpenAI.
     *
     * @param openAiApiKey   API key for OpenAI.
     * @param pineconeApiKey API key for Pinecone.
     * @param index          Pinecone index name where vector data is stored.
     * @param nameSpace      Namespace within the Pinecone index.
     * @param prompt         Query prompt to be processed.
     * @param temperature    Temperature for OpenAI response generation.
     * @return               JSON response containing the generated response or an error message.
     */

public class PineconeController {
    private final PineconService pineconeService;

    public PineconeController(PineconService pineconeService) {
        this.pineconeService = pineconeService;
    }

    @PostMapping("/search")
    public ResponseEntity<?> searchOnPinecone(
            @RequestParam String openAiApiKey,
            @RequestParam String pineconeApiKey,
            @RequestParam String index,
            @RequestParam String nameSpace,
            @RequestParam String prompt,
            @RequestParam Double temperature) {
        try {
            JSONObject jsonResponse = pineconeService.performSearch(openAiApiKey, pineconeApiKey, index, nameSpace, prompt, temperature);
            return ResponseEntity.ok(jsonResponse.toString());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    /**
     * Endpoint to perform a semantic search using Pinecone without generating a response.
     *
     * @param openAiApiKey   API key for OpenAI.
     * @param pineconeApiKey API key for Pinecone.
     * @param index          Pinecone index name where vector data is stored.
     * @param nameSpace      Namespace within the Pinecone index.
     * @param prompt         Query prompt to be processed.
     * @return               JSON response containing the search result or an error message.
     */
    @PostMapping("/pinecone-search")
    public ResponseEntity<?> searchVectorPineconeEndpoint(
            @RequestParam String openAiApiKey,
            @RequestParam String pineconeApiKey,
            @RequestParam String index,
            @RequestParam String nameSpace,
            @RequestParam String prompt) {
        try {
            JSONObject jsonResponse = pineconeService.searchVectorPinecone(openAiApiKey, pineconeApiKey, index, nameSpace, prompt);
            return ResponseEntity.ok(jsonResponse.toString());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/test")
    public String test() {
        return "Hello";
    }

}
