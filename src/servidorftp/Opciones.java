/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package servidorftp;

/**
 *
 * @author alberto
 */
        
public class Opciones {
    static final String[] options = new String[]{"UPLOAD", "DOWNLOAD", "CLOSE"};
    
    Opciones(){}
    
    public static String getMenu(){
         return " Elige una opcion: - "
            + "1. Subir archivo al servidor. - "
            + "2. Bajar archivo del servidor. - "
            + "3. Salir de la aplicacion.\n";
    }
    
    public static String getSelect(int i){
        return options[i];
    }
    
    public static int getSelect(String line){
        int i=0;
        while(i<options.length){
            if(options[i] == null ? line == null : options[i].equals(line)) break;
            else i++;
        }
        if(i==options.length) i=-1;
        return i+1;
    }
}
