package co.com.minvivienda.rnec;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;


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
    		String xmlOutputBody = RnecUtil.templateParser(file);
    		List<String> cedulas = inputMap.get("nuip"); 
    		String datosCedulas = RnecUtil.getDatosCedulas(cedulas);
    		xmlOutputBody = xmlOutputBody.replace("#DATOS_CEDULAS", datosCedulas);
    		String jsonResponse = RnecUtil.xmlToJson(xmlOutputBody);
    		
        	return ResponseEntity.status(HttpStatus.OK).body(jsonResponse);
    	}catch(Exception ex) {
    		ex.printStackTrace();
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"response\":\"error\": \"message\":\"Error interno del servidor\"}");
    	}
    }
    
    
   
    
	/**
	 * 
	 * @param jsonRequest
	 * @return
	 * @throws Exception
	 */
    @PostMapping(value = "/validarCedulas", produces = {"application/json"}, consumes = {"application/json"})
    @CrossOrigin(origins = "*")
    public ResponseEntity<?> validarCedulas(@RequestBody Map<String, Object> jsonRequest)  throws Exception {
    	List<Map<String, Object>> postulaciones = (List<Map<String, Object>>) jsonRequest.get("postulaciones");
    	
    	List<Map<String, Object>> miembrosHogar = null;
    	List<Map<String, Object>> postulacionesCumplen = new ArrayList<Map<String, Object>>();
    	List<Map<String, Object>> postulacionesNoCumplen = new ArrayList<Map<String, Object>>();
    	Map<String, Object> resultadoMiembroHogar = null;
    	List<Map<String, Object>> resultadoMiembroHogarList = null;
    	Map<String, Object> resultadoPostulacion = null;
    	
    	int estadoRandom = 0;
    	Map<Integer, String> estadosCedulas = RnecUtil.getEstadosCedulas();
    	int postulacionResultado = 1;
    	
    	for(Map<String, Object> postulacion : postulaciones) {
    		postulacionResultado = 1;
    		resultadoPostulacion = new LinkedHashMap<String, Object>();
    		resultadoPostulacion.put("PostulanteId", postulacion.get("PostulanteId"));
    		resultadoMiembroHogarList = new ArrayList<Map<String, Object>>();
        	
    		miembrosHogar = (List<Map<String, Object>>) postulacion.get("MiembrosHogar");
    		for(Map<String, Object> miembroHogar : miembrosHogar) {
    			estadoRandom = RnecUtil.getEstadoRandom();
    			
    			resultadoMiembroHogar = new LinkedHashMap<String, Object>();
    			resultadoMiembroHogar.put("PostulanteId", miembroHogar.get("PostulanteId"));
    			resultadoMiembroHogar.put("MiembroHogarId", miembroHogar.get("MiembroHogarId"));
    			resultadoMiembroHogar.put("TipoDocumentoCatId", miembroHogar.get("TipoDocumentoCatId"));
    			resultadoMiembroHogar.put("NumeroDocumento", miembroHogar.get("NumeroDocumento"));
    			resultadoMiembroHogar.put("Valor", estadoRandom);
    			resultadoMiembroHogar.put("Descripcion", estadosCedulas.get(estadoRandom));
    			
        		if(estadoRandom < 21) {
        			resultadoMiembroHogar.put("Resultado", 1);
        		}else {
        			postulacionResultado = 0;
        			resultadoMiembroHogar.put("Resultado", 0);
        		}
        		
        		resultadoMiembroHogarList.add(resultadoMiembroHogar);
    		}
    		
    		resultadoPostulacion.put("Resultado", postulacionResultado);
    		resultadoPostulacion.put("MiembrosHogar", resultadoMiembroHogarList);
    		
    		if(postulacionResultado == 1) {
    			postulacionesCumplen.add(resultadoPostulacion);
    		}else {
    			postulacionesNoCumplen.add(resultadoPostulacion);
    		}	
    	}
    	
    	
    	Map<String, Object> response = new LinkedHashMap<String, Object>();
    	response.put("Postulacion.Cumplen", postulacionesCumplen);
    	response.put("Postulacion.NoCumplen", postulacionesNoCumplen);
    	
    	return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(response);
    }
    
}
