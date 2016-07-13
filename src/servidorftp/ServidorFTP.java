package servidorftp;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 *
 * @author Jose Manuel Martinez de la Insua
 * @author Alberto Moreno Mantas
 * 
 */
public class ServidorFTP {
    public static void main(String[] args) throws IOException {
		
        int port=8987;
	Socket socketServicio = null;
        ServerSocket socketServidor = null;
        
        //CREAMOS LA BASE DE DATOS SI NO ESTUVIERA CREADA
        File fichDB = new File ("./basedatos.txt");
        BufferedWriter bw = null;
        
        if(!fichDB.exists())
        {
           bw = new BufferedWriter(new FileWriter(fichDB));
           bw.write("Usuario Contraseña\n");
           bw.write("Jose 1\n");
           bw.write("Alberto 1\n");
           bw.close();
        }      
        
        //CREAMOS EL FICHEO LOGS SI NO ESTUVIERA CREADO
        fichDB = new File("./logs.txt");
        bw = null;
        
        if(!fichDB.exists()){
           bw = new BufferedWriter(new FileWriter(fichDB));
           bw.close();
        }
        try {
            
            socketServidor = new ServerSocket(port);

            do {
                
                try{
                    socketServicio = socketServidor.accept();
                }catch(IOException e){
                    System.out.println("No se pudo aceptar la conexion solicitada");
                }
                
                // LANZAMIENTO DE HEBRA
                RobotFTP bot = new RobotFTP(socketServicio);
                bot.start();
                
            } while (true);

        } catch (IOException e) {
                System.err.println("Error al escuchar en el puerto " + port);
        }

    }
}