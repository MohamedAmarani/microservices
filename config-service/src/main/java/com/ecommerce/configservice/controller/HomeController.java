package com.ecommerce.configservice.controller;

import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/")
@Service
public class HomeController {
    @Autowired
    private Environment env;

    @Value("${eureka.instance.instance-id}")
    private String instanceId;

    @GetMapping("/info")
    @ApiOperation(value = "Get information from the config-service instance", notes = "Retrieve information from a config-service instance")
    public String getInfo() {
        return "Hello from Config Service running at port: " + env.getProperty("local.server.port") +
                " InstanceId " + instanceId;
    }
}