package co.com.minvivienda.rnec;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

public class RnecUtil {

	
    
    public static Map<Integer, String> getEstadosCedulas(){
    	Map<Integer, String> estados = new LinkedHashMap<Integer, String>();
    	estados.put(0, "Vigente");
    	estados.put(1, "Vigente");
    	estados.put(12, "Vigente con Pérdida o Suspensión de los Derechos Políticos");
    	estados.put(14, "Vigente con Interdicción Judicial");
    	estados.put(21, "Cancelada por Muerte");
    	estados.put(22, "Cancelada por Doble Cedulación");
    	estados.put(23, "Cancelada por Suplantación");
    	estados.put(24, "Cancelada por Menoría de Edad");
    	estados.put(25, "Cancelada por Extranjería");
    	estados.put(26, "Cancelada por Mala Elaboración");
    	estados.put(27, "Cancelada con reasignación de cupo numérico");
    	estados.put(28, "Cancelada por Extranjería sin Carta de Naturaleza");
    	estados.put(51, "Cancelada por Muerte Facultad Ley");
    	estados.put(52, "Cancelada por Intento de Doble Cedulación NO Expedida");
    	estados.put(53, "Cancelada por Falsa Identidad");
    	estados.put(54, "Cancelada por Menoría de Edad NO Expedida");
    	estados.put(55, "Cancelada por Extranjería NO Expedida");
    	estados.put(56, "Cancelada por Mala Elaboración No Expedida");
    	estados.put(88, "Pendiente Solicitud en Reproceso");
    	estados.put(99, "Pendiente por estar en Proceso de Expedición");
    	
    	return estados;
    }
    
    
    public static List<Integer> getEstadoCedulas(){
    	List<Integer> estados = new ArrayList<Integer>();
    	estados.add(0);
    	estados.add(1);
    	estados.add(12);
    	estados.add(14);
    	estados.add(21);
    	estados.add(22);
    	estados.add(23);
    	estados.add(24);
    	estados.add(25);
    	estados.add(26);
    	estados.add(27);
    	estados.add(28);
    	estados.add(51);
    	estados.add(52);
    	estados.add(53);
    	estados.add(54);
    	estados.add(55);
    	estados.add(56);
    	estados.add(88);
    	estados.add(99);
    	
    	return estados;
    }
    
    /**
     * 
     * @return
     */
    public static Integer getEstadoRandom() {
    	Random random = new Random();
    	int posEstado = random.nextInt(20);
    	List<Integer> estados = getEstadoCedulas();
    	return estados.get(posEstado);
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
	public static String templateParser(InputStream file) {
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
	public static String getDatosCedulas(List<String> cedulas){
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
    public static byte[] readImageFromClasspath(String fileName) throws IOException {
        Resource resource = new ClassPathResource(fileName);
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
