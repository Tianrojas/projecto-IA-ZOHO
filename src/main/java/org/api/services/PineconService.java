package org.api.services;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pinecone.PineconeEmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

/**
 * This class is a placeholder for services related to Pinecone, a vector database service.
 * Currently, the class does not implement any specific functionality but can be extended
 * to interact with Pinecone APIs or services.
 */
@Service
public class PineconService {

    private static EmbeddingStore<TextSegment> embeddingStore;

    /**
     * Performs a semantic search in Pinecone and generates a response using OpenAI.
     *
     * @param openAiApiKey   API key for OpenAI.
     * @param pineconeApiKey API key for Pinecone.
     * @param index          Pinecone index name where vector data is stored.
     * @param nameSpace      Namespace within the Pinecone index.
     * @param prompt         Query prompt to be processed.
     * @param temperature    Temperature for OpenAI response generation.
     * @return               JSON response containing the generated response.
     * @throws Exception     If an error occurs during processing.
     */


    public JSONObject performSearch(String openAiApiKey, String pineconeApiKey, String index, String nameSpace, String prompt, Double temperature) throws Exception {
        // Initialize OpenAI chat model
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .apiKey(openAiApiKey)
                .modelName("gpt-4")
                .temperature(temperature)
                .build();

        // Fetch knowledge from Pinecone
        JSONObject searchResult = searchVectorPinecone(openAiApiKey, pineconeApiKey, index, nameSpace, prompt);
        String queryWithKnowledge = searchResult.getString("queryWithKnowledge");

        // Generate response using OpenAI
        String responseWithKnowledge = chatModel.generate(queryWithKnowledge);

        // Create JSON response
        JSONObject jsonResponse = new JSONObject();
        jsonResponse.put("responseWithKnowledge", responseWithKnowledge);
        return jsonResponse;
    }

    /**
     * Performs a semantic search in Pinecone using an input prompt.
     *
     * @param openAiApiKey   API key for OpenAI.
     * @param pineconeApiKey API key for Pinecone.
     * @param index          Pinecone index name where vector data is stored.
     * @param nameSpace      Namespace within the Pinecone index.
     * @param prompt         Query prompt to be processed.
     * @return               JSON response containing the search result or an error message.
     * @throws Exception     If an error occurs during processing.
     */
    public JSONObject searchVectorPinecone(String openAiApiKey, String pineconeApiKey, String index, String nameSpace, String prompt) throws Exception {
        // Initialize Pinecone embedding store
        PineconeEmbeddingStore embeddingStore = PineconeEmbeddingStore.builder()
                .apiKey(pineconeApiKey)
                .index(index)
                .nameSpace(nameSpace)
                .build();

        // Initialize embedding model
        EmbeddingModel embeddingModel = OpenAiEmbeddingModel.builder()
                .apiKey(openAiApiKey)
                .modelName("text-embedding-3-small")
                .build();

        // Generate embedding for the prompt
        Embedding queryEmbedding = embeddingModel.embed(prompt).content();

        // Perform search in Pinecone
        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(1)
                .build();

        EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);

        // Prepare response
        JSONObject jsonResponse = new JSONObject();
        if (!searchResult.matches().isEmpty()) {
            String retrievedText = searchResult.matches().get(0).embedded().text();
            String queryWithKnowledge = "Basado en la informacion de la base de datos, responde la pregunta: " + prompt +
                    "\n\n Informacion de la base de datos: \n" + retrievedText +
                    ". ¿Puedes responder el prompt solo con la informacion encontrada?";
            jsonResponse.put("queryWithKnowledge", queryWithKnowledge);
        } else {
            jsonResponse.put("error", "No se encontró ningún resultado en Pinecone.");
        }
        return jsonResponse;
    }

}