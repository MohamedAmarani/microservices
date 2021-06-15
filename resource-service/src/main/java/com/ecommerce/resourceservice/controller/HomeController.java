package com.ecommerce.resourceservice.controller;

import com.ecommerce.resourceservice.model.Resource;
import com.ecommerce.resourceservice.repository.ResourceRepository;
import com.google.common.util.concurrent.AtomicDouble;
import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.io.FileUtils;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.env.Environment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@RefreshScope
@RestController
@RequestMapping("/")
@Service
public class HomeController {
    @Autowired
    private Environment env;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ResourceRepository resourceRepository;

    @Value("${message:Hello default}")
    private String message;

    @Value("${eureka.instance.instance-id}")
    private String instanceId;

    @Autowired
    private DiscoveryClient discoveryClient;

    private AtomicDouble ref;

    private Map<String, Integer> requestsLastMinute = new HashMap<>();

    public HomeController(MeterRegistry registry) {
        ref = registry.gauge("requests_per_second", new AtomicDouble(0.0f));
    }

    @PostConstruct
    public void getSetup() {
        //inicializar las 60 posiciones con 0s
        for (int i = 0; i < 60; ++i) {
            String key = Integer.toString(i);
            requestsLastMinute.put(key.length() < 2 ? "0" + key : key, 0);
        }
        //actualizando campos de los segundos cada segundo
        Timer t = new Timer();
        t.schedule(new TimerTask() {
            @Override
            public void run() {
                //borrar el segundo que viene
                String timeStamp = new SimpleDateFormat("ss").format(Calendar.getInstance().getTime());
                String timeStampIncremented = Integer.toString(Integer.parseInt(timeStamp) + 1);
                requestsLastMinute.put(Integer.parseInt(timeStamp) + 1 < 60 ?
                        (timeStampIncremented.length() < 2 ? "0" + timeStampIncremented : timeStampIncremented) : "00", 0);
            }
        }, 0, 1000);

        //actualizando el valor de micrometer cada 3 segundos
        Timer t1 = new Timer();
        t1.schedule(new TimerTask() {
            @Override
            public void run() {
                //calcular las requests por segundo en el ultimo minuto
                int counter = 0;
                for (String key: requestsLastMinute.keySet()) {
                    counter += requestsLastMinute.get(key);
                    //System.out.println(key);
                }
                ref.set(counter / (double) 60);
            }
        }, 0, 3000);
    };

    private synchronized void incrementCounter() {
        String timeStamp = new SimpleDateFormat("ss").format(Calendar.getInstance().getTime());
        requestsLastMinute.put(timeStamp, requestsLastMinute.get(timeStamp) + 1);
    }

    @GetMapping("")
    @ApiOperation(value = "Get all resources", notes = "Retrieve a specific resource from the Database")
    public List<Resource> getResources() {
        incrementCounter();
        List<Resource> resources = resourceRepository.findAll();
        return resources;
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "Get a resource", notes = "Provide the Id of the specific resource to retrieve from the Database")
    public Resource getResource(@ApiParam(value = "Id of the resource to get", required = true) @PathVariable final String id) throws Exception {
        Resource resource = null;
        //si no existe ningun producto con ese id retornamos null
        try {
            //throw new Exception("Images can't be fetched");
            resource = resourceRepository.findById(id).get();
        } catch (Exception e1) {
            try {
                resource = resourceRepository.findByName(id).get();
            } catch (Exception e2) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Resource not found"
                );
            }
        }
        incrementCounter();
        return resource;
    }

    @PostMapping("")
    @ApiOperation(value = "Create a resource", notes = "Provide information to create a resource")
    public Resource postResource(@ApiParam(value = "Resource to create", required = true) @RequestBody Map<String, String> resourceBody) throws IOException {
        incrementCounter();
        Resource resource = new Resource();
        resource.setName(resourceBody.get("name"));
        resource.setDescription(resourceBody.get("description"));
        String data;

        //si es url descargar imagen
        if (resourceBody.get("url") != null) {
            //descargar archivo temporal
            File file = new File("C:/Users/moha1/Pictures/eCommerceSaas/" + resource.getName());
            FileUtils.copyURLToFile(new URL(resourceBody.get("url")), file, 0, 0);

            //codificar archivo temporal a base64
            byte[] encoded = Base64.encodeBase64(FileUtils.readFileToByteArray(file));
            data = new String(encoded, StandardCharsets.US_ASCII);

            //eliminar archivo temporal
            file.delete();
        }
        else {
            data = resourceBody.get("data");
        }
        resource.setData(data);
        try {
            resource = resourceRepository.save(resource);
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Duplicated key"
            );
        }
        return resource;
    }

    @DeleteMapping("/{id}")
    @ApiOperation(value = "Delete a resource", notes = "Provide an Id to delete a specific resource from the Database")
    public Resource deleteResource(@ApiParam(value = "Id of the resource to delete", required = true) @PathVariable final String id) throws Exception {
        Resource resource = null;
        //si no existe ningun resource con ese id retornamos null
        try {
            //throw new Exception("Images can't be fetched");
            resource = resourceRepository.findById(id).get();
            resourceRepository.deleteById(id);
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Resource not found"
            );
        }
        incrementCounter();
        return resource;
    }

    @GetMapping("/{id}/download")
    @ApiOperation(value = "Download a resource", notes = "Provide the Id of the specific resource to download from the Database")
    public void downloadResource(@ApiParam(value = "Id of the resource to download", required = true) @PathVariable final String id,
                                 HttpServletResponse response) throws Exception {
        incrementCounter();
        Resource resource = null;
        //si no existe ningun producto con ese id retornamos null
        try {
            //throw new Exception("Images can't be fetched");
            resource = resourceRepository.findById(id).get();
        } catch (Exception e1) {
            try {
                resource = resourceRepository.findByName(id).get();
            } catch (Exception e2) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Resource not found"
                );
            }
        }
        try {
            byte[] data = Base64.decodeBase64(resource.getData());

            try (OutputStream stream = new FileOutputStream(resource.getName())) {
                stream.write(data);
            }
            // get your file as InputStream
            InputStream is = new FileInputStream(resource.getName());
            // copy it to response's OutputStream
            org.apache.commons.io.IOUtils.copy(is, response.getOutputStream());
            response.flushBuffer();
            new File(resource.getName()).delete();

        } catch (IOException ex) {
            throw new RuntimeException("Could not find the given file");
        }
    }

    @GetMapping("/admin")
    public String homeAdmin() {
        incrementCounter();
        return "This is the admin area of resource service running at port: " + env.getProperty("local.server.port");
    }
}