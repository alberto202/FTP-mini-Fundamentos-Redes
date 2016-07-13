package servidorftp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
        
/**
 *
 * @author Jose Manuel Martinez de la Insua
 * @author Alberto Moreno Mantas
 * 
 */
public class ClientFTP{
    
    public static void main(String[] args) throws Exception{
        
        String host = "localhost";
        int port = 8987;
        
        Socket socketServicio = null;
        
        PrintWriter outPrinter = null;
        BufferedReader inReader = null;
        
        BufferedInputStream objetoLeer =  null;
        BufferedOutputStream objetoEnviar = null;
        
        DataOutputStream datosSalida = null;
        DataInputStream datosEntrada = null;
        
        File f;
        Scanner entradaEscaner = null;
        String resultado;
        String entradaTeclado;
        String line, line2;
        Integer option;
        
        try {		
            
            // INICIALIZAMOS SERVICIOS
            socketServicio = new Socket(host,port);
            inReader = new BufferedReader(new InputStreamReader(socketServicio.getInputStream()));
            outPrinter =  new PrintWriter(socketServicio.getOutputStream());
            entradaEscaner = new Scanner (System.in); 
            
            // SALUDAR AL SERVIDOR
            
            outPrinter.write("Conectado desde : " + host + " por el puerto :" + port + "\n");
            outPrinter.flush();
            
            line = inReader.readLine(); //Conexion del cliente --HELO
            System.out.println("Bienvenido!!!");
            if(!"HELO".equals(line)) throw new UnsupportedOperationException("Error de conexión.");

            // BUCLE QUE CONTROLA EL PROCESO DE LOGIN
            do{
                // LEER PREGUNTA POR LOGIN
                System.out.println("Introduce el login:");
                
                // ESCRIBIR LOGIN
                entradaTeclado = entradaEscaner.nextLine();
                
                // LEER PREGUNTA POR PASSWORD
                System.out.println("Introduce el pass:");
                
                // ESCRIBIR PASSWORD 
                entradaTeclado += " ";
                entradaTeclado += entradaEscaner.nextLine();
                
                outPrinter.println(entradaTeclado);
                outPrinter.flush();
                
                // RESULTADO GUARDA EL CODIGO DE EXITO O FALLO EN EL LOGIN
                resultado = inReader.readLine();
                
                if(!"200".equals(resultado))
                    System.out.println("Usuario o contraseña incorrectos. Por favor, vuelve a introducirlos. Error: "+resultado);                
                else System.out.println("¡¡Bienvenido!!");
                
            }while(!"200".equals(resultado));
            
            do{
                // LEE MENU Y LO MUESTRA POR PANTALLA
                //resultado = inReader.readLine();
                //if(!"208".equals(resultado)) throw new UnsupportedOperationException("Codigo no esperado");
                
                for(String s: Opciones.getMenu().split("-")){
                    System.out.println(s);
                }

                // ELEGIR OPCION Y ENVIAR A SERVIDOR
                entradaTeclado = entradaEscaner.nextLine();
                while(Integer.parseInt(entradaTeclado)<=0 || Integer.parseInt(entradaTeclado)>Opciones.options.length+1){
                    entradaTeclado = entradaEscaner.nextLine();
                }
                option = Integer.parseInt(entradaTeclado);
                outPrinter.println(Opciones.getSelect(option-1));
                outPrinter.flush();

                // ESPERANDO CONFIRMACION SOLICITUD Y PROCESADO
                f = new File("./DirectorioCliente/");    f.mkdirs();
                
                switch(option){
                    // SUBIDA DE FICHERO AL SERVIDOR
                    case 1:
                        // LEEMOS EL MENSAJE PARA SELECCIONAR ARCHIVO
                        line = inReader.readLine();
                        if(!"201".equals(line)) System.out.println("Codigo no esperado. Error: "+line);
                        else line = "Introduce nombre de fichero para subir: ";        

                        do{
                            System.out.println(line);
                            // SELECCION DE ARCHIVO PARA SUBIR
                            entradaTeclado = entradaEscaner.nextLine();
                            f = new File("./DirectorioCliente/" + entradaTeclado);
                            
                            if(!f.exists()){
                                System.out.println("El fichero no existe");
                            }
                        }while(!f.exists());
                        
                        // SERVICIOS PARA SUBIDA DE FICHERO
                        // OBJETO_LEER -> CARGA FICHERO DESDE PC
                        // OBJETO ENVIAR -> MANDA FICHERO AL SERVIDOR
                        // DATO_SALIDA -> INFORMACION DEL FICHERO
                        objetoLeer = new BufferedInputStream(new FileInputStream(f));
                        objetoEnviar = new BufferedOutputStream(socketServicio.getOutputStream());
                        datosSalida = new DataOutputStream(socketServicio.getOutputStream());

                        // ENVIO DE FICHERO Y DE DATOS ADJUNTOS
                        datosSalida.writeUTF(f.getName());
                        datosSalida.flush();
                        datosSalida.writeInt((int) f.length());
                        datosSalida.flush();
                        byte[] buffer = new byte[(int) f.length()];
                        objetoLeer.read(buffer);
                        
                        objetoEnviar.write(buffer);
                        objetoEnviar.flush();
                        

                        break;

                    // BAJADA DE FICHERO DESDE EL SERVIDOR
                    case 2:
                        // LEEMOS EL MENSAJE PARA SELECCIONAR ARCHIVO Y COMPROBAMOS QUE EXISTE EL FICHERO
                        line = inReader.readLine();
                        if(!"201".equals(line)) System.out.println("Codigo no esperado. Error: "+line);
                        else line = "Introduce nombre de fichero para descargar: ";        

                        do{
                            System.out.println(line);
                            
                            // ENVIAMOS EL NOMBRE DEL ARCHIVO A DESCARGAR
                            entradaTeclado = entradaEscaner.nextLine();
                            outPrinter.println(entradaTeclado);
                            outPrinter.flush();
                            line2 = inReader.readLine();
                            if(!"102".equals(line2))
                                System.out.println("El fichero no existe.");
                        }while(!"102".equals(line2));
                        
                        // RECOGIDA DE DATOS SOBRE EL FICHERO
                        datosEntrada = new DataInputStream(socketServicio.getInputStream());
                        String nombreArchivo = datosEntrada.readUTF(); 
                        buffer = new byte[datosEntrada.readInt()];

                        // OBJETO_LEER -> CARGA EL FICHERO MANDADO POR EL SERVIDOR
                        // OBJETO_ENVIAR -> ESCRIBE EL FICHERO EN EL PC

                        objetoLeer = new BufferedInputStream(socketServicio.getInputStream());
                        objetoEnviar = new BufferedOutputStream(new FileOutputStream("./DirectorioCliente/"+ nombreArchivo));

                        // LEO DEL BUFFER EL FICHERO BYTE A BYTE
                        objetoLeer.read(buffer);
                       
                        // ESCRIBO EL FICHERO EN EL PC
                        objetoEnviar.write( buffer );
                        objetoEnviar.flush();

                        break;

                    // CERRAMOS EL CLIENTE Y SALIMOS
                    case 3:
                        System.out.println(inReader.readLine());
                        socketServicio.close();
                        break;
                }
                
                line = inReader.readLine();
                if(!"202".equals(line))  throw new UnsupportedOperationException("Codigo no esperado. Error: "+line);
                else System.out.println("Solicitud realizada con exito");
                
            }while(!socketServicio.isClosed());
            
        } catch (UnknownHostException e) {
                System.err.println("Error: Nombre de host no encontrado.");
        } catch (IOException e) {
                System.err.println(e.getMessage());
                System.err.println("Error de entrada/salida al abrir el socket.");
        }
        
    }
}