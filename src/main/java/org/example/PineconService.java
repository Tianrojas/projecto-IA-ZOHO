package org.example;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pinecone.PineconeEmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.json.JSONObject;
import static dev.langchain4j.model.openai.OpenAiEmbeddingModelName.TEXT_EMBEDDING_3_SMALL;

/**
 * This class is a placeholder for services related to Pinecone, a vector database service.
 * Currently, the class does not implement any specific functionality but can be extended
 * to interact with Pinecone APIs or services.
 */
public class PineconService {

    private static EmbeddingStore<TextSegment> embeddingStore;

    /**
     * This method performs a semantic search using a prompt in the Pinecone vector database
     * and returns a {@link JSONObject} containing the search results or an error message.
     *
     * The method utilizes an embedding model to convert the input prompt into a vector representation,
     * which is then searched against the vectors stored in the specified Pinecone index and namespace.
     * If relevant matches are found, they are used to construct a response,
     * which is returned as part of the JSON result.
     *
     * @param apiKey        The API key required to authenticate and access the Pinecone service.
     *                      This key ensures secure access to the vector database.
     * @param index         The name of the Pinecone index where the vector data is stored.
     *                      The index is the primary collection of vectors used for the search operation.
     * @param nameSpace     The namespace within the Pinecone index that groups records into logical partitions.
     *                      This is useful for segmenting data into distinct categories, enabling more targeted searches.
     * @param prompt        The input text or query that is transformed into an embedding vector
     *                      and used to perform the search operation in Pinecone.
     * @return              A {@link JSONObject} containing the search results. If matching vectors are found,
     *                      the results are included in the response, along with relevant data from Pinecone.
     *                      If no matches are found, the JSONObject includes an error message.
     * @throws Exception    This method may throw an {@link Exception} if there are issues during the embedding process,
     *                      connecting to the Pinecone API, or if the search operation encounters an error.
     */


    public static JSONObject searchVectorPinecone(String openAApikey,String apiKey, String index, String nameSpace, String prompt) throws Exception{

        embeddingStore = PineconeEmbeddingStore.builder()
                .apiKey(apiKey)         //"19199b7e-571d-4dd3-aee6-c3397cbc1b97"
                .index(index)           //"knowledge-test2-llmhugginface"
                .nameSpace(nameSpace)   //"dev.langchain4j.store.embedding.pinecone.PineconeServerlessIndexConfig@7d38aed2"
                .build();

        EmbeddingModel embeddingModel = OpenAiEmbeddingModel.builder()
                .apiKey(openAApikey)
                .modelName(TEXT_EMBEDDING_3_SMALL)
                .build();

        Embedding queryEmbedding = embeddingModel.embed(prompt).content();
        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(1)
                .build();
        EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);
        System.out.println("Resultados de la búsqueda: " + searchResult.matches());

        JSONObject jsonResponse = new JSONObject();

        try {
            if (!searchResult.matches().isEmpty()) {
                String retrievedText = searchResult.matches().get(0).embedded().text();
                String queryWithKnowledge = "Basado en la informacion de la base de datos, responde la pregunta: " + prompt +
                        "\n\n Informacion de la base de datos: \n" + retrievedText +
                        ". ¿Puedes responder el prompt solo con la informacion encontrada?";
                jsonResponse.put("queryWithKnowledge", queryWithKnowledge);
            } else {
                jsonResponse.put("error", "No se encontró ningún resultado en Pinecone.");
            }
        } catch (Exception e) {
            jsonResponse.put("exception", "Ocurrió un error al procesar la búsqueda: " + e.getMessage());
        }

        return jsonResponse;
    }


    /**
     * The main method serves as the entry point for the PineconService class.
     * Currently, it does not perform any actions but can be used to test or demonstrate
     * future functionality related to Pinecone services.
     *
     * @param args Command-line arguments (not used).
     * @throws Exception If an error occurs during execution (currently no exceptions are expected).
     */
    public static void main(String[] args) throws Exception {
    }

}
