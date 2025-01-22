package co.com.minvivienda.middleware.exception;

public class ServiceException extends Exception{

   private static final long serialVersionUID = 1L;
   private final int codigoError;
  
   /**
    * Constructor de la clase que invoca al metodo getMessage()
    * @param msg Mensaje de error que es parseado para retornar el error customizado
    */
   public ServiceException(String msg) {
      super(msg);
      codigoError=Integer.parseInt(msg);
   }  
   
   /**
    * Metodo que retorna el mensaje de error customizado dependiendo del tipo
    * @return un mensaje de error
    */
   @Override
   public String getMessage(){
	   switch(codigoError){
	   case 400:
		   return "Bad request";
	   case 453:
		   return "No Data Found";
	   case 452 :
		   return "Exceeds the maximum length";
	   case 454:
		   return "Invalid Data Type";
	   case 455:
		   return "DataBase Error Exception";
	   case 456:
		   return "Api EspoCRM Bad Response";
	   case 458:
		   return "Gateway Internal Error";
			   
		default :
			return "";
	   }
   }
   
   /**
    * Recibe el codigo de error y lo retorna
    * @return un entero que especifica el codigo de error
    */
   public int getCodigoError() {
		 return codigoError;
   }

  
}