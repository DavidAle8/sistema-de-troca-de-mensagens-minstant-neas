package br.ufs.dcomp.ChatRabbitMQ;
import java.util.Scanner;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import io.github.cdimascio.dotenv.Dotenv;

public class ChatMain {

    public static void main(String[] argv) throws Exception {
        
        ConnectionFactory factory = new ConnectionFactory();
        
        Dotenv dotenv = Dotenv.load();
        String uri = dotenv.get("URI_STRING");

        factory.setUri(uri);

        Connection connection = factory.newConnection();
        Channel channelMessage = connection.createChannel();
        Channel channelFile = connection.createChannel();
        
        String EXCHANGE_NAME = "Correios";
        channelMessage.exchangeDeclare(EXCHANGE_NAME, "direct");
        channelFile.exchangeDeclare(EXCHANGE_NAME, "direct");
            
        Scanner scanf = new Scanner(System.in);
        String msg, source_queue_name, destination_name;
        String command_group_user = "@";
        
        String prefix_MS = "MS_";
        String prefix_UP = "UP_";
        
        System.out.print("User: ");
        source_queue_name = scanf.nextLine();
        ChatSerializer.createUser(channelMessage, channelFile, EXCHANGE_NAME, source_queue_name);

        
        while (true) {
            
            System.out.print(">>");
            destination_name = scanf.nextLine();
            
            if (!destination_name.isEmpty() && destination_name.charAt(0) == '@') {
                
                destination_name = destination_name.replace("@", "");
                ChatSerializer.createUser(channelMessage, channelFile, EXCHANGE_NAME, destination_name);
                break;
            } else {
                System.out.println("Nome de destinatário inválido!");
            }
        }
        
        System.out.println("");

        // Já é a thread de consume de mensagens e upload.
        ThreadConsumeMessage consumeMessage = new ThreadConsumeMessage(channelMessage, source_queue_name);
        consumeMessage.start();

        ThreadConsumeUpload consumeUpload = new ThreadConsumeUpload(channelFile, source_queue_name);
        consumeUpload.start();

        while (true) {
            
            System.out.printf("%s%s<< ", command_group_user, destination_name);
            msg = scanf.nextLine();
            
            if (msg.startsWith("@")) {
                
                destination_name = msg.replace("@", "").trim();
                ChatSerializer.createUser(channelMessage, channelFile, EXCHANGE_NAME, destination_name);
                command_group_user = "@";
                
            } else if (msg.startsWith("#")) {
                
                String group = msg.replace("#", "").trim();
                ChatSerializer.selectGroup(channelMessage, prefix_MS+group, prefix_MS+source_queue_name);
                channelFile.queueBind("UP_" + source_queue_name, "UP_" + group, "");
                
                destination_name = group;
                command_group_user = "#";
                
            } else if (msg.startsWith("!addUser")) {
                
                ChatSerializer.addUserToGroup(channelMessage, channelFile, msg);
        
            } else if (msg.startsWith("!removeUser")) {
                
                ChatSerializer.removeUserToGroup(channelMessage, channelFile, msg);
                
            } else if (msg.startsWith("!addGroup")) {
                
                ChatSerializer.createGroup(channelMessage, channelFile, msg);

                String group_name = msg.replace("!addGroup", "").trim();

                channelMessage.queueBind(prefix_MS+source_queue_name, prefix_MS+group_name, "");
                channelFile.queueBind("UP_" + source_queue_name, "UP_" + group_name, "");
                
            } else if (msg.startsWith("!removeGroup")) {
                
                String group_name = msg.replace("!removeGroup", "").trim();
                
                ChatSerializer.deleteGroup(channelMessage, prefix_MS+group_name);
                ChatSerializer.deleteGroup(channelFile, prefix_UP+group_name);
                
            } else if(msg.startsWith("!upload")){
                
                String[] partes = msg.trim().split("\\s+", 2);
                String path = partes[1];
                String group = command_group_user.equals("#") ? destination_name : "";
                
                ChatSerializer.uploadFile(channelFile, msg);
                
                ThreadSend threadSend = new ThreadSend(channelFile, source_queue_name, destination_name, path, group);
                threadSend.start();

            } else if (msg.startsWith("!listUsers")){
                
                String[] partes = msg.trim().split("\\s+", 2);
                
                if (partes.length < 2) {
                    System.out.println("Uso correto: !listUsers <grupo>");
                    continue;
                }

                ChatSerializer.listUsersOfGroup(partes[1]);
                
            } else if(msg.startsWith("!listGroups")){

                ChatSerializer.listGroupsOfUser(source_queue_name);
        
            } else {
                
                String group_destination = command_group_user.equals("#") ? destination_name : "";
                
                MensagemProto.Mensagem msgEnviar = ChatSerializer.messageSerializer(null, msg, group_destination, source_queue_name);
                    
                if (command_group_user.equals("#")){
                    
                    String queueGroupMessage = prefix_MS + destination_name;
                    channelMessage.basicPublish(queueGroupMessage, "", null, msgEnviar.toByteArray());
                } else {
                    
                    String queueMessage = prefix_MS + destination_name;
                    channelMessage.basicPublish("", queueMessage, null, msgEnviar.toByteArray());
                }
            }
        }
    }
}



/*

mvn clean compile assembly:single

java -jar target/ChatRabbitMQ-1.0-SNAPSHOT-jar-with-dependencies.jar

!listUsers grupo1

!listGroups


!upload arquivos\teste.txt
!upload arquivos\svh.pdf
!upload arquivos\livroIA.pdf


Comandos grupo:

!addGroup grupo3

!removeGroup alguem grupo1

!removeUser rafael grupo1







*/

