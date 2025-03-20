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
    	List<Map<String, Object>> miembrosHogar = null;
    	List<String> cedulas = new ArrayList<String>();
    	List<Map<String, Object>> miembrosHogarList =  new ArrayList<Map<String, Object>>();;
    	Number numeroDocumento = null;
    	List<Map<String, Object>> postulaciones = (List<Map<String, Object>>) jsonRequest.get("postulaciones");
    	
    	for(Map<String, Object> postulacion : postulaciones) {
    		
    		miembrosHogar = (List<Map<String, Object>>) postulacion.get("MiembrosHogar");
    		for(Map<String, Object> miembroHogar : miembrosHogar) {
    			numeroDocumento  = (Number) miembroHogar.get("NumeroDocumento");
    			cedulas.add(String.valueOf(numeroDocumento));
    			miembrosHogarList.add(miembroHogar);
    		}
    	}
    	
    	List<Map<String, Object>> postulacionesCumplen = new ArrayList<Map<String, Object>>();
    	List<Map<String, Object>> postulacionesNoCumplen = new ArrayList<Map<String, Object>>();
    	Map<String, Object> resultadoMiembroHogar = null;
    	int estadoRandom = 0;
    	Map<String, Object> miembroHogar = null;
    	Map<Integer, String> estadosCedulas = RnecUtil.getEstadosCedulas();
    	for(int i = 0; i < cedulas.size(); i++) {
    		estadoRandom = RnecUtil.getEstadoRandom();
			miembroHogar = miembrosHogarList.get(i);
			
			resultadoMiembroHogar = new LinkedHashMap<String, Object>();
			resultadoMiembroHogar.put("PostulanteId", miembroHogar.get("PostulanteId"));
			resultadoMiembroHogar.put("MiembroHogarId", miembroHogar.get("MiembroHogarId"));
			resultadoMiembroHogar.put("TipoDocumentoCatId", miembroHogar.get("TipoDocumentoCatId"));
			resultadoMiembroHogar.put("NumeroDocumento", miembroHogar.get("NumeroDocumento"));
			resultadoMiembroHogar.put("EstadoCedula", estadoRandom);
			resultadoMiembroHogar.put("DescripcionEstadoCedula", estadosCedulas.get(estadoRandom));
			
    		if(estadoRandom < 21) {
    			postulacionesCumplen.add(resultadoMiembroHogar);
    		}else {
    			postulacionesNoCumplen.add(resultadoMiembroHogar);
    		}	
    	}
    	
    	
    	Map<String, Object> response = new LinkedHashMap<String, Object>();
    	response.put("Postulacion.Cumplen", postulacionesCumplen);
    	response.put("Postulacion.NoCumplen", postulacionesNoCumplen);
    	
    	return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(response);
    }
    
}
