/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package com.seldon.mih;


import com.google.common.base.Predicates;
import com.seldon.mih.mr.TfIdf;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@SpringBootApplication
@EnableWebSocket
@EnableSwagger2

/**
 * Run this as command line application.
 *  ./gradlew build
 *   java -jar build/libs/MostImportantHT-0.0.1-SNAPSHOT.jar /Users/manigandanm/Documents/test-docs/ /tmp
 *
 *   in my laptop /Users/manigandanm/Documents/test-docs/ is the source path
 *   /tmp is the destination path
 *   Run as per configuration in your laptop
 */
public class App {

    public static void main(String[] args) throws Exception {
        System.out.println("Seldon Analytics Engine .....");
        // SpringApplication.run(App.class, args);
        System.out.println(args.length);

        if(args.length < 2) {
            System.out.println(" Missing arguments, source path and destination path required");
            return;
        }
        TfIdf tfIdf = new TfIdf();
        tfIdf.compute(args[0]);
        tfIdf.toCSV(args[1]);
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


