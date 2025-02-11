package co.com.minvivienda.rnec;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

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
            Map<String, List<String>> inputMap = null;
            
    		try {
    			inputMap = objectMapper.readValue(jsonRequest, new TypeReference<Map<String, List<String>>>(){});
    		} catch (JsonProcessingException e) {
    			e.printStackTrace();
    			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"response\":\"error\": \"message\":\"Los datos de entrada no son correctos\"}");
    		}
    		
    		InputStream file = RnecController.class.getClassLoader().getResourceAsStream("velocity/rnec-response.vm");
    		String xmlOutputBody = templateParser(file);
    		List<String> cedulas = inputMap.get("nuip"); 
    		String datosCedulas = getDatosCedulas(cedulas);
    		xmlOutputBody = xmlOutputBody.replace("#DATOS_CEDULAS", datosCedulas);
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
     * Retorna un string con el contenido del archivo especificado
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
	 * Retorna un string en formato JSON con los datos de las cedulas especificadas  
	 * 
	 * @param cedulas
	 * @return
	 */
	private String getDatosCedulas(List<String> cedulas){
		String datosCedulas = "";
		if(cedulas != null && !cedulas.isEmpty()) {
			InputStream file = null;
			for(String cedula : cedulas) {
				try {
		    		file = RnecController.class.getClassLoader().getResourceAsStream("velocity/rnec-cedula-" + cedula + ".vm");
		    		if(file != null) {
		    			datosCedulas = datosCedulas + templateParser(file);
		    		}					
				}catch(Exception ex) {
					System.err.println("No existe el numero de cedula: " + ex.getMessage());
				}
			}
			
			try {
				if(file != null) {
					file.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}finally {
				file = null;
			}
		}
		
		return datosCedulas;
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