package com.future.ai.config;

import com.future.ai.repository.ConsultantChatMemoryStore;
import com.future.ai.repository.CreateChatMemoryStore;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.ClassPathDocumentLoader;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class CommonConfig {

    @Resource
    private EmbeddingModel embeddingModel;

    @Resource
    private CreateChatMemoryStore createChatMemoryStore;

    @Resource
    private ConsultantChatMemoryStore consultantChatMemoryStore;

//    @Bean
//    public ChatMemoryStore createChatMemoryStore() {
//        return new CreateChatMemoryStore();
//    }
    @Bean
    public ChatMemoryProvider createChatMemoryProvider(){
        return new ChatMemoryProvider() {
            @Override
            public ChatMemory get(Object memoryId) {
                return MessageWindowChatMemory.builder()
                        .id(memoryId)
                        .maxMessages(20)
                        .chatMemoryStore(createChatMemoryStore)
                        .build();
            }
        };
    }

//    @Bean
//    public ChatMemoryStore consultantChatMemoryStore() {
//        return new ConsultantChatMemoryStore();
//    }
    @Bean
    public ChatMemoryProvider consultantChatMemoryProvider(){
        return new ChatMemoryProvider() {
            @Override
            public ChatMemory get(Object memoryId) {
                return MessageWindowChatMemory.builder()
                        .id(memoryId)
                        .maxMessages(20)
                        .chatMemoryStore(consultantChatMemoryStore)
                        .build();
            }
        };
    }

//    //构建向量数据库操作对象
//    @Bean
//    public EmbeddingStore store(){//依赖中有个embeddingStore对象，名字不能重复，所以设置成store
//        // 加载文档进内存
//        List<Document> documents = ClassPathDocumentLoader.loadDocuments("content", new ApachePdfBoxDocumentParser());
//        // 构建文档分割器对象
//        DocumentSplitter ds = new DocumentByParagraphSplitter(500, 50);
//        // 构建向量数据库操作对象
//        InMemoryEmbeddingStore store = new InMemoryEmbeddingStore();
//        // 构建一个EmbeddingStoreIngestor对象，完成文本数据切割，向量化，存储
//        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
//                .embeddingStore(store)
//                // 指定文档分割器
//                .documentSplitter(ds)
//                .embeddingModel(embeddingModel)
//                .build();
//        ingestor.ingest(documents);
//        return store;
//    }
//    //构建向量数据库检索对象
//    @Bean
//    public ContentRetriever contentRetriever(EmbeddingStore store){
//        return EmbeddingStoreContentRetriever.builder()
//                .embeddingStore(store)
//                .embeddingModel(embeddingModel)
//                .minScore(0.5)
//                .maxResults(3)
//                .build();
//    }



}
