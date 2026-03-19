package br.ufs.dcomp.ChatRabbitMQ;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.protobuf.ByteString;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import io.github.cdimascio.dotenv.Dotenv;


    public class ChatSerializer{
        
        public static void listUsersOfGroup(String group) {

            Dotenv dotenv = Dotenv.load();
            String vhost = dotenv.get("VHOST");
            String VHOST = vhost;
            String exchange = "MS_" + group;

            try {
                
                String path = "/api/exchanges/" + VHOST + "/" + exchange + "/bindings/source";
                String json = Rest.doGet(path);

                Gson gson = new Gson();
                Binding[] bindings = gson.fromJson(json, Binding[].class);

                boolean first = true;

                for (Binding b : bindings) {

                    if ("queue".equals(b.destination_type) && b.destination != null && b.destination.startsWith("MS_")){
                        
                        if (!first)
                            System.out.print(", ");
                        
                        System.out.print(b.destination.substring(3));
                        first = false;
                    }
                }
                System.out.println();


            } catch (Exception e) {
                System.out.println("Erro ao listar usuários do grupo.");
            }
        }


        public static void listGroupsOfUser(String user) {

            Dotenv dotenv = Dotenv.load();
            String vhost = dotenv.get("VHOST");

            String VHOST = vhost; 
            String queue = "MS_" + user;
            
            // LISTAR OS GRUPOS QUE O USUARIO LOGADO ESTÁ
            // !listGroups 
            // resultado: ufs, amigos, familia

            try {
                String path = "/api/queues/" + VHOST + "/" + queue + "/bindings";
                String json = Rest.doGet(path);

                Gson gson = new Gson();
                Binding[] bindings = gson.fromJson(json, Binding[].class);

                boolean first = true;
                for (Binding b : bindings) {
                    // Verifico se o exchange é null e se ele tem de fato o prefixo 'MS_'
                    if (b.source != null && b.source.startsWith("MS_")) {
                        if (!first) 
                            System.out.print(", ");
                        
                        System.out.print(b.source.substring(3));
                        first = false;
                    }
                }
                System.out.println();


            } catch (Exception e) {
                System.out.println("Erro ao listar grupos do usuário.");
            }
        }

        
        // Método para criar um usuario.
        public static void createUser(Channel channelMessage, Channel channelFile, String exchange_name, String queue_name) throws IOException {
            
            if (!queue_name.isEmpty()) {
                
                String queueMessage = String.format("MS_%s",queue_name);
                String queueUpload = String.format("UP_%s",queue_name);
                
                Map<String, Object> args = new HashMap<>();
                args.put("x-queue-type", "quorum");
                
                channelMessage.queueDeclare(queueMessage, true, false, false, args);
                channelMessage.queueBind(queueMessage, exchange_name, queue_name);

                channelFile.queueDeclare(queueUpload, true, false, false, args);
                channelFile.queueBind(queueUpload, exchange_name, queue_name);
                
                
            }else {
            System.out.println("Nome de usuário inválido!!");
            }
        } 

        public static void createGroup(Channel channelMessage, Channel channelFile, String exchange_name) throws IOException {
            
            if (exchange_name.startsWith("!addGroup")){
                
                String[] group_name = exchange_name.split(" ", 2);
                
                if(group_name.length < 2 || group_name[1].isBlank()){
                    
                    System.out.println("Nome de grupo inválido!!");
                    return;
                }
                
                channelMessage.exchangeDeclare("MS_"+group_name[1], "fanout");
                channelFile.exchangeDeclare("UP_"+group_name[1], "fanout");
                
            }
        }

        public static void addUserToGroup(Channel channelMessage, Channel channelFile, String message) throws IOException {
            
            String[] partes = message.trim().split("\\s+", 3);
            
            //!addUser joao amigos
            
            if (partes.length < 3) {
                System.out.println("Uso incorreto! Formato válido: !addUser <usuario> <grupo>");
                return;
            }
            
            if(partes.length == 2)
                System.out.println("Nome de grupo inválido ou não existente");
            
            String username = partes[1].trim();
            String group = partes[2].trim();
            
            channelMessage.queueBind("MS_"+username, "MS_"+group, ""); 
            channelFile.queueBind("UP_"+username, "UP_"+group, ""); 
        }
    
    //Mesma lógica de add no grupo.
    public static void removeUserToGroup(Channel channelMessage, Channel channelFile, String message) throws IOException {
        
        String[] partes = message.trim().split("\\s+", 3);
        
        //!removeUser joao amigos
        
        if (partes.length < 3) {
            System.out.println("Uso incorreto! Formato válido: !removeUser <usuario> <grupo>");
            return;
        }
        
        if(partes.length == 2)
            System.out.println("Nome de grupo inválido ou não existente");
        
        String username = partes[1].trim();
        String group = partes[2].trim();
        
        channelMessage.queueUnbind("MS_"+username, "MS_"+group, ""); 
        channelFile.queueUnbind("UP_"+username, "UP_"+group, ""); 
        
    }
    
    public static void uploadFile(Channel channel, String message) throws IOException{
        
        String[] partes = message.trim().split("\\s+", 2);
        
        if(partes.length < 2){
            System.out.printf("Comando para upload de arquivos inválido. Formato: !upload <caminho/do/arquivo>");
            return;
        }
        
        String caminho = partes[1];
        File f = new File(caminho);
    
        if(!f.exists()){
            System.out.println("Arquivo não encontrado: " + caminho);
            return;
        }
    
        if(!f.isFile()){
            System.out.println("O caminho não aponta para um arquivo válido.");
            return;
        }
    }
    
    // Só linko/aponto o usuario para outro exchange_name.
    public static void selectGroup(Channel channel, String exchange_name, String username) throws IOException{
        channel.queueBind(username, exchange_name, "");
        
    }
    
    // Só pego o nome do exchange_name digitado e deleto
    public static void deleteGroup(Channel channel, String exchange_name) throws IOException {
        channel.exchangeDelete(exchange_name);
    }
    
    private static boolean isGroup(String command){
        
        if(command.startsWith("#"))
            return true;
        return false;
    }

    public static Consumer consumeMessage(Channel channel, String queue_name) {
        
        Consumer consumer = new DefaultConsumer(channel) {
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                
                MensagemProto.Mensagem mensagem = MensagemProto.Mensagem.parseFrom(body);
                
                String emissor = mensagem.getEmissor();
                String grupo = mensagem.getGrupo();  
                
                String formattedMessage;
                
                
                if (grupo == null || grupo.isEmpty()) {
                    formattedMessage = emissor;  
                } else {
                    formattedMessage = emissor + "#" + grupo; 
                }
                
                String mensagemFinal = String.format(
                    "\n(%s às %s) @%s diz: ",
                    mensagem.getData(),
                    mensagem.getHora(),
                    formattedMessage
                );
                
                String tipo = mensagem.getConteudo().getTipo();
                String corpo;
                
                if (tipo.startsWith("text/")) {
                    corpo = mensagem.getConteudo().getCorpo().toStringUtf8();
                } else {
                    corpo = "[arquivo recebido: " + mensagem.getConteudo().getNome() + "]";
                }
                
                mensagemFinal += corpo;
                
                System.out.print(mensagemFinal);
            }
        };
        return consumer;
    }

    public static Consumer consumeFile(Channel channel, String queueName) {
        
        Consumer consumer = new DefaultConsumer(channel) {
            
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                
                MensagemProto.Mensagem mensagem = MensagemProto.Mensagem.parseFrom(body);

                
                String emissor = mensagem.getEmissor();
                String nomeArquivo = mensagem.getConteudo().getNome();
                byte[] conteudo = mensagem.getConteudo().getCorpo().toByteArray();
                
                //String pathArq = "/home/ec2-user/environment/sistema-de-troca-de-mensagens-minstant-neas-grupo-m-2025-2/src/main/Arquivos";
                String pathArq = "../Arquivos";
                
                //Path pasta = Paths.get(System.getProperty("user.home"), "chat", "downloads");
                Path pasta = Paths.get(pathArq);
                Files.createDirectories(pasta);
                
                Path destino = pasta.resolve(nomeArquivo);
                Files.write(destino, conteudo);
                
                System.out.printf(
                    "(%s às %s) Arquivo \"%s\" recebido de @%s!\n",
                    mensagem.getData(),
                    mensagem.getHora(),
                    nomeArquivo,
                    emissor
                );
               
            }
        };
        return consumer;
    }
    
    public static MensagemProto.Mensagem messageSerializer(File file, String msg, String group, String source_queue_name) throws IOException{

        String data = new SimpleDateFormat("dd/MM/yyyy").format(new Date());
        String hora = new SimpleDateFormat("HH:mm:ss").format(new Date());
        
        // Criar conteúdo 
        MensagemProto.Conteudo conteudo;

        if (file != null && file.exists()){
            
            // É arquivo
            byte[] bytes = Files.readAllBytes(file.toPath());
            String mime = Files.probeContentType(file.toPath());
            
            if (mime == null){
                mime = "application/octet-stream";
            }
            
            conteudo = MensagemProto.Conteudo.newBuilder()
            .setTipo(mime)
            .setCorpo(ByteString.copyFrom(bytes))
            .setNome(file.getName())
            .build();
           
        } else {
            
            // É texto
            conteudo = MensagemProto.Conteudo.newBuilder()
            .setTipo("text/plain")
            .setCorpo(ByteString.copyFromUtf8(msg))
            .setNome("") 
            .build();
        }
        
        // Criar mensagem completa
        MensagemProto.Mensagem msgEnviar = MensagemProto.Mensagem.newBuilder()
        .setEmissor(source_queue_name)
        .setData(data)  
        .setHora(hora) 
        .setGrupo(group) 
        .setConteudo(conteudo)
        .build();
        
        return msgEnviar;
    }
}