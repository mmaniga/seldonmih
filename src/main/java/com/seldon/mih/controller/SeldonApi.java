package com.seldon.mih.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(path = "/seldon/api")
public class SeldonApi {

    @Autowired
    ObjectMapper objectMapper;

    @PostMapping("/getTopMIH")
    @ApiOperation("Get Top 10 Words List")
    public Map<String, Integer> getTopMIT(
            @RequestBody String requestBody,
            @RequestHeader Map<String, String> headers) {
        Map<String, Integer> wordCount = new HashMap<>();
        System.out.println("SeldonApi.TransportMessageToCS");

        return wordCount;
    }
}


