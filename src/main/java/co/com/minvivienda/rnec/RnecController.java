package co.com.minvivienda.rnec;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;


@RestController
//@RequestMapping("/rnec")
@CrossOrigin(origins = "*")
public class RnecController {
	
	
    @PostMapping(value = "/consultarCedulas", produces = {"application/json"}, consumes = {"application/json"})
    @CrossOrigin(origins = "*")
    public ResponseEntity<?> consultarCedulas(@RequestBody String jsonRequest)  throws Exception {
    	
    	try {
            ObjectMapper objectMapper = new ObjectMapper();
            
    		try {
    			objectMapper.readValue(jsonRequest, new TypeReference<Map<String, String>>(){});
    		} catch (JsonProcessingException e) {
    			e.printStackTrace();
    			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"response\":\"error\": \"message\":\"Los datos de entrada no son correctos\"}");
    		}
    		
    		InputStream file = RnecController.class.getClassLoader().getResourceAsStream("velocity/rnec-response.vm");
    		String xmlOutputBody = templateParser(file);
    		String jsonResponse = xmlToJson(xmlOutputBody);
        	
        	return ResponseEntity.status(HttpStatus.OK).body(jsonResponse);
    	}catch(Exception ex) {
    		ex.printStackTrace();
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"response\":\"error\": \"message\":\"Error interno del servidor\"}");
    	}
    }
    
    
   
    
    /**
     * Convierte una cadena en formato XML a formato JSON
     * 
     * @param xml
     * @return
     */
    public static String xmlToJson(String xml) {
        try {
            XmlMapper xmlMapper = new XmlMapper();
            JsonNode node = xmlMapper.readTree(xml.getBytes());
            
            ObjectMapper jsonMapper = new ObjectMapper();
            return jsonMapper.writeValueAsString(node);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    
    
    public static String jsonToXmlBody(String documentJson) {
		InputStream file = RnecController.class.getClassLoader().getResourceAsStream("velocity/soap-createReceived-body2.vm");
		String xmlInputBody = templateParser(file);
		
    	Map<String, String> jsonInputMap =  new HashMap<>();;
    	
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode rootNode = objectMapper.readTree(documentJson);
	        flattenJson("", rootNode, jsonInputMap);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			return null;
		}
		
		System.out.println("******* jsonInputMap ********");
		System.out.println(jsonInputMap);
		
		
		Map<String, String> templateValues = new HashMap<String, String>();
		templateValues.put("targetDependence.code", (String) jsonInputMap.get("targetDependence.code"));
		templateValues.put("sourceThirdPerson.identification", (String) jsonInputMap.get("sourceThirdPerson.identification"));
		templateValues.put("sourceThirdPerson.address", (String) jsonInputMap.get("sourceThirdPerson.address"));
		templateValues.put("sourceThirdPerson.municipality.code", (String) jsonInputMap.get("sourceThirdPerson.municipality.code"));	                
		templateValues.put("type.id", (String) jsonInputMap.get("type.id"));
		templateValues.put("targetUser.identification", (String) jsonInputMap.get("targetUser.identification"));
		templateValues.put("sourceThirdPerson.name", (String) jsonInputMap.get("sourceThirdPerson.name"));
		templateValues.put("sourceThirdPerson.lastname", (String) jsonInputMap.get("sourceThirdPerson.lastname"));
		templateValues.put("sourceThirdPerson.email", (String) jsonInputMap.get("sourceThirdPerson.email"));
		templateValues.put("reference", (String) jsonInputMap.get("reference"));
		templateValues.put("observations", (String) jsonInputMap.get("observations"));
		templateValues.put("authorDependence.code", (String) jsonInputMap.get("authorDependence.code"));
		templateValues.put("sourceThirdPerson.phone", (String) jsonInputMap.get("sourceThirdPerson.phone"));
		templateValues.put("author.identification", (String) jsonInputMap.get("author.identification"));
		templateValues.put("sourceThirdPerson.identificationType.name", (String) jsonInputMap.get("sourceThirdPerson.identificationType.name"));
		
		
		Iterator<Map.Entry<String, String>> itJson = templateValues.entrySet().iterator();
        Map.Entry<String, String> pair = null;
        while (itJson.hasNext()) {
            pair = itJson.next(); 
        	Pattern p = Pattern.compile("#\\{"+pair.getKey()+"\\}", Pattern.MULTILINE);
        	Matcher m = p.matcher(xmlInputBody);
        	xmlInputBody = m.replaceAll(pair.getValue() == null ? "-" : pair.getValue());
        }
        
		return xmlInputBody;
    }
    
    
    
    private static void flattenJson(String parentKey, JsonNode node, Map<String, String> result) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String newKey = parentKey.isEmpty() ? field.getKey() : parentKey + "." + field.getKey();
                flattenJson(newKey, field.getValue(), result);
            }
        } else if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                String newKey = parentKey + "[" + i + "]";
                flattenJson(newKey, node.get(i), result);
            }
        } else {
            result.put(parentKey, node.asText());
        }
    }
    
    /**
     * Extrae el mensaje de fallo (nodo faultstring del XML de respuesta) cuando la busqueda no retorna ningun resultado
     * 
     * @param xml
     * @return
     */
    public static String extractFaultString(String xml) {
        try {
            XmlMapper xmlMapper = new XmlMapper();
            JsonNode node = xmlMapper.readTree(xml.getBytes());
            ObjectMapper jsonMapper = new ObjectMapper();
            String valueAsString = jsonMapper.writeValueAsString(node);
            Map<String, Map<String, Object>> configReglaMap = jsonMapper.readValue(valueAsString, new TypeReference<Map<String, Map<String, Object>>>(){});            
            Map<String, Object> body = (Map<String, Object>) configReglaMap.get("Body");
            Map<String, String> fault = (Map<String, String>) body.get("Fault");
            ObjectMapper jsonMapper2 = new ObjectMapper();
            return jsonMapper2.writeValueAsString(fault);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 
     * @param file
     * @return
     */
	private static String templateParser(InputStream file) {
		StringBuilder templateContent = new StringBuilder();
		
		try {
			String line = "";
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(file));
			while ((line = bufferedReader.readLine()) != null) {
				templateContent.append(line);
			}
			
			bufferedReader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return templateContent.toString();
	}
    
    /**
     * 
     * @param fileName
     * @return
     * @throws IOException
     */
    public byte[] readImageFromClasspath(String fileName) throws IOException {
        Resource resource = new ClassPathResource(fileName);
        
        //System.out.println("exists = " + resource.exists());
        //System.out.println("contentLength = " + resource.contentLength());
        //System.out.println(resource.getURI());
        try (InputStream inputStream = resource.getInputStream()) {
            
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[inputStream.available()];
            int bytesRead;
            while ((bytesRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, bytesRead);
            }
            
            return buffer.toByteArray();
        }
    }
    
}