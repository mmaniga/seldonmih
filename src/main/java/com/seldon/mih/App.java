/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package com.seldon.mih;


import com.google.common.base.Predicates;
import com.seldon.mih.model.TermFrequenceIDF;
import com.seldon.mih.mr.FileUtil;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.io.FileWriter;

@SpringBootApplication
@EnableWebSocket
@EnableSwagger2

public class App {

    public static void main(String  [] args) throws Exception {
        System.out.println("Seldon Analytics Engine .....");
       //SpringApplication.run(App.class, args);

        String dPath = "/Users/manigandanm/Documents/workspace/cloudbroker-poc/seldon/src/main/resources/test-docs";
        FileUtil.toFrequencyMap(dPath);
        System.out.println(FileUtil.wordFreqMap);
        System.out.println(FileUtil.termFrequencyMap);
        System.out.println(FileUtil.docFreqMap);
        System.out.println(FileUtil.inverseDocumentFrequency);
        System.out.println(FileUtil.tdIDFVector);

        System.out.println("Writiing to output file");
        FileWriter fileWriter = new FileWriter("/tmp/seldon-tfidf.csv",true);
        for (TermFrequenceIDF entry : FileUtil.tdIDFVector)
            fileWriter.write(String.format("%s,%s,%d,%f,%f\n",entry.term,entry.doc,entry.count,entry.tf,entry.tfIDF));
        fileWriter.close();
    }

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.any())
                .apis(Predicates.not(RequestHandlerSelectors.basePackage("org.springframework.boot")))
                .apis(Predicates.not(RequestHandlerSelectors.basePackage("org.springframework.cloud")))
                .apis(Predicates.not(RequestHandlerSelectors.basePackage("org.springframework.data.rest.webmvc")))
                .paths(PathSelectors.any())
                .build();
    }
}


