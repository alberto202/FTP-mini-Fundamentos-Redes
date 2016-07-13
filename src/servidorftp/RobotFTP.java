package servidorftp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Jose Manuel Martinez de la Insua
 * @author Alberto Moreno Mantas
 * 
 */
public class RobotFTP extends Thread{
    
    private boolean conect;
    
    private Socket socketServicio;
    
    private BufferedInputStream objetoLeer;
    private BufferedOutputStream objetoEnviar;
    
    private PrintWriter outPrinter;
    private BufferedReader inReader;
    
    private DataInputStream datosEntrada;
    private DataOutputStream datosSalida;

    public RobotFTP(Socket socketServicio) {
        this.socketServicio = socketServicio;
        
    }
    
    @Override
    public void run() {
        this.conect = true;
        
        String user = null;
        String passwd = null;
        
        File archivo = null;
        FileReader fr = null;
        BufferedReader br = null;
        
        boolean logeado = false;
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date fecha = null;
        String line;
        
        try {
            // INICIALIZAMOS SERVICIOS
            this.inReader = new BufferedReader(new InputStreamReader(socketServicio.getInputStream()));
            this.outPrinter =  new PrintWriter(socketServicio.getOutputStream());
            
            //ESPERAR SALUDO DEL CLIENTE
            System.out.println(inReader.readLine());
            
            outPrinter.println("HELO");
            outPrinter.flush();
            
            while(conect){                    
                while(!logeado){
                    
                    // LEER USUARIO  Y PASSWORD
                    String[] vlineas ;
                    do{
                        line = inReader.readLine();
                        vlineas = line.split(" ");
                        if(vlineas.length != 2){
                            outPrinter.println("405"); //No permitido
                            outPrinter.flush(); 
                        }
                    }while(vlineas.length != 2);
                    
                    user = vlineas[0];
                    passwd = vlineas[1];
                    
                    try {
                       
                       // LEEMOS SI EL USUARIO ESTA EN NUESTRA BD
                       archivo = new File ("./basedatos.txt");
                       fr = new FileReader (archivo);
                       br = new BufferedReader(fr);

                       String linea;
                       while((linea=br.readLine())!=null && !logeado){
                           vlineas = linea.split(" ");
                           if(vlineas[0].equals(user) && vlineas[1].equals(passwd)){
                               logeado = true;
                           }
                       }
                       
                       // SE MANDA CONFIRMACION DE AUTENTIFICACION
                       if(!logeado)       outPrinter.println("401");    //No autorizado
                       else {
                            outPrinter.println(  "200"  );  //OK
                            fecha = new Date();
                            writerLogs("El usuario '" +user+"' se ha conectado. - " + dateFormat.format(fecha));
                       }
                       outPrinter.flush();   
                    
                    }catch(Exception e){
                    }finally{
                       try{                    
                          if( null != fr ){   
                             fr.close();     
                          }                  
                       }catch (Exception e2){}
                    }

                };    
                
                // LEER LA OPCION Y LA PROCESA
                do{
                    line = inReader.readLine();
                }while(line == null && !socketServicio.isClosed());
                
                procesar(Opciones.getSelect(line),user);
                
            }
        } catch (IOException ex) {
            this.conect = false;
            fecha = new Date();
            writerLogs("El usuario '" +user+"' desconectado inesperadamente. - " + dateFormat.format(fecha));

            System.err.println(ex.getMessage());
            System.err.println("ERROR : 417 Cliente cerrado inesperadamente.");
        }

        try {
            socketServicio.close();
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
            System.err.println("Sesión finalizada.");
        }
    }
    
    private void procesar(int leido,String user) throws IOException {
        String nombreArchivo = null;
        byte[] buffer = null;
        File f = null;
        f = new File("./DirectorioServidor/");    f.mkdirs();  
        
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date fecha = null;
        
        switch(leido){
            
            // SUBIDA DE FICHEROS AL SERVIDOR
            case 1:
                // MENU PARA SELECCION DE FICHERO
                outPrinter.println("201");
                outPrinter.flush();
                
                // RECOGIDA DE DATOS SOBRE EL FICHERO
                datosEntrada = new DataInputStream(socketServicio.getInputStream());
                nombreArchivo = datosEntrada.readUTF(); 
                buffer = new byte[datosEntrada.readInt()];
                
                // OBJETO_LEER -> CARGA EL FICHERO MANDADO POR EL CLIENTE
                // OBJETO_ENVIAR -> ESCRIBE EL FICHERO EN EL PC
                // OBJETO FILE PARA CREAR EL DIRECTORIO DONDE ALOJAR LOS ARCHIVOS
                f = new File("./DirectorioServidor/" + user); 
                if(!f.exists() || !f.isDirectory()) f.mkdirs();
                
                objetoLeer = new BufferedInputStream(socketServicio.getInputStream());
                objetoEnviar = new BufferedOutputStream(new FileOutputStream("./DirectorioServidor/"+ user + "/" + nombreArchivo));
                
                // LEO DEL BUFFER EL FICHERO BYTE A BYTE
                objetoLeer.read(buffer);
                                
                // ESCRIBO EL FICHERO EN EL PC
                objetoEnviar.write( buffer );
                objetoEnviar.flush();
                
                objetoEnviar.close();
                
                outPrinter.println("202");
                outPrinter.flush();
                
                fecha = new Date();
                writerLogs("El usuario '" +user+"' ha subido el fichero " + nombreArchivo + ". - " + dateFormat.format(fecha));
                
                break;
                
            // BAJADA DE FICHEROS DESDE EL SERVIDOR
            case 2:
                
                // MENU PARA SELECCION DE FICHERO
                outPrinter.println("201");
                outPrinter.flush();
                
                do{
                    // LEER EL FICHERO QUE SE DESEA DESCARGAR
                    nombreArchivo = inReader.readLine();

                    // SELECCION DE ARCHIVO PARA BAJAR
                    f = new File("./DirectorioServidor/" + user + "/" + nombreArchivo);
                    if(!f.exists()){
                        outPrinter.println("404");  //No encontrado
                        outPrinter.flush();
                    }
                    else{
                        outPrinter.println("102");  //Procesando
                        outPrinter.flush();
                    }
                }while(!f.exists());
                
                // SERVICIOS PARA BAJADA DE FICHERO
                // OBJETO_LEER -> CARGA FICHERO DESDE PC
                // OBJETO ENVIAR -> MANDA FICHERO AL CLIENTE
                // DATO_SALIDA -> INFORMACION DEL FICHERO
                objetoLeer = new BufferedInputStream(new FileInputStream(f));
                objetoEnviar = new BufferedOutputStream(socketServicio.getOutputStream());
                datosSalida = new DataOutputStream(socketServicio.getOutputStream());
                
                // ENVIO DE FICHERO Y DE DATOS ADJUNTOS
                datosSalida.writeUTF(f.getName());
                datosSalida.flush();
                datosSalida.writeInt((int) f.length());
                datosSalida.flush();
                
                buffer = new byte[(int) f.length()];
                objetoLeer.read(buffer);
                
                objetoEnviar.write(buffer);
                objetoEnviar.flush();
                
                objetoLeer.close();
                
                fecha = new Date();
                writerLogs("El usuario '" +user+"' ha bajado el fichero " + nombreArchivo + ". - " + dateFormat.format(fecha));
                
                outPrinter.println("202");
                outPrinter.flush();
                
                break;
            case 3:
                outPrinter.println("BYE.");
                outPrinter.flush();
                outPrinter.println("202");
                outPrinter.flush();
                this.conect = false;
                
                fecha = new Date();
                writerLogs("El usuario '" +user+"' ha desconectado. - " + dateFormat.format(fecha));

                break;
            default:
                break;
        }
        
    }
    
    private synchronized void writerLogs(String line)
    {
        PrintWriter pw = null;
        FileWriter fichLog = null;
        try {
            fichLog = new FileWriter("./logs.txt", true);
            pw = new PrintWriter(fichLog);
            pw.println(line);
            pw.close();
        } catch (IOException ex) {
            Logger.getLogger(RobotFTP.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}