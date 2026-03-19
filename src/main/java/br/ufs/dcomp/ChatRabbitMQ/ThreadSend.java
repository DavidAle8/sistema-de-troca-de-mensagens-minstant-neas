package br.ufs.dcomp.ChatRabbitMQ;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.rabbitmq.client.Channel;

class ThreadSend extends Thread {
    
    private Channel channel;
    private String destinationName;
    private String sender;       
    private String filePath;
    private String group;
    
    
    public ThreadSend(Channel channel, String sender, String destinationName, String filePath, String group) {
        
        this.channel = channel;
        this.sender = sender;
        this.destinationName = destinationName;
        this.filePath = filePath;
        this.group = group;
    }
    
    public void run(){
        
        try{
            
            Path path = Paths.get(filePath);
            
            if (!Files.exists(path)) {
                System.out.println("Arquivo não encontrado: " + filePath);
                return;
            }
            
            byte[] fileBytes = Files.readAllBytes(path);
            String fileName = path.getFileName().toString();
            
            String mimeType = Files.probeContentType(path);
            
            if (mimeType == null){
                mimeType = "application/octet-stream";
            }
                
            //System.out.printf("Enviando \"%s\" para %s%s.\n", fileName, group.isEmpty() ? "@" : "#", destinationName);
            System.out.printf("Enviando \"%s\" para %s%s.\n", fileName, group.isEmpty() ? "@" : "#", (group.isEmpty() ? destinationName.replace("UP_", "") : destinationName));
            
            MensagemProto.Conteudo conteudo = MensagemProto.Conteudo.newBuilder()
                .setTipo(mimeType)
                .setCorpo(com.google.protobuf.ByteString.copyFrom(fileBytes))
                .setNome(fileName)
                .build();    
            
            String data = java.time.LocalDate.now().toString();
            String hora = java.time.LocalTime.now().withNano(0).toString(); 
            
            MensagemProto.Mensagem mensagem = MensagemProto.Mensagem.newBuilder()
                .setEmissor(sender)
                .setData(data)
                .setHora(hora)
                .setGrupo(group)
                .setConteudo(conteudo)
                .build();
                
            String exchange;
            String routingKey;

            if (group.isEmpty()) {
                exchange = "";
                routingKey = "UP_" + destinationName;
            } else {
                exchange = "UP_" + group;
                routingKey = "";
            }

            channel.basicPublish(exchange, routingKey, null, mensagem.toByteArray());

            //System.out.printf("Arquivo \"%s\" foi enviado para %s%s!\n", fileName, group.isEmpty() ? "@" : "#", destinationName);
            System.out.printf("Arquivo \"%s\" foi enviado para %s%s!\n", fileName, group.isEmpty() ? "@" : "#", (group.isEmpty() ? destinationName.replace("UP_", "") : destinationName));
            
        }catch(Exception e){
            e.printStackTrace();
        }
    }  
}